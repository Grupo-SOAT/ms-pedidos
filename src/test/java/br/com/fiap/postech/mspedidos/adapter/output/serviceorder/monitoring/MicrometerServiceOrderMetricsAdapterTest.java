package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.monitoring;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerServiceOrderMetricsAdapterTest {

    @Test
    void should_increment_transition_counter_with_tags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerServiceOrderMetricsAdapter adapter = new MicrometerServiceOrderMetricsAdapter(registry);

        adapter.recordStatusTransition(ServiceOrderStatus.PENDING, ServiceOrderStatus.IN_INSPECTION);
        adapter.recordStatusTransition(ServiceOrderStatus.PENDING, ServiceOrderStatus.IN_INSPECTION);

        assertThat(registry.get("service_order_status_transition_total")
                .tags("source", "PENDING", "target", "IN_INSPECTION")
                .counter()
                .count()).isEqualTo(2.0);
    }
}
