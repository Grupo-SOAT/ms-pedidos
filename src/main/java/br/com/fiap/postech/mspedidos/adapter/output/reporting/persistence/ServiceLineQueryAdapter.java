package br.com.fiap.postech.mspedidos.adapter.output.reporting.persistence;

import br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence.ServiceLineEntity;
import br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence.ServiceLineJpaRepository;
import br.com.fiap.postech.mspedidos.domain.reporting.ServiceLineSnapshot;
import br.com.fiap.postech.mspedidos.port.ServiceLineQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceLineQueryAdapter implements ServiceLineQueryPort {

    private final ServiceLineJpaRepository repository;

    @Override
    public List<ServiceLineSnapshot> findAll() {
        return repository.findAll().stream().map(ServiceLineQueryAdapter::toSnapshot).toList();
    }

    @Override
    public List<ServiceLineSnapshot> findByCatalogServiceId(Long catalogServiceId) {
        return repository.findByCatalogServiceId(catalogServiceId).stream()
                .map(ServiceLineQueryAdapter::toSnapshot)
                .toList();
    }

    @Override
    public List<ServiceLineSnapshot> findByServiceOrderId(Long serviceOrderId) {
        return repository.findByServiceOrderId(serviceOrderId).stream()
                .map(ServiceLineQueryAdapter::toSnapshot)
                .toList();
    }

    static ServiceLineSnapshot toSnapshot(ServiceLineEntity entity) {
        return new ServiceLineSnapshot(
                entity.getCatalogServiceId(),
                entity.getCatalogServiceName(),
                entity.getCreatedAt(),
                entity.getApprovedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }
}
