package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.monitoring;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.port.monitoring.ServiceOrderMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerServiceOrderMetricsAdapter implements ServiceOrderMetricsPort {

    private final MeterRegistry meterRegistry;

    public MicrometerServiceOrderMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordStatusTransition(ServiceOrderStatus source, ServiceOrderStatus target) {
        Counter.builder("service_order_status_transition_total")
                .tag("source", source.name())
                .tag("target", target.name())
                .register(meterRegistry)
                .increment();
    }
}
