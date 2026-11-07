package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;
import br.com.fiap.postech.mspedidos.port.CatalogQueryPort;
import br.com.fiap.postech.mspedidos.port.ServiceLinePersistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Service line CRUD inside a service order.
 */
public class ServiceLineUseCase {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ServiceOrderPersistencePort serviceOrderPersistencePort;
    private final ServiceLinePersistencePort serviceLinePersistencePort;
    private final CatalogQueryPort catalogQueryPort;
    private final Clock clock;

    public ServiceLineUseCase(
            ServiceOrderPersistencePort serviceOrderPersistencePort,
            ServiceLinePersistencePort serviceLinePersistencePort,
            CatalogQueryPort catalogQueryPort,
            Clock clock
    ) {
        this.serviceOrderPersistencePort = serviceOrderPersistencePort;
        this.serviceLinePersistencePort = serviceLinePersistencePort;
        this.catalogQueryPort = catalogQueryPort;
        this.clock = clock;
    }

    public ServiceLine create(Long serviceOrderId, Long catalogServiceId, BigDecimal price) {
        requireServiceOrder(serviceOrderId);
        var catalog = catalogQueryPort.findById(catalogServiceId);
        ServiceLine line = ServiceLine.create(serviceOrderId, catalog.id(), catalog.name(),
                price == null ? catalog.basePrice() : price, Instant.now(clock));
        return serviceLinePersistencePort.save(line);
    }

    public ServiceLine update(Long serviceOrderId, Long serviceLineId, Long catalogServiceId,
                              BigDecimal price, String status) {
        ServiceLine line = get(serviceOrderId, serviceLineId);
        if (catalogServiceId != null) {
            var catalog = catalogQueryPort.findById(catalogServiceId);
            line.updateDetails(catalog.id(), catalog.name(), price);
        } else {
            line.updateDetails(null, null, price);
        }
        if (status != null && !status.isBlank()) {
            line.moveTo(status.toUpperCase(Locale.ROOT), Instant.now(clock));
        }
        return serviceLinePersistencePort.save(line);
    }

    public void delete(Long serviceOrderId, Long serviceLineId) {
        serviceLinePersistencePort.delete(get(serviceOrderId, serviceLineId));
    }

    public ServiceLine get(Long serviceOrderId, Long serviceLineId) {
        requireServiceOrder(serviceOrderId);
        return serviceLinePersistencePort.findByIdAndServiceOrderId(serviceLineId, serviceOrderId)
                .orElseThrow(() -> new ServiceLineNotFoundException(serviceLineId));
    }

    public ServiceLinePage list(Long serviceOrderId, Long serviceLineId, Integer pageSize, String cursor) {
        requireServiceOrder(serviceOrderId);
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        long afterId = parseCursor(cursor);
        List<ServiceLine> filtered = serviceLinePersistencePort.findByServiceOrderId(serviceOrderId).stream()
                .filter(line -> serviceLineId == null || serviceLineId.equals(line.getId()))
                .filter(line -> line.getId() != null && line.getId() > afterId)
                .sorted(Comparator.comparing(ServiceLine::getId))
                .toList();
        List<ServiceLine> page = filtered.stream().limit(size).toList();
        boolean last = filtered.size() <= size;
        String nextCursor = page.isEmpty() || last
                ? null : String.valueOf(page.get(page.size() - 1).getId());
        return new ServiceLinePage(page, size, nextCursor, last);
    }

    private void requireServiceOrder(Long serviceOrderId) {
        if (!serviceOrderPersistencePort.existsById(serviceOrderId)) {
            throw new ServiceOrderNotFoundException(serviceOrderId);
        }
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

    public record ServiceLinePage(List<ServiceLine> data, int pageSize, String cursor, boolean isLast) {
    }
}
