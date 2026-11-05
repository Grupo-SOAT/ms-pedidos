package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.BudgetRequestedEvent;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderEvent;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.port.message.ServiceOrderEventPublisherPort;
import br.com.fiap.postech.mspedidos.port.ServiceLineQueryPort;
import br.com.fiap.postech.mspedidos.port.monitoring.ServiceOrderMetricsPort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Applies lifecycle transitions. This class is the only writer of the service order status.
 */
public class ChangeServiceOrderStatusUseCase {

    public static final String STATUS_CHANGED_EVENT = "SERVICE_ORDER_STATUS_CHANGED";
    public static final String BUDGET_CREATED_EVENT = "BUDGET_CREATED";

    private final ServiceOrderPersistencePort persistencePort;
    private final ServiceOrderEventPublisherPort eventPublisherPort;
    private final ServiceOrderMetricsPort metricsPort;
    private final ServiceLineQueryPort serviceLineQueryPort;
    private final Clock clock;

    public ChangeServiceOrderStatusUseCase(
            ServiceOrderPersistencePort persistencePort,
            ServiceOrderEventPublisherPort eventPublisherPort,
            ServiceOrderMetricsPort metricsPort,
            ServiceLineQueryPort serviceLineQueryPort,
            Clock clock
    ) {
        this.persistencePort = persistencePort;
        this.eventPublisherPort = eventPublisherPort;
        this.metricsPort = metricsPort;
        this.serviceLineQueryPort = serviceLineQueryPort;
        this.clock = clock;
    }

    public ServiceOrder changeStatus(Long id, ServiceOrderStatus target, String correlationId) {
        ServiceOrder order = persistencePort.findById(id)
                .orElseThrow(() -> new ServiceOrderNotFoundException(id));

        ServiceOrderStatus source = order.getStatus();
        Instant now = Instant.now(clock);
        order.transitionTo(target, now);
        ServiceOrder saved = persistencePort.save(order);

        metricsPort.recordStatusTransition(source, target);
        eventPublisherPort.publish(ServiceOrderEvent.of(STATUS_CHANGED_EVENT, saved, now, correlationId));
        if (target == ServiceOrderStatus.AWAITING_APPROVAL) {
            publishBudgetRequested(saved, now, correlationId);
        }
        return saved;
    }

    public ServiceOrder applyBudget(Long id, BigDecimal totalAmount, String correlationId) {
        ServiceOrder order = persistencePort.findById(id)
                .orElseThrow(() -> new ServiceOrderNotFoundException(id));

        Instant now = Instant.now(clock);
        order.applyEstimatedAmount(totalAmount, now);
        order.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, now);
        return persistencePort.save(order);
    }

    private void publishBudgetRequested(ServiceOrder order, Instant now, String correlationId) {
        List<BudgetRequestedEvent.Item> items = serviceLineQueryPort.findByServiceOrderId(order.getId()).stream()
                .map(line -> new BudgetRequestedEvent.Item(line.catalogServiceId(), 1, List.of()))
                .toList();
        eventPublisherPort.publishBudgetRequested(BudgetRequestedEvent.of(order, items, now, correlationId));
    }
}
