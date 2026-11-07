package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServiceOrderPersistenceAdapter implements ServiceOrderPersistencePort {

    private final ServiceOrderJpaRepository repository;

    @Override
    public ServiceOrder save(ServiceOrder serviceOrder) {
        ServiceOrderEntity saved = repository.save(toEntity(serviceOrder));
        return toDomain(saved);
    }

    @Override
    public Optional<ServiceOrder> findById(Long id) {
        return repository.findById(id).map(ServiceOrderPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public List<ServiceOrder> findAll() {
        return repository.findAll().stream()
                .map(ServiceOrderPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    static ServiceOrderEntity toEntity(ServiceOrder order) {
        return ServiceOrderEntity.builder()
                .id(order.getId())
                .clientId(order.getClientId())
                .vehicleId(order.getVehicleId())
                .branchId(order.getBranchId())
                .description(order.getDescription())
                .status(order.getStatus())
                .estimatedAmount(order.getEstimatedAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .inspectedAt(order.getInspectedAt())
                .approvedAt(order.getApprovedAt())
                .rejectedAt(order.getRejectedAt())
                .cancelledAt(order.getCancelledAt())
                .startedAt(order.getStartedAt())
                .completedAt(order.getCompletedAt())
                .deliveredAt(order.getDeliveredAt())
                .partiallyRejectedAt(order.getPartiallyRejectedAt())
                .build();
    }

    static ServiceOrder toDomain(ServiceOrderEntity entity) {
        return ServiceOrder.restore(
                entity.getId(),
                entity.getClientId(),
                entity.getVehicleId(),
                entity.getBranchId(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getEstimatedAmount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getInspectedAt(),
                entity.getApprovedAt(),
                entity.getRejectedAt(),
                entity.getCancelledAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getDeliveredAt(),
                entity.getPartiallyRejectedAt()
        );
    }
}
