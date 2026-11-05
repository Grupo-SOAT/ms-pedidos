package br.com.fiap.postech.mspedidos.port;

import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;

import java.util.List;

public interface ServiceLinePersistencePort {

    List<ServiceLine> saveAll(List<ServiceLine> lines);

    List<ServiceLine> findByServiceOrderId(Long serviceOrderId);
}
