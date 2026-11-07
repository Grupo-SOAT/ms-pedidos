package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;
import br.com.fiap.postech.mspedidos.port.ServiceLinePersistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Applies explicit progress actions to a service order and its service lines.
 */
public class RegisterProgressUseCase {

    private final ServiceOrderPersistencePort serviceOrderPersistencePort;
    private final ServiceLinePersistencePort serviceLinePersistencePort;
    private final ChangeServiceOrderStatusUseCase changeStatusUseCase;
    private final Clock clock;

    public RegisterProgressUseCase(
            ServiceOrderPersistencePort serviceOrderPersistencePort,
            ServiceLinePersistencePort serviceLinePersistencePort,
            ChangeServiceOrderStatusUseCase changeStatusUseCase,
            Clock clock
    ) {
        this.serviceOrderPersistencePort = serviceOrderPersistencePort;
        this.serviceLinePersistencePort = serviceLinePersistencePort;
        this.changeStatusUseCase = changeStatusUseCase;
        this.clock = clock;
    }

    public void register(Long serviceOrderId, String action, Long relatedServiceId,
                         String correlationId) {
        ServiceOrder order = serviceOrderPersistencePort.findById(serviceOrderId)
                .orElseThrow(() -> new ServiceOrderNotFoundException(serviceOrderId));

        String normalized = action == null ? "" : action.toUpperCase(Locale.ROOT);
        Instant now = Instant.now(clock);
        switch (normalized) {
            case "START_INSPECTION" -> changeStatusUseCase.changeStatus(
                    serviceOrderId, ServiceOrderStatus.IN_INSPECTION, correlationId);
            case "COMPLETE_INSPECTION" -> changeStatusUseCase.changeStatus(
                    serviceOrderId, ServiceOrderStatus.AWAITING_APPROVAL, correlationId);
            case "DELIVER_VEHICLE" -> changeStatusUseCase.changeStatus(
                    serviceOrderId, ServiceOrderStatus.DELIVERED, correlationId);
            case "START_SERVICE" -> startService(order, relatedServiceId, now, correlationId);
            case "COMPLETE_SERVICE" -> completeService(order, relatedServiceId, now, correlationId);
            case "CANCEL_SERVICE" -> cancelService(order, relatedServiceId, now);
            default -> throw new IllegalArgumentException("UNSUPPORTED_PROGRESS_ACTION");
        }
    }

    private void startService(ServiceOrder order, Long serviceLineId, Instant now,
                              String correlationId) {
        ServiceLine line = requireLine(order, serviceLineId);
        if ("AWAITING_APPROVAL".equals(line.getStatus())) {
            line.approve(now);
        }
        line.start(now);
        serviceLinePersistencePort.save(line);
        if (order.getStatus() == ServiceOrderStatus.APPROVED) {
            changeStatusUseCase.changeStatus(order.getId(), ServiceOrderStatus.IN_PROGRESS, correlationId);
        }
    }

    private void completeService(ServiceOrder order, Long serviceLineId, Instant now,
                                 String correlationId) {
        ServiceLine line = requireLine(order, serviceLineId);
        line.complete(now);
        serviceLinePersistencePort.save(line);

        List<ServiceLine> lines = serviceLinePersistencePort.findByServiceOrderId(order.getId());
        boolean allCompleted = !lines.isEmpty()
                && lines.stream().allMatch(item -> "COMPLETED".equals(item.getStatus()));
        if (allCompleted && order.getStatus() == ServiceOrderStatus.IN_PROGRESS) {
            changeStatusUseCase.changeStatus(order.getId(), ServiceOrderStatus.COMPLETED, correlationId);
        }
    }

    private void cancelService(ServiceOrder order, Long serviceLineId, Instant now) {
        ServiceLine line = requireLine(order, serviceLineId);
        line.cancel(now);
        serviceLinePersistencePort.save(line);
    }

    private ServiceLine requireLine(ServiceOrder order, Long serviceLineId) {
        if (serviceLineId == null) {
            throw new IllegalArgumentException("RELATED_SERVICE_ID_REQUIRED");
        }
        return serviceLinePersistencePort.findByIdAndServiceOrderId(serviceLineId, order.getId())
                .orElseThrow(() -> new ServiceLineNotFoundException(serviceLineId));
    }
}
