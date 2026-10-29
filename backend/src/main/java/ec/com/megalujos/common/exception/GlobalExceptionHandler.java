package ec.com.megalujos.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.warn("ResourceNotFoundException [{}]: {}", traceId, ex.getMessage());

        ProblemDetails details = new ProblemDetails(
                "urn:megalujos:error:not-found",
                "Recurso no encontrado",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(details);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetails> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.warn("BusinessRuleException [{}]: {}", traceId, ex.getMessage());

        ProblemDetails details = new ProblemDetails(
                "urn:megalujos:error:" + ex.getCode().toLowerCase(),
                "Violación de regla de negocio",
                ex.getStatus().value(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.status(ex.getStatus()).body(details);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }

        ProblemDetails details = new ProblemDetails(
                "urn:megalujos:error:validation",
                "Error de validación en la solicitud",
                HttpStatus.BAD_REQUEST.value(),
                "Uno o más campos contienen errores de formato o valores no válidos",
                request.getRequestURI(),
                traceId
        );
        details.setFieldErrors(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(details);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetails> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        ProblemDetails details = new ProblemDetails(
                "urn:megalujos:error:unauthorized",
                "Credenciales no válidas",
                HttpStatus.UNAUTHORIZED.value(),
                "Usuario o contraseña incorrectos",
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(details);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetails> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        ProblemDetails details = new ProblemDetails(
                "urn:megalujos:error:forbidden",
                "Acceso denegado",
                HttpStatus.FORBIDDEN.value(),
                "No cuenta con los permisos requeridos para ejecutar esta operación",
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleGeneral(Exception ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("Internal server error [{}]: ", traceId, ex);

        ProblemDetails details = new ProblemDetails(
                "urn:megalujos:error:internal",
                "Error interno del servidor",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocurrió un error inesperado. Por favor contacte con soporte indicando el traceId.",
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(details);
    }
}
