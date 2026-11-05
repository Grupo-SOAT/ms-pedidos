package br.com.fiap.postech.mspedidos.port.monitoring;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;

public interface ServiceOrderMetricsPort {

    void recordStatusTransition(ServiceOrderStatus source, ServiceOrderStatus target);
}
