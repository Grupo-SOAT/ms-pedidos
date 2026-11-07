package br.com.fiap.postech.mspedidos.port;

import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;

import java.util.List;
import java.util.Optional;

public interface ServiceLinePersistencePort {

    List<ServiceLine> saveAll(List<ServiceLine> lines);

    ServiceLine save(ServiceLine line);

    List<ServiceLine> findByServiceOrderId(Long serviceOrderId);

    Optional<ServiceLine> findByIdAndServiceOrderId(Long id, Long serviceOrderId);

    void delete(ServiceLine line);
}
