package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.message;

import br.com.fiap.postech.mspedidos.domain.serviceorder.BudgetRequestedEvent;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaServiceOrderEventPublisherTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void should_publish_status_and_budget_events_with_service_order_key() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaServiceOrderEventPublisher publisher =
                new KafkaServiceOrderEventPublisher(kafkaTemplate, new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(publisher, "topic", "service-order-events");
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "OS", NOW);
        order.assignId(7L);

        publisher.publish(ServiceOrderEvent.of("SERVICE_ORDER_STATUS_CHANGED", order, NOW, "corr"));
        publisher.publishBudgetRequested(BudgetRequestedEvent.of(order, List.of(), NOW, "corr"));

        verify(kafkaTemplate, org.mockito.Mockito.times(2))
                .send(eq("service-order-events"), eq("7"), anyString());
    }

    @Test
    void should_fail_when_event_cannot_be_serialized() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                throw new com.fasterxml.jackson.core.JsonProcessingException("boom") {
                };
            }
        };
        KafkaServiceOrderEventPublisher publisher =
                new KafkaServiceOrderEventPublisher(kafkaTemplate, failingMapper);
        ReflectionTestUtils.setField(publisher, "topic", "service-order-events");
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "OS", NOW);
        order.assignId(7L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> publisher.publish(
                        ServiceOrderEvent.of("SERVICE_ORDER_STATUS_CHANGED", order, NOW, "corr")))
                .isInstanceOf(IllegalStateException.class);
    }
}
