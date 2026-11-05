package br.com.fiap.postech.mspedidos.adapter.input.message;

import br.com.fiap.postech.mspedidos.adapter.input.message.event.BudgetCreatedEvent;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ChangeServiceOrderStatusUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Applies the priced budget to the service order. The status is already
 * {@code AWAITING_APPROVAL}; this listener only stores the total amount.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetCreatedListener {

    static final String BUDGET_CREATED = "BUDGET_CREATED";

    private final ObjectMapper objectMapper;
    private final ChangeServiceOrderStatusUseCase changeServiceOrderStatusUseCase;

    @KafkaListener(
            topics = "${app.events.topic.budget:budget-events}",
            groupId = "${spring.kafka.consumer.group-id:ms-pedidos-group}"
    )
    public void consume(String payload, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        BudgetCreatedEvent event = read(payload);
        if (!BUDGET_CREATED.equals(event.getType()) || event.getPayload() == null) {
            log.info("Ignoring budget event type={}", event.getType());
            return;
        }
        changeServiceOrderStatusUseCase.applyBudget(
                event.getServiceOrderId(), event.getPayload().getTotalAmount(), event.getCorrelationId());
    }

    private BudgetCreatedEvent read(String payload) {
        try {
            return objectMapper.readValue(payload, BudgetCreatedEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot deserialize budget event", exception);
        }
    }
}
