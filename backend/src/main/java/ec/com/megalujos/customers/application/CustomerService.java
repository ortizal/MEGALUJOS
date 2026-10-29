package ec.com.megalujos.customers.application;

import ec.com.megalujos.catalog.domain.VehicleVersionRepository;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.customers.domain.Customer;
import ec.com.megalujos.customers.domain.CustomerRepository;
import ec.com.megalujos.customers.domain.CustomerVehicle;
import ec.com.megalujos.customers.domain.CustomerVehicleRepository;
import ec.com.megalujos.customers.dto.CustomerRequest;
import ec.com.megalujos.customers.dto.CustomerResponseDto;
import ec.com.megalujos.customers.dto.CustomerVehicleRequest;
import ec.com.megalujos.customers.dto.CustomerVehicleResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomerService {

    private static final Set<String> DOCUMENT_TYPES = Set.of("CEDULA", "RUC", "PASAPORTE", "CONSUMIDOR_FINAL");
    private static final String CONSUMIDOR_FINAL = "CONSUMIDOR_FINAL";

    private final CustomerRepository customerRepository;
    private final CustomerVehicleRepository vehicleRepository;
    private final VehicleVersionRepository vehicleVersionRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerVehicleRepository vehicleRepository,
                           VehicleVersionRepository vehicleVersionRepository,
                           AuditService auditService) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehicleVersionRepository = vehicleVersionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> list(String q, Pageable pageable) {
        return customerRepository.search(normalize(q), pageable).map(this::mapSummary);
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto get(UUID id) {
        return mapDetail(requireCustomer(id));
    }

    @Transactional
    public CustomerResponseDto create(CustomerRequest request, UUID actorId) {
        validateDocument(request.getDocumentType(), request.getDocumentNumber(), null);

        Customer customer = new Customer();
        apply(customer, request);

        Customer saved = customerRepository.save(customer);
        auditService.record(actorId, "CUSTOMER_CREATED", "Customer", saved.getId(), auditMetadata(saved), null);
        return mapDetail(saved);
    }

    @Transactional
    public CustomerResponseDto update(UUID id, CustomerRequest request, UUID actorId) {
        Customer customer = requireCustomer(id);
        validateDocument(request.getDocumentType(), request.getDocumentNumber(), id);

        apply(customer, request);

        Customer saved = customerRepository.save(customer);
        auditService.record(actorId, "CUSTOMER_UPDATED", "Customer", saved.getId(), auditMetadata(saved), null);
        return mapDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerVehicleResponseDto> listVehicles(UUID customerId) {
        requireCustomer(customerId);
        return vehicleRepository.findByCustomerId(customerId).stream()
                .map(this::mapVehicle)
                .toList();
    }

    @Transactional
    public CustomerVehicleResponseDto addVehicle(UUID customerId, CustomerVehicleRequest request) {
        Customer customer = requireCustomer(customerId);
        validateVehicleVersion(request.getVehicleVersionId());

        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomer(customer);
        applyVehicle(vehicle, request);
        return mapVehicle(vehicleRepository.save(vehicle));
    }

    @Transactional
    public CustomerVehicleResponseDto updateVehicle(UUID customerId, UUID vehicleId, CustomerVehicleRequest request) {
        requireCustomer(customerId);
        validateVehicleVersion(request.getVehicleVersionId());

        CustomerVehicle vehicle = vehicleRepository.findByIdAndCustomerId(vehicleId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo de cliente", vehicleId));
        applyVehicle(vehicle, request);
        return mapVehicle(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(UUID customerId, UUID vehicleId) {
        requireCustomer(customerId);
        CustomerVehicle vehicle = vehicleRepository.findByIdAndCustomerId(vehicleId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo de cliente", vehicleId));
        vehicleRepository.delete(vehicle);
    }

    private void validateDocument(String documentType, String documentNumber, UUID customerId) {
        if (documentType == null || !DOCUMENT_TYPES.contains(documentType)) {
            throw new BusinessRuleException("INVALID_DOCUMENT_TYPE",
                    "El tipo de documento debe ser CEDULA, RUC, PASAPORTE o CONSUMIDOR_FINAL");
        }
        String number = normalize(documentNumber);
        if (!CONSUMIDOR_FINAL.equals(documentType) && (number == null || number.isBlank())) {
            throw new BusinessRuleException("DOCUMENT_NUMBER_REQUIRED",
                    "El número de documento es obligatorio para el tipo " + documentType);
        }
        if (number == null || number.isBlank()) {
            return;
        }
        boolean duplicated = customerId == null
                ? customerRepository.existsByDocumentTypeAndDocumentNumber(documentType, number)
                : customerRepository.existsByDocumentTypeAndDocumentNumberAndIdNot(documentType, number, customerId);
        if (duplicated) {
            throw new BusinessRuleException("CUSTOMER_DUPLICATE",
                    "Ya existe un cliente registrado con el documento " + number);
        }
    }

    private void validateVehicleVersion(UUID vehicleVersionId) {
        if (vehicleVersionId != null && !vehicleVersionRepository.existsById(vehicleVersionId)) {
            throw new ResourceNotFoundException("Versión de vehículo", vehicleVersionId);
        }
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setDocumentType(request.getDocumentType());
        customer.setDocumentNumber(normalize(request.getDocumentNumber()));
        customer.setName(request.getName());
        customer.setEmail(normalize(request.getEmail()));
        customer.setPhone(normalize(request.getPhone()));
        customer.setAddress(normalize(request.getAddress()));
    }

    private void applyVehicle(CustomerVehicle vehicle, CustomerVehicleRequest request) {
        vehicle.setPlate(normalize(request.getPlate()));
        vehicle.setVehicleVersionId(request.getVehicleVersionId());
        vehicle.setBrandName(normalize(request.getBrandName()));
        vehicle.setModelName(normalize(request.getModelName()));
        vehicle.setYear(request.getYear());
        vehicle.setColor(normalize(request.getColor()));
        vehicle.setNotes(normalize(request.getNotes()));
    }

    private Customer requireCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Map<String, Object> auditMetadata(Customer customer) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentType", customer.getDocumentType());
        metadata.put("documentNumber", customer.getDocumentNumber());
        metadata.put("name", customer.getName());
        return metadata;
    }

    private CustomerResponseDto mapSummary(Customer customer) {
        CustomerResponseDto dto = mapDetail(customer);
        dto.setVehicles(new ArrayList<>());
        return dto;
    }

    private CustomerResponseDto mapDetail(Customer customer) {
        CustomerResponseDto dto = new CustomerResponseDto();
        dto.setId(customer.getId());
        dto.setDocumentType(customer.getDocumentType());
        dto.setDocumentNumber(customer.getDocumentNumber());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());
        dto.setCreatedAt(customer.getCreatedAt());

        for (CustomerVehicle vehicle : customer.getVehicles()) {
            CustomerResponseDto.VehicleDto vehicleDto = new CustomerResponseDto.VehicleDto();
            vehicleDto.setId(vehicle.getId());
            vehicleDto.setPlate(vehicle.getPlate());
            vehicleDto.setVehicleVersionId(vehicle.getVehicleVersionId());
            vehicleDto.setBrandName(vehicle.getBrandName());
            vehicleDto.setModelName(vehicle.getModelName());
            vehicleDto.setYear(vehicle.getYear());
            vehicleDto.setColor(vehicle.getColor());
            vehicleDto.setNotes(vehicle.getNotes());
            vehicleDto.setCreatedAt(vehicle.getCreatedAt());
            dto.getVehicles().add(vehicleDto);
        }
        return dto;
    }

    private CustomerVehicleResponseDto mapVehicle(CustomerVehicle vehicle) {
        CustomerVehicleResponseDto dto = new CustomerVehicleResponseDto();
        dto.setId(vehicle.getId());
        dto.setCustomerId(vehicle.getCustomer() != null ? vehicle.getCustomer().getId() : null);
        dto.setPlate(vehicle.getPlate());
        dto.setVehicleVersionId(vehicle.getVehicleVersionId());
        dto.setBrandName(vehicle.getBrandName());
        dto.setModelName(vehicle.getModelName());
        dto.setYear(vehicle.getYear());
        dto.setColor(vehicle.getColor());
        dto.setNotes(vehicle.getNotes());
        dto.setCreatedAt(vehicle.getCreatedAt());
        return dto;
    }
}
