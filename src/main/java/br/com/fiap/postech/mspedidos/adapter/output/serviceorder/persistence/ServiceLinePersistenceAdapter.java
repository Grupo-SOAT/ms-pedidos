package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence;

import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;
import br.com.fiap.postech.mspedidos.port.ServiceLinePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServiceLinePersistenceAdapter implements ServiceLinePersistencePort {

    private final ServiceLineJpaRepository repository;

    @Override
    public List<ServiceLine> saveAll(List<ServiceLine> lines) {
        return repository.saveAll(lines.stream().map(ServiceLinePersistenceAdapter::toEntity).toList())
                .stream()
                .map(ServiceLinePersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public ServiceLine save(ServiceLine line) {
        return toDomain(repository.save(toEntity(line)));
    }

    @Override
    public List<ServiceLine> findByServiceOrderId(Long serviceOrderId) {
        return repository.findByServiceOrderId(serviceOrderId).stream()
                .map(ServiceLinePersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<ServiceLine> findByIdAndServiceOrderId(Long id, Long serviceOrderId) {
        return repository.findById(id)
                .filter(entity -> serviceOrderId.equals(entity.getServiceOrderId()))
                .map(ServiceLinePersistenceAdapter::toDomain);
    }

    @Override
    public void delete(ServiceLine line) {
        repository.deleteById(line.getId());
    }

    static ServiceLineEntity toEntity(ServiceLine line) {
        return ServiceLineEntity.builder()
                .id(line.getId())
                .serviceOrderId(line.getServiceOrderId())
                .catalogServiceId(line.getCatalogServiceId())
                .catalogServiceName(line.getCatalogServiceName())
                .price(line.getPrice())
                .status(line.getStatus())
                .createdAt(line.getCreatedAt())
                .approvedAt(line.getApprovedAt())
                .startedAt(line.getStartedAt())
                .completedAt(line.getCompletedAt())
                .build();
    }

    static ServiceLine toDomain(ServiceLineEntity entity) {
        return ServiceLine.restore(entity.getId(), entity.getServiceOrderId(), entity.getCatalogServiceId(),
                entity.getCatalogServiceName(), entity.getPrice(), entity.getStatus(), entity.getCreatedAt(),
                entity.getApprovedAt(), entity.getStartedAt(), entity.getCompletedAt());
    }
}
