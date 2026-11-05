package br.com.fiap.postech.mspedidos.adapter.input.message.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class BudgetCreatedEvent {

    private String eventId;
    private String type;
    private Instant occurredAt;
    private String correlationId;
    private Long serviceOrderId;
    private Payload payload;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Payload {
        private Long budgetId;
        private BigDecimal totalAmount;
        private String currency;
    }
}
