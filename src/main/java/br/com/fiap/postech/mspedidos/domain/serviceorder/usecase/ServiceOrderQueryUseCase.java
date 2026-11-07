package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.port.persistence.OwnerExistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.VehicleExistencePort;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Listing, update and removal of service orders.
 */
public class ServiceOrderQueryUseCase {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ServiceOrderPersistencePort persistencePort;
    private final OwnerExistencePort ownerExistencePort;
    private final VehicleExistencePort vehicleExistencePort;
    private final Clock clock;

    public ServiceOrderQueryUseCase(
            ServiceOrderPersistencePort persistencePort,
            OwnerExistencePort ownerExistencePort,
            VehicleExistencePort vehicleExistencePort,
            Clock clock
    ) {
        this.persistencePort = persistencePort;
        this.ownerExistencePort = ownerExistencePort;
        this.vehicleExistencePort = vehicleExistencePort;
        this.clock = clock;
    }

    public ServiceOrderPage list(String status, Long clientId, Long vehicleId,
                                 Integer pageSize, String cursor) {
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        long afterId = parseCursor(cursor);
        ServiceOrderStatus filter = status == null || status.isBlank()
                ? null : ServiceOrderStatus.valueOf(status.toUpperCase(Locale.ROOT));

        List<ServiceOrder> filtered = persistencePort.findAll().stream()
                .filter(order -> filter == null || order.getStatus() == filter)
                .filter(order -> filter != null
                        || (order.getStatus() != ServiceOrderStatus.COMPLETED
                        && order.getStatus() != ServiceOrderStatus.DELIVERED))
                .filter(order -> clientId == null || Objects.equals(clientId, order.getClientId()))
                .filter(order -> vehicleId == null || Objects.equals(vehicleId, order.getVehicleId()))
                .filter(order -> order.getId() != null && order.getId() > afterId)
                .sorted(Comparator.comparingInt((ServiceOrder order) -> priority(order.getStatus()))
                        .thenComparing(ServiceOrder::getId))
                .toList();

        List<ServiceOrder> page = filtered.stream().limit(size).toList();
        boolean last = filtered.size() <= size;
        String nextCursor = page.isEmpty() || last
                ? null : String.valueOf(page.get(page.size() - 1).getId());
        return new ServiceOrderPage(page, size, nextCursor, last);
    }

    public ServiceOrder getById(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ServiceOrderNotFoundException(id));
    }

    public ServiceOrder update(Long id, Long clientId, Long vehicleId, String description,
                               String status, String correlationId) {
        ServiceOrder order = getById(id);

        if (clientId != null && !ownerExistencePort.existsById(clientId)) {
            throw new OwnerNotFoundException(clientId);
        }
        if (vehicleId != null && !vehicleExistencePort.vehicleExistsById(vehicleId)) {
            throw new VehicleNotFoundException(vehicleId);
        }

        Instant now = Instant.now(clock);
        order.updateDetails(clientId, vehicleId, description, now);
        if (status != null && !status.isBlank()) {
            order.transitionTo(ServiceOrderStatus.valueOf(status.toUpperCase(Locale.ROOT)), now);
        }
        return persistencePort.save(order);
    }

    public void delete(Long id) {
        getById(id);
        persistencePort.deleteById(id);
    }

    private int priority(ServiceOrderStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case IN_INSPECTION -> 1;
            case AWAITING_APPROVAL -> 2;
            case PARTIALLY_REJECTED -> 3;
            case APPROVED -> 4;
            case IN_PROGRESS -> 5;
            case CANCELLED -> 6;
            case COMPLETED -> 7;
            case DELIVERED -> 8;
        };
    }

    private long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    public record ServiceOrderPage(List<ServiceOrder> data, int pageSize, String cursor, boolean isLast) {
    }
}
