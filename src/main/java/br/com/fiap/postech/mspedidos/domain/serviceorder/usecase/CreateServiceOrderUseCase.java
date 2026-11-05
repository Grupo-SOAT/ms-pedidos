package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderEvent;
import br.com.fiap.postech.mspedidos.port.message.ServiceOrderEventPublisherPort;
import br.com.fiap.postech.mspedidos.port.persistence.OwnerExistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.VehicleExistencePort;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Creates a service order after validating its owner, vehicle and branch.
 */
public class CreateServiceOrderUseCase {

    public static final String CREATED_EVENT = "SERVICE_ORDER_CREATED";

    private final ServiceOrderPersistencePort persistencePort;
    private final OwnerExistencePort ownerExistencePort;
    private final VehicleExistencePort vehicleExistencePort;
    private final ServiceOrderEventPublisherPort eventPublisherPort;
    private final Clock clock;

    public CreateServiceOrderUseCase(
            ServiceOrderPersistencePort persistencePort,
            OwnerExistencePort ownerExistencePort,
            VehicleExistencePort vehicleExistencePort,
            ServiceOrderEventPublisherPort eventPublisherPort,
            Clock clock
    ) {
        this.persistencePort = persistencePort;
        this.ownerExistencePort = ownerExistencePort;
        this.vehicleExistencePort = vehicleExistencePort;
        this.eventPublisherPort = eventPublisherPort;
        this.clock = clock;
    }

    public ServiceOrder create(
            Long clientId,
            Long vehicleId,
            Long branchId,
            String description,
            String correlationId
    ) {
        Objects.requireNonNull(branchId, "branchId is required");
        if (!ownerExistencePort.existsById(clientId)) {
            throw new OwnerNotFoundException(clientId);
        }
        if (!vehicleExistencePort.vehicleExistsById(vehicleId)) {
            throw new VehicleNotFoundException(vehicleId);
        }

        Instant now = Instant.now(clock);
        ServiceOrder order = ServiceOrder.create(clientId, vehicleId, branchId, description, now);
        ServiceOrder saved = persistencePort.save(order);
        eventPublisherPort.publish(ServiceOrderEvent.of(CREATED_EVENT, saved, now, correlationId));
        return saved;
    }
}
