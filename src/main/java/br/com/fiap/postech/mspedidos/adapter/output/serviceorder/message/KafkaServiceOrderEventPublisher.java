package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.message;

import br.com.fiap.postech.mspedidos.domain.serviceorder.BudgetRequestedEvent;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderEvent;
import br.com.fiap.postech.mspedidos.port.message.ServiceOrderEventPublisherPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaServiceOrderEventPublisher implements ServiceOrderEventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.events.topic.service-order}")
    private String topic;

    @Override
    public void publish(ServiceOrderEvent event) {
        send(event.serviceOrderId(), event);
    }

    @Override
    public void publishBudgetRequested(BudgetRequestedEvent event) {
        send(event.serviceOrderId(), event);
    }

    private void send(Long serviceOrderId, Object event) {
        try {
            kafkaTemplate.send(topic, String.valueOf(serviceOrderId), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize service order event", exception);
        }
    }
}
