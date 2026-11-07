package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import br.com.fiap.postech.mspedidos.adapter.output.catalog.CatalogQueryAdapter;
import br.com.fiap.postech.mspedidos.adapter.output.registration.RegistrationConflictException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.InvalidStatusTransitionException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.model.InvalidServiceLineException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.OwnerNotFoundException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceLineNotFoundException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceOrderNotFoundException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.VehicleNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ServiceOrderExceptionHandler {

    @ExceptionHandler(OwnerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOwnerNotFound(OwnerNotFoundException exception) {
        return error(HttpStatus.BAD_REQUEST, "SERVICE_ORDER_CLIENT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleVehicleNotFound(VehicleNotFoundException exception) {
        return error(HttpStatus.BAD_REQUEST, "SERVICE_ORDER_VEHICLE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ServiceOrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleServiceOrderNotFound(ServiceOrderNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "SERVICE_ORDER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ServiceLineNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleServiceLineNotFound(ServiceLineNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(CatalogQueryAdapter.CatalogServiceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCatalogNotFound(
            CatalogQueryAdapter.CatalogServiceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "CATALOG_SERVICE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidServiceLineException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidServiceLine(InvalidServiceLineException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getReason(), exception.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(InvalidStatusTransitionException exception) {
        return error(HttpStatus.CONFLICT, "STATUS_CHANGE_NOT_ALLOWED", exception.getMessage());
    }

    @ExceptionHandler(RegistrationConflictException.class)
    public ResponseEntity<Map<String, Object>> handleRegistrationConflict(RegistrationConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getReason(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, String.valueOf(exception.getMessage()), exception.getMessage());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String reason, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "reason", reason,
                "message", String.valueOf(message)
        ));
    }
}
