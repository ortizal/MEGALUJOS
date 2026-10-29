package ec.com.megalujos.services.api;

import ec.com.megalujos.services.application.ServiceOrderService;
import ec.com.megalujos.services.dto.AgendaDayDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Agenda de Servicios", description = "Órdenes programadas por fecha (ventana de ±7 días)")
public class ServicesAgendaController {

    private final ServiceOrderService serviceOrderService;

    public ServicesAgendaController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    @GetMapping("/agenda")
    @PreAuthorize("hasAuthority('services:read') or hasRole('ADMIN')")
    @Operation(summary = "Agenda de servicios", description = "Órdenes con fecha programada entre hoy -7 y hoy +7 días, agrupadas por fecha (America/Guayaquil)")
    public ResponseEntity<List<AgendaDayDto>> agenda() {
        return ResponseEntity.ok(serviceOrderService.agenda());
    }
}
