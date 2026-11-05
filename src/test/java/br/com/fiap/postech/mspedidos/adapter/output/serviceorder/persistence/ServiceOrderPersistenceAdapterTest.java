package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceOrderPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final ServiceOrderJpaRepository repository = mock(ServiceOrderJpaRepository.class);
    private final ServiceOrderPersistenceAdapter adapter = new ServiceOrderPersistenceAdapter(repository);

    @Test
    void should_save_and_map_back_to_domain() {
        when(repository.save(any(ServiceOrderEntity.class))).thenAnswer(invocation -> {
            ServiceOrderEntity entity = invocation.getArgument(0);
            entity.setId(7L);
            return entity;
        });
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);

        ServiceOrder saved = adapter.save(order);

        assertThat(saved).usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(order);
        assertThat(saved.getId()).isEqualTo(7L);
    }

    @Test
    void should_find_by_id() {
        ServiceOrderEntity entity = ServiceOrderEntity.builder()
                .id(7L)
                .clientId(1L)
                .vehicleId(2L)
                .branchId(3L)
                .description("Revisao")
                .status(ServiceOrderStatus.PENDING)
                .estimatedAmount(BigDecimal.TEN)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        when(repository.findById(7L)).thenReturn(Optional.of(entity));

        Optional<ServiceOrder> found = adapter.findById(7L);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ServiceOrderStatus.PENDING);
        assertThat(found.get().getBranchId()).isEqualTo(3L);
    }

    @Test
    void should_return_empty_when_not_found() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(9L)).isEmpty();
    }

    @Test
    void should_delegate_exists_by_id() {
        when(repository.existsById(7L)).thenReturn(true);

        assertThat(adapter.existsById(7L)).isTrue();
        when(repository.existsById(8L)).thenReturn(false);
        assertThat(adapter.existsById(8L)).isFalse();
        verify(repository).existsById(7L);
    }
}
