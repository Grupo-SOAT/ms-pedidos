package br.com.fiap.postech.mspedidos.adapter.input.message;

import br.com.fiap.postech.mspedidos.adapter.input.message.event.PaymentEvent;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
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
 * Compensates the service order when a payment is rejected or times out.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    static final String PAYMENT_REJECTED = "PAYMENT_REJECTED";
    static final String PAYMENT_TIMEOUT = "PAYMENT_TIMEOUT";

    private final ObjectMapper objectMapper;
    private final ChangeServiceOrderStatusUseCase changeServiceOrderStatusUseCase;

    @KafkaListener(
            topics = "${app.events.topic.payment:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:ms-pedidos-group}"
    )
    public void consume(String payload, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        PaymentEvent event = read(payload);
        if (PAYMENT_REJECTED.equals(event.getType()) || PAYMENT_TIMEOUT.equals(event.getType())) {
            changeServiceOrderStatusUseCase.changeStatus(
                    event.getServiceOrderId(), ServiceOrderStatus.CANCELLED, event.getCorrelationId());
            return;
        }
        log.info("Ignoring payment event type={}", event.getType());
    }

    private PaymentEvent read(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot deserialize payment event", exception);
        }
    }
}
