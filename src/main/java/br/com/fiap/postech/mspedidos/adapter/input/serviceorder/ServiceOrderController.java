package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ChangeServiceOrderStatusUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.CreateServiceOrderCascadeUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.CreateServiceOrderUseCase;
import br.com.fiap.postech.mspedidos.port.RegistrationCommandPort;
import br.com.fiap.postech.mspedidos.port.ServiceLinePersistencePort;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceOrderNotFoundException;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/service-orders")
public class ServiceOrderController {

    private final CreateServiceOrderUseCase createServiceOrderUseCase;
    private final CreateServiceOrderCascadeUseCase cascadeUseCase;
    private final ChangeServiceOrderStatusUseCase changeServiceOrderStatusUseCase;
    private final ServiceOrderPersistencePort persistencePort;
    private final ServiceLinePersistencePort serviceLinePersistencePort;

    public ServiceOrderController(
            CreateServiceOrderUseCase createServiceOrderUseCase,
            CreateServiceOrderCascadeUseCase cascadeUseCase,
            ChangeServiceOrderStatusUseCase changeServiceOrderStatusUseCase,
            ServiceOrderPersistencePort persistencePort,
            ServiceLinePersistencePort serviceLinePersistencePort
    ) {
        this.createServiceOrderUseCase = createServiceOrderUseCase;
        this.cascadeUseCase = cascadeUseCase;
        this.changeServiceOrderStatusUseCase = changeServiceOrderStatusUseCase;
        this.persistencePort = persistencePort;
        this.serviceLinePersistencePort = serviceLinePersistencePort;
    }

    @PostMapping
    public ResponseEntity<ServiceOrderResponse> create(
            @RequestBody CreateServiceOrderRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        ServiceOrder created = createServiceOrderUseCase.create(
                request.clientId(),
                request.vehicleId(),
                request.branchId(),
                request.description(),
                correlationId(correlationId)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PostMapping("/cascade")
    public ResponseEntity<ServiceOrderCascadeResponse> createCascade(
            @RequestBody ServiceOrderCascadeRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        CreateServiceOrderCascadeUseCase.ServiceOrderCascadeCommand command =
                new CreateServiceOrderCascadeUseCase.ServiceOrderCascadeCommand(
                        request.ownerId(),
                        request.vehicleId(),
                        request.branchId(),
                        request.description(),
                        correlationId(correlationId),
                        toOwner(request.owner()),
                        toVehicle(request.vehicle()),
                        request.catalogServiceIds()
                );
        var result = cascadeUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ServiceOrderCascadeResponse(
                toResponse(result.order()),
                result.services().stream().map(ServiceOrderController::toLineResponse).toList()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrderResponse> findById(@PathVariable Long id) {
        ServiceOrder order = persistencePort.findById(id)
                .orElseThrow(() -> new ServiceOrderNotFoundException(id));
        return ResponseEntity.ok(toResponse(order));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceOrderResponse> changeStatus(
            @PathVariable Long id,
            @RequestBody ChangeStatusRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        ServiceOrderStatus target = ServiceOrderStatus.valueOf(request.status().toUpperCase());
        ServiceOrder updated = changeServiceOrderStatusUseCase.changeStatus(id, target, correlationId(correlationId));
        return ResponseEntity.ok(toResponse(updated));
    }

    @PatchMapping("/{id}/budget")
    public ResponseEntity<ServiceOrderResponse> applyBudget(
            @PathVariable Long id,
            @RequestBody BudgetAmountRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        ServiceOrder updated = changeServiceOrderStatusUseCase.applyBudget(
                id, request.totalAmount(), correlationId(correlationId));
        return ResponseEntity.ok(toResponse(updated));
    }

    @GetMapping("/{id}/services")
    public ResponseEntity<List<ServiceOrderCascadeResponse.ServiceLineResponse>> listServices(@PathVariable Long id) {
        return ResponseEntity.ok(serviceLinePersistencePort.findByServiceOrderId(id).stream()
                .map(ServiceOrderController::toLineResponse)
                .toList());
    }

    static ServiceOrderCascadeResponse.ServiceLineResponse toLineResponse(
            br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine line
    ) {
        return new ServiceOrderCascadeResponse.ServiceLineResponse(
                line.getId(), line.getCatalogServiceId(), line.getCatalogServiceName(),
                line.getPrice(), line.getStatus(), line.getCreatedAt());
    }

    private RegistrationCommandPort.OwnerData toOwner(ServiceOrderCascadeRequest.OwnerPayload owner) {
        return owner == null ? null : new RegistrationCommandPort.OwnerData(
                owner.name(), owner.document(), owner.documentType(), owner.phone(), owner.email());
    }

    private RegistrationCommandPort.VehicleData toVehicle(ServiceOrderCascadeRequest.VehiclePayload vehicle) {
        return vehicle == null ? null : new RegistrationCommandPort.VehicleData(
                vehicle.ownerId(), vehicle.licensePlate(), vehicle.brand(), vehicle.model(),
                vehicle.year(), vehicle.color());
    }

    private String correlationId(String headerValue) {
        return headerValue == null || headerValue.isBlank() ? MDC.get("correlation_id") : headerValue;
    }

    static ServiceOrderResponse toResponse(ServiceOrder order) {
        return new ServiceOrderResponse(
                order.getId(),
                order.getClientId(),
                order.getVehicleId(),
                order.getBranchId(),
                order.getDescription(),
                order.getStatus().name(),
                order.getEstimatedAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
