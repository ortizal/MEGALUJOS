package ec.com.megalujos.customers.api;

import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.customers.application.CustomerService;
import ec.com.megalujos.customers.dto.CustomerRequest;
import ec.com.megalujos.customers.dto.CustomerResponseDto;
import ec.com.megalujos.customers.dto.CustomerVehicleRequest;
import ec.com.megalujos.customers.dto.CustomerVehicleResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Clientes", description = "Gestión de clientes y vehículos asociados")
public class CustomersController {

    private final CustomerService customerService;

    public CustomersController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customers:manage') or hasRole('ADMIN')")
    @Operation(summary = "Listar clientes", description = "Lista paginada de clientes con filtro de texto por nombre, documento o teléfono")
    public ResponseEntity<PageResponse<CustomerResponseDto>> getCustomers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(customerService.list(q, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customers:manage') or hasRole('ADMIN')")
    @Operation(summary = "Crear cliente", description = "Crea un cliente validando tipo de documento, número obligatorio (salvo consumidor final) y documento duplicado")
    public ResponseEntity<CustomerResponseDto> createCustomer(
            @Valid @RequestBody CustomerRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CustomerResponseDto response = customerService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customers:manage') or hasRole('ADMIN')")
    @Operation(summary = "Detalle de cliente", description = "Consulta un cliente con sus vehículos registrados")
    public ResponseEntity<CustomerResponseDto> getCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customers:manage') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar cliente", description = "Actualiza los datos de un cliente validando documento duplicado")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(customerService.update(id, request, principal.getId()));
    }

    @GetMapping("/{id}/vehicles")
    @PreAuthorize("hasAuthority('customers:manage') or hasRole('ADMIN')")
    @Operation(summary = "Listar vehículos del cliente", description = "Lista los vehículos registrados para un cliente")
    public ResponseEntity<List<CustomerVehicleResponseDto>> getVehicles(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.listVehicles(id));
    }

    @PostMapping("/{id}/vehicles")
    @PreAuthorize("hasAuthority('customers:manage') or hasRole('ADMIN')")
    @Operation(summary = "Registrar vehículo al cliente", description = "Agrega un vehículo validando la versión de vehículo si se envía")
    public ResponseEntity<CustomerVehicleResponseDto> addVehicle(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerVehicleRequest request
    ) {
        CustomerVehicleResponseDto response = customerService.addVehicle(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/vehicles/{vehicleId}")
    @PreAuthorize("hasAuthority('customers:manage') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar vehículo del cliente", description = "Actualiza los datos de un vehículo del cliente")
    public ResponseEntity<CustomerVehicleResponseDto> updateVehicle(
            @PathVariable UUID id,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody CustomerVehicleRequest request
    ) {
        return ResponseEntity.ok(customerService.updateVehicle(id, vehicleId, request));
    }

    @DeleteMapping("/{id}/vehicles/{vehicleId}")
    @PreAuthorize("hasAuthority('customers:manage') or hasRole('ADMIN')")
    @Operation(summary = "Eliminar vehículo del cliente", description = "Elimina un vehículo registrado del cliente")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable UUID id,
            @PathVariable UUID vehicleId
    ) {
        customerService.deleteVehicle(id, vehicleId);
        return ResponseEntity.noContent().build();
    }
}
