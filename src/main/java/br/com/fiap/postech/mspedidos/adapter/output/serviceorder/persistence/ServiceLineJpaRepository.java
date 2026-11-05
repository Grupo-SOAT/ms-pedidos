package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceLineJpaRepository extends JpaRepository<ServiceLineEntity, Long> {

    List<ServiceLineEntity> findByServiceOrderId(Long serviceOrderId);

    List<ServiceLineEntity> findByCatalogServiceId(Long catalogServiceId);
}
