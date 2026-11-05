package br.com.fiap.postech.mspedidos.domain.serviceorder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event asking the budget service to price the service order. It carries references only.
 */
public record BudgetRequestedEvent(
        String eventId,
        String type,
        Instant occurredAt,
        String correlationId,
        Long serviceOrderId,
        Long branchId,
        Payload payload
) {

    public static final String TYPE = "BUDGET_REQUESTED";

    public static BudgetRequestedEvent of(ServiceOrder order, List<Item> items, Instant now,
                                          String correlationId) {
        return new BudgetRequestedEvent(UUID.randomUUID().toString(), TYPE, now, correlationId,
                order.getId(), order.getBranchId(), new Payload(order.getId(), "BRL", items));
    }

    public record Payload(Long serviceOrderId, String currency, List<Item> items) {
    }

    public record Item(Long catalogServiceId, int quantity, List<Supply> supplies) {
    }

    public record Supply(Long supplyId, int quantity) {
    }
}
