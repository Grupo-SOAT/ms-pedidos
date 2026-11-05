package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceOrderJpaRepository extends JpaRepository<ServiceOrderEntity, Long> {
}
