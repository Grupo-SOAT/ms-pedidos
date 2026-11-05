package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;
import br.com.fiap.postech.mspedidos.port.CatalogQueryPort;
import br.com.fiap.postech.mspedidos.port.RegistrationCommandPort;
import br.com.fiap.postech.mspedidos.port.ServiceLinePersistencePort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Creates a service order and its service lines. Owner/vehicle data, when supplied, is created in
 * the registration service before the local transaction.
 */
public class CreateServiceOrderCascadeUseCase {

    private final CreateServiceOrderUseCase createServiceOrderUseCase;
    private final RegistrationCommandPort registrationCommandPort;
    private final CatalogQueryPort catalogQueryPort;
    private final ServiceLinePersistencePort serviceLinePersistencePort;
    private final Clock clock;

    public CreateServiceOrderCascadeUseCase(
            CreateServiceOrderUseCase createServiceOrderUseCase,
            RegistrationCommandPort registrationCommandPort,
            CatalogQueryPort catalogQueryPort,
            ServiceLinePersistencePort serviceLinePersistencePort,
            Clock clock
    ) {
        this.createServiceOrderUseCase = createServiceOrderUseCase;
        this.registrationCommandPort = registrationCommandPort;
        this.catalogQueryPort = catalogQueryPort;
        this.serviceLinePersistencePort = serviceLinePersistencePort;
        this.clock = clock;
    }

    public CascadeResult execute(ServiceOrderCascadeCommand command) {
        Objects.requireNonNull(command.branchId(), "branchId is required");

        Long ownerId = command.owner() != null
                ? registrationCommandPort.createOwner(command.owner())
                : command.ownerId();
        Long vehicleId = command.vehicle() != null
                ? registrationCommandPort.createVehicle(command.vehicle().withOwnerId(ownerId))
                : command.vehicleId();

        ServiceOrder order = createServiceOrderUseCase.create(
                ownerId, vehicleId, command.branchId(), command.description(), command.correlationId());

        Instant now = Instant.now(clock);
        List<ServiceLine> lines = command.catalogServiceIds().stream()
                .map(catalogId -> {
                    var catalog = catalogQueryPort.findById(catalogId);
                    return ServiceLine.create(order.getId(), catalog.id(), catalog.name(), catalog.basePrice(), now);
                })
                .toList();

        return new CascadeResult(order, serviceLinePersistencePort.saveAll(lines));
    }

    public record ServiceOrderCascadeCommand(
            Long ownerId,
            Long vehicleId,
            Long branchId,
            String description,
            String correlationId,
            RegistrationCommandPort.OwnerData owner,
            RegistrationCommandPort.VehicleData vehicle,
            List<Long> catalogServiceIds
    ) {

        public ServiceOrderCascadeCommand {
            catalogServiceIds = catalogServiceIds == null ? List.of() : List.copyOf(catalogServiceIds);
        }
    }

    public record CascadeResult(ServiceOrder order, List<ServiceLine> services) {
    }
}
