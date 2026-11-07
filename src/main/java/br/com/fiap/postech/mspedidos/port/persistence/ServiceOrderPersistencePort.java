package br.com.fiap.postech.mspedidos.port.persistence;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;

import java.util.List;
import java.util.Optional;

public interface ServiceOrderPersistencePort {

    ServiceOrder save(ServiceOrder serviceOrder);

    Optional<ServiceOrder> findById(Long id);

    boolean existsById(Long id);

    List<ServiceOrder> findAll();

    void deleteById(Long id);
}
