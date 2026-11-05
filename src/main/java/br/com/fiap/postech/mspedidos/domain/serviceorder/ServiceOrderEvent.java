package br.com.fiap.postech.mspedidos.domain.serviceorder;

import java.time.Instant;
import java.util.UUID;

/**
 * Event emitted by the service order aggregate. The type is a plain string so producers and
 * consumers are not coupled to a shared enum.
 */
public record ServiceOrderEvent(
        String eventId,
        String type,
        Long serviceOrderId,
        Long branchId,
        ServiceOrderStatus status,
        Instant occurredAt,
        String correlationId
) {

    public static ServiceOrderEvent of(String type, ServiceOrder order, Instant now, String correlationId) {
        return new ServiceOrderEvent(UUID.randomUUID().toString(), type, order.getId(), order.getBranchId(),
                order.getStatus(), now, correlationId);
    }
}
