package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence;

import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;
import br.com.fiap.postech.mspedidos.port.ServiceLinePersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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
    public List<ServiceLine> findByServiceOrderId(Long serviceOrderId) {
        return repository.findByServiceOrderId(serviceOrderId).stream()
                .map(ServiceLinePersistenceAdapter::toDomain)
                .toList();
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
