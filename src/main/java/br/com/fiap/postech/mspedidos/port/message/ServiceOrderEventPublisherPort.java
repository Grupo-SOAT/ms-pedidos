package br.com.fiap.postech.mspedidos.port.message;

import br.com.fiap.postech.mspedidos.domain.serviceorder.BudgetRequestedEvent;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderEvent;

public interface ServiceOrderEventPublisherPort {

    void publish(ServiceOrderEvent event);

    void publishBudgetRequested(BudgetRequestedEvent event);
}
