package ec.com.megalujos.cash.application;

import ec.com.megalujos.cash.domain.*;
import ec.com.megalujos.cash.dto.CashCountRequest;
import ec.com.megalujos.cash.dto.CashReconciliationDto;
import ec.com.megalujos.cash.dto.CashSessionDto;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class CashSessionService {

    private final CashSessionRepository sessionRepository;
    private final CashSessionSearchRepository sessionSearchRepository;
    private final CashRegisterRepository registerRepository;
    private final CashMovementRepository movementRepository;
    private final CashCountRepository countRepository;
    private final CashReconciliationRepository reconciliationRepository;
    private final AuditService auditService;

    public CashSessionService(CashSessionRepository sessionRepository,
                              CashSessionSearchRepository sessionSearchRepository,
                              CashRegisterRepository registerRepository,
                              CashMovementRepository movementRepository,
                              CashCountRepository countRepository,
                              CashReconciliationRepository reconciliationRepository,
                              AuditService auditService) {
        this.sessionRepository = sessionRepository;
        this.sessionSearchRepository = sessionSearchRepository;
        this.registerRepository = registerRepository;
        this.movementRepository = movementRepository;
        this.countRepository = countRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CashSession openSession(UUID registerId, BigDecimal openingAmount, UUID userId, String notes, String ipAddress) {
        CashRegister register = registerRepository.findById(registerId)
                .orElseThrow(() -> new ResourceNotFoundException("Caja registradora", registerId));

        // Verificar que no haya una sesión ya abierta en esta caja
        Optional<CashSession> openSession = sessionRepository.findByRegisterIdAndStatus(registerId, "OPEN");
        if (openSession.isPresent()) {
            throw new BusinessRuleException("SESSION_ALREADY_OPEN", "Ya existe una sesión de caja abierta en la registradora " + register.getName());
        }

        CashSession session = new CashSession();
        session.setRegister(register);
        session.setOpenedBy(userId);
        session.setOpenedAt(OffsetDateTime.now());
        session.setOpeningAmount(openingAmount != null ? openingAmount : BigDecimal.ZERO);
        session.setStatus("OPEN");
        session.setNotes(notes);

        CashSession saved = sessionRepository.save(session);

        auditService.record(userId, "CASH_OPENED", "CASH_SESSION", saved.getId(), Map.of(
                "registerId", registerId,
                "openingAmount", saved.getOpeningAmount()
        ), ipAddress);

        return saved;
    }

    @Transactional
    public CashSession closeSession(UUID sessionId, BigDecimal countedAmount, List<CashCountRequest.CountItem> counts,
                                    UUID userId, String notes, String ipAddress) {
        CashSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja", sessionId));

        if (!"OPEN".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessRuleException("SESSION_NOT_OPEN", "La sesión de caja no está abierta");
        }

        // Persistar el arqueo por medio de pago si se envía junto al cierre
        if (counts != null && !counts.isEmpty()) {
            persistCounts(session, counts);
        }

        BigDecimal income = movementRepository.sumIncomeBySessionId(sessionId);
        BigDecimal expense = movementRepository.sumExpenseBySessionId(sessionId);
        BigDecimal expected = session.getOpeningAmount().add(income).subtract(expense);

        if (countedAmount != null) {
            if (countedAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException("INVALID_COUNTED_AMOUNT", "El monto contado no puede ser negativo");
            }
        }
        BigDecimal difference = countedAmount != null ? countedAmount.subtract(expected) : BigDecimal.ZERO;

        session.setClosedBy(userId);
        session.setClosedAt(OffsetDateTime.now());
        session.setCountedAmount(countedAmount);
        session.setExpectedAmount(expected);
        session.setDifferenceAmount(difference);
        session.setStatus("CLOSED");
        if (notes != null) {
            session.setNotes((session.getNotes() != null ? session.getNotes() + "\n" : "") + notes);
        }

        CashSession saved = sessionRepository.save(session);

        auditService.record(userId, "CASH_CLOSED", "CASH_SESSION", saved.getId(), Map.of(
                "expectedAmount", expected,
                "countedAmount", countedAmount != null ? countedAmount : BigDecimal.ZERO,
                "differenceAmount", difference
        ), ipAddress);

        return saved;
    }

    /** Registra (upsert) el conteo físico por medio de pago y devuelve el arqueo calculado. */
    @Transactional
    public CashReconciliationDto recordCounts(UUID sessionId, List<CashCountRequest.CountItem> counts,
                                              UUID userId, String ipAddress) {
        CashSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja", sessionId));

        if (!"OPEN".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessRuleException("SESSION_NOT_OPEN", "La sesión de caja no está abierta");
        }

        persistCounts(session, counts);

        auditService.record(userId, "CASH_COUNTED", "CASH_SESSION", session.getId(),
                Map.of("methods", counts.stream().map(CashCountRequest.CountItem::getMethod).toList()), ipAddress);

        return buildReconciliation(session);
    }

    /** Arqueo por medio de pago: esperado (ventas + movimientos + apertura) vs contado. */
    @Transactional(readOnly = true)
    public CashReconciliationDto reconcile(UUID sessionId) {
        CashSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja", sessionId));
        return buildReconciliation(session);
    }

    @Transactional(readOnly = true)
    public PageResponse<CashSessionDto> listSessions(String status, Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "openedAt"));

        Page<CashSession> page = sessionSearchRepository.search(status, sorted);
        return PageResponse.from(page, this::toDto);
    }

    @Transactional
    public CashMovement recordMovement(UUID sessionId, String movementType, BigDecimal amount, String reason, UUID userId) {
        CashSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja", sessionId));

        if (!"OPEN".equalsIgnoreCase(session.getStatus())) {
            throw new BusinessRuleException("SESSION_CLOSED", "No se pueden registrar movimientos en una caja cerrada");
        }

        CashMovement movement = new CashMovement();
        movement.setSessionId(sessionId);
        movement.setMovementType(movementType);
        movement.setAmount(amount);
        movement.setReason(reason);
        movement.setActorId(userId);

        return movementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public CashSession getActiveSession(UUID registerId) {
        if (registerId != null) {
            return sessionRepository.findByRegisterIdAndStatus(registerId, "OPEN")
                    .orElse(null);
        }
        return sessionRepository.findFirstByStatusOrderByOpenedAtDesc("OPEN")
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CashMovement> getSessionMovements(UUID sessionId) {
        return movementRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    @Transactional(readOnly = true)
    public List<CashRegister> getActiveRegisters() {
        return registerRepository.findByActiveTrue();
    }

    /**
     * Esperado por medio de pago:
     * CASH = fondo de apertura + entradas en efectivo (cobros SALE_PAYMENT, anticipos
     *        ADVANCE_PAYMENT e ingresos) - salidas (retiros EXPENSE y devoluciones REFUND).
     *        Los cobros en efectivo ya están incluidos en income, por eso no se descuentan.
     * Otros medios = suma de sale_payments de las ventas de la sesión por método
     *        (APPLIED_ADVANCE se excluye: no es dinero físico de la caja).
     */
    private Map<String, BigDecimal> computeExpectedByMethod(CashSession session) {
        Map<String, BigDecimal> expected = new TreeMap<>();

        for (Object[] row : reconciliationRepository.sumPaymentsByMethodForSession(session.getId())) {
            expected.merge((String) row[0], (BigDecimal) row[1], BigDecimal::add);
        }

        BigDecimal income = movementRepository.sumIncomeBySessionId(session.getId());
        BigDecimal expense = movementRepository.sumExpenseBySessionId(session.getId());
        expected.put("CASH", session.getOpeningAmount().add(income).subtract(expense));
        return expected;
    }

    private void persistCounts(CashSession session, List<CashCountRequest.CountItem> counts) {
        Map<String, BigDecimal> expectedByMethod = computeExpectedByMethod(session);
        for (CashCountRequest.CountItem item : counts) {
            String method = item.getMethod().trim().toUpperCase();
            BigDecimal expected = expectedByMethod.getOrDefault(method, BigDecimal.ZERO);
            BigDecimal counted = item.getCountedAmount();
            BigDecimal difference = counted.subtract(expected);

            CashCount cashCount = countRepository.findBySessionIdAndMethod(session.getId(), method)
                    .orElseGet(CashCount::new);
            cashCount.setSessionId(session.getId());
            cashCount.setMethod(method);
            cashCount.setExpectedAmount(expected);
            cashCount.setCountedAmount(counted);
            cashCount.setDifference(difference);
            countRepository.save(cashCount);
        }
    }

    private CashReconciliationDto buildReconciliation(CashSession session) {
        Map<String, BigDecimal> expectedByMethod = computeExpectedByMethod(session);
        Map<String, BigDecimal> countedByMethod = new TreeMap<>();
        OffsetDateTime countedAt = null;
        for (CashCount cashCount : countRepository.findBySessionIdOrderByMethodAsc(session.getId())) {
            countedByMethod.put(cashCount.getMethod(), cashCount.getCountedAmount());
            if (countedAt == null || cashCount.getCreatedAt().isAfter(countedAt)) {
                countedAt = cashCount.getCreatedAt();
            }
        }

        Set<String> methods = new TreeSet<>();
        methods.addAll(expectedByMethod.keySet());
        methods.addAll(countedByMethod.keySet());

        CashReconciliationDto dto = new CashReconciliationDto();
        dto.setSessionId(session.getId());
        dto.setStatus(session.getStatus());
        dto.setOpeningAmount(session.getOpeningAmount());
        dto.setCountedAt(countedAt);

        BigDecimal totalExpected = BigDecimal.ZERO;
        BigDecimal totalCounted = BigDecimal.ZERO;
        List<CashReconciliationDto.MethodLineDto> lines = new ArrayList<>();
        for (String method : methods) {
            BigDecimal expected = expectedByMethod.getOrDefault(method, BigDecimal.ZERO);
            BigDecimal counted = countedByMethod.get(method);
            BigDecimal difference = counted != null ? counted.subtract(expected) : null;
            lines.add(new CashReconciliationDto.MethodLineDto(method, expected, counted, difference));

            totalExpected = totalExpected.add(expected);
            if (counted != null) {
                totalCounted = totalCounted.add(counted);
            }
        }

        dto.setLines(lines);
        dto.setTotalExpected(totalExpected);
        dto.setTotalCounted(totalCounted);
        dto.setTotalDifference(countedByMethod.isEmpty() ? BigDecimal.ZERO : totalCounted.subtract(totalExpected));
        return dto;
    }

    private CashSessionDto toDto(CashSession session) {
        CashSessionDto dto = new CashSessionDto();
        dto.setId(session.getId());
        dto.setRegisterId(session.getRegister() != null ? session.getRegister().getId() : null);
        dto.setRegisterName(session.getRegister() != null ? session.getRegister().getName() : null);
        dto.setOpenedBy(session.getOpenedBy());
        dto.setClosedBy(session.getClosedBy());
        dto.setOpenedAt(session.getOpenedAt());
        dto.setClosedAt(session.getClosedAt());
        dto.setOpeningAmount(session.getOpeningAmount());
        dto.setCountedAmount(session.getCountedAmount());
        dto.setExpectedAmount(session.getExpectedAmount());
        dto.setDifferenceAmount(session.getDifferenceAmount());
        dto.setStatus(session.getStatus());
        dto.setNotes(session.getNotes());
        return dto;
    }
}
