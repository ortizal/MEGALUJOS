package ec.com.megalujos.purchasing.application;

import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.purchasing.domain.Supplier;
import ec.com.megalujos.purchasing.domain.SupplierRepository;
import ec.com.megalujos.purchasing.dto.SupplierRequest;
import ec.com.megalujos.purchasing.dto.SupplierResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final AuditService auditService;

    public SupplierService(SupplierRepository supplierRepository, AuditService auditService) {
        this.supplierRepository = supplierRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponseDto> list(String q, Pageable pageable) {
        return supplierRepository.search(normalize(q), pageable).map(this::map);
    }

    @Transactional
    public SupplierResponseDto create(SupplierRequest request, UUID actorId) {
        String taxId = normalize(request.getTaxId());
        if (taxId != null && supplierRepository.existsByTaxId(taxId)) {
            throw new BusinessRuleException("SUPPLIER_DUPLICATE",
                    "Ya existe un proveedor registrado con el RUC/cédula " + taxId);
        }

        Supplier supplier = new Supplier();
        apply(supplier, request, taxId);

        Supplier saved = supplierRepository.save(supplier);
        auditService.record(actorId, "SUPPLIER_CREATED", "Supplier", saved.getId(), auditMetadata(saved), null);
        return map(saved);
    }

    @Transactional
    public SupplierResponseDto update(UUID id, SupplierRequest request, UUID actorId) {
        Supplier supplier = requireSupplier(id);
        String taxId = normalize(request.getTaxId());
        if (taxId != null && supplierRepository.existsByTaxIdAndIdNot(taxId, id)) {
            throw new BusinessRuleException("SUPPLIER_DUPLICATE",
                    "Ya existe un proveedor registrado con el RUC/cédula " + taxId);
        }

        apply(supplier, request, taxId);

        Supplier saved = supplierRepository.save(supplier);
        auditService.record(actorId, "SUPPLIER_UPDATED", "Supplier", saved.getId(), auditMetadata(saved), null);
        return map(saved);
    }

    @Transactional
    public SupplierResponseDto setActive(UUID id, boolean active, UUID actorId) {
        Supplier supplier = requireSupplier(id);
        supplier.setActive(active);

        Supplier saved = supplierRepository.save(supplier);
        Map<String, Object> metadata = auditMetadata(saved);
        metadata.put("active", active);
        auditService.record(actorId, "SUPPLIER_UPDATED", "Supplier", saved.getId(), metadata, null);
        return map(saved);
    }

    private void apply(Supplier supplier, SupplierRequest request, String taxId) {
        supplier.setTaxId(taxId);
        supplier.setName(request.getName());
        supplier.setContactName(normalize(request.getContactName()));
        supplier.setEmail(normalize(request.getEmail()));
        supplier.setPhone(normalize(request.getPhone()));
        supplier.setAddress(normalize(request.getAddress()));
    }

    private Supplier requireSupplier(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Map<String, Object> auditMetadata(Supplier supplier) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", supplier.getName());
        metadata.put("taxId", supplier.getTaxId());
        metadata.put("active", supplier.isActive());
        return metadata;
    }

    private SupplierResponseDto map(Supplier supplier) {
        SupplierResponseDto dto = new SupplierResponseDto();
        dto.setId(supplier.getId());
        dto.setTaxId(supplier.getTaxId());
        dto.setName(supplier.getName());
        dto.setContactName(supplier.getContactName());
        dto.setEmail(supplier.getEmail());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setActive(supplier.isActive());
        dto.setCreatedAt(supplier.getCreatedAt());
        return dto;
    }
}
