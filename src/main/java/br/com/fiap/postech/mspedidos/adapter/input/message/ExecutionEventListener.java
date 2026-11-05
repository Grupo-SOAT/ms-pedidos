package br.com.fiap.postech.mspedidos.adapter.input.message;

import br.com.fiap.postech.mspedidos.adapter.input.message.event.ExecutionEvent;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ChangeServiceOrderStatusUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reflects execution progress in the service order status.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionEventListener {

    static final String EXECUTION_STARTED = "EXECUTION_STARTED";
    static final String EXECUTION_COMPLETED = "EXECUTION_COMPLETED";

    private final ObjectMapper objectMapper;
    private final ChangeServiceOrderStatusUseCase changeServiceOrderStatusUseCase;

    @KafkaListener(
            topics = "${app.events.topic.execution:execution-events}",
            groupId = "${spring.kafka.consumer.group-id:ms-pedidos-group}"
    )
    public void consume(String payload) {
        ExecutionEvent event = read(payload);
        if (EXECUTION_STARTED.equals(event.getType())) {
            changeServiceOrderStatusUseCase.changeStatus(
                    event.getServiceOrderId(), ServiceOrderStatus.IN_PROGRESS, event.getCorrelationId());
            return;
        }
        if (EXECUTION_COMPLETED.equals(event.getType())) {
            changeServiceOrderStatusUseCase.changeStatus(
                    event.getServiceOrderId(), ServiceOrderStatus.COMPLETED, event.getCorrelationId());
            return;
        }
        log.info("Ignoring execution event type={}", event.getType());
    }

    private ExecutionEvent read(String payload) {
        try {
            return objectMapper.readValue(payload, ExecutionEvent.class);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot deserialize execution event", exception);
        }
    }
}
