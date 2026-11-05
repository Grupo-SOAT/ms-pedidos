package br.com.fiap.postech.mspedidos.port;

import br.com.fiap.postech.mspedidos.domain.reporting.ServiceLineSnapshot;

import java.util.List;

public interface ServiceLineQueryPort {

    List<ServiceLineSnapshot> findAll();

    List<ServiceLineSnapshot> findByCatalogServiceId(Long catalogServiceId);

    List<ServiceLineSnapshot> findByServiceOrderId(Long serviceOrderId);
}
