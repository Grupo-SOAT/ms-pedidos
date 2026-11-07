package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ChangeServiceOrderStatusUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.CreateServiceOrderCascadeUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.CreateServiceOrderUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.RegisterProgressUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceLineUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceOrderNotFoundException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceOrderQueryUseCase;
import br.com.fiap.postech.mspedidos.port.RegistrationCommandPort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/service-orders")
public class ServiceOrderController {

    private final CreateServiceOrderUseCase createServiceOrderUseCase;
    private final CreateServiceOrderCascadeUseCase cascadeUseCase;
    private final ChangeServiceOrderStatusUseCase changeServiceOrderStatusUseCase;
    private final ServiceOrderQueryUseCase queryUseCase;
    private final ServiceLineUseCase serviceLineUseCase;
    private final RegisterProgressUseCase progressUseCase;
    private final ServiceOrderPersistencePort persistencePort;

    public ServiceOrderController(
            CreateServiceOrderUseCase createServiceOrderUseCase,
            CreateServiceOrderCascadeUseCase cascadeUseCase,
            ChangeServiceOrderStatusUseCase changeServiceOrderStatusUseCase,
            ServiceOrderQueryUseCase queryUseCase,
            ServiceLineUseCase serviceLineUseCase,
            RegisterProgressUseCase progressUseCase,
            ServiceOrderPersistencePort persistencePort
    ) {
        this.createServiceOrderUseCase = createServiceOrderUseCase;
        this.cascadeUseCase = cascadeUseCase;
        this.changeServiceOrderStatusUseCase = changeServiceOrderStatusUseCase;
        this.queryUseCase = queryUseCase;
        this.serviceLineUseCase = serviceLineUseCase;
        this.progressUseCase = progressUseCase;
        this.persistencePort = persistencePort;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(ServiceOrderResponse.from(created));
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
                ServiceOrderResponse.from(result.order()),
                result.services().stream().map(ServiceOrderController::toLineResponse).toList()
        ));
    }

    @GetMapping
    public ResponseEntity<PaginatedServiceOrderResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String cursor
    ) {
        var page = queryUseCase.list(status, clientId, vehicleId, pageSize, cursor);
        if (page.data().isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new PaginatedServiceOrderResponse(
                page.data().stream().map(ServiceOrderResponse::from).toList(),
                page.pageSize(), page.cursor(), page.isLast()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ServiceOrderResponse.from(queryUseCase.getById(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServiceOrderResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateServiceOrderRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        ServiceOrder updated = queryUseCase.update(id, request.clientId(), request.vehicleId(),
                request.description(), request.status(), correlationId(correlationId));
        return ResponseEntity.ok(ServiceOrderResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        queryUseCase.delete(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceOrderResponse> changeStatus(
            @PathVariable Long id,
            @RequestBody ChangeStatusRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        ServiceOrderStatus target = ServiceOrderStatus.valueOf(request.status().toUpperCase());
        ServiceOrder updated = changeServiceOrderStatusUseCase.changeStatus(id, target, correlationId(correlationId));
        return ResponseEntity.ok(ServiceOrderResponse.from(updated));
    }

    @PatchMapping("/{id}/budget")
    public ResponseEntity<ServiceOrderResponse> applyBudget(
            @PathVariable Long id,
            @RequestBody BudgetAmountRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        ServiceOrder updated = changeServiceOrderStatusUseCase.applyBudget(
                id, request.totalAmount(), correlationId(correlationId));
        return ResponseEntity.ok(ServiceOrderResponse.from(updated));
    }

    @PostMapping("/{id}/progress")
    public ResponseEntity<Void> registerProgress(
            @PathVariable Long id,
            @RequestBody ProgressRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        progressUseCase.register(id, request.action(), request.relatedServiceId(),
                correlationId(correlationId));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{id}/services")
    public ResponseEntity<PaginatedServiceLineResponse> listServices(
            @PathVariable Long id,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String cursor
    ) {
        var page = serviceLineUseCase.list(id, serviceId, pageSize, cursor);
        if (page.data().isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new PaginatedServiceLineResponse(
                page.data().stream().map(ServiceOrderController::toLineResponse).toList(),
                page.pageSize(), page.cursor(), page.isLast()));
    }

    @PostMapping("/{id}/services")
    public ResponseEntity<ServiceOrderCascadeResponse.ServiceLineResponse> createService(
            @PathVariable Long id,
            @RequestBody ServiceLineRequest request
    ) {
        ServiceLine created = serviceLineUseCase.create(
                id, request.catalogServiceId(), request.price());
        return ResponseEntity.status(HttpStatus.CREATED).body(toLineResponse(created));
    }

    @GetMapping("/{id}/services/{serviceId}")
    public ResponseEntity<ServiceOrderCascadeResponse.ServiceLineResponse> getService(
            @PathVariable Long id,
            @PathVariable Long serviceId
    ) {
        return ResponseEntity.ok(toLineResponse(serviceLineUseCase.get(id, serviceId)));
    }

    @PatchMapping("/{id}/services/{serviceId}")
    public ResponseEntity<ServiceOrderCascadeResponse.ServiceLineResponse> updateService(
            @PathVariable Long id,
            @PathVariable Long serviceId,
            @RequestBody ServiceLineRequest request
    ) {
        ServiceLine updated = serviceLineUseCase.update(
                id, serviceId, request.catalogServiceId(), request.price(), request.status());
        return ResponseEntity.ok(toLineResponse(updated));
    }

    @DeleteMapping("/{id}/services/{serviceId}")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id,
            @PathVariable Long serviceId
    ) {
        serviceLineUseCase.delete(id, serviceId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    static ServiceOrderCascadeResponse.ServiceLineResponse toLineResponse(ServiceLine line) {
        return new ServiceOrderCascadeResponse.ServiceLineResponse(
                line.getId(),
                line.getServiceOrderId(),
                line.getCatalogServiceId(),
                line.getCatalogServiceName(),
                line.getPrice(),
                line.getStatus(),
                line.getStatusLabel(),
                line.getCreatedAt(),
                line.getApprovedAt(),
                line.getStartedAt(),
                line.getCompletedAt()
        );
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
}
