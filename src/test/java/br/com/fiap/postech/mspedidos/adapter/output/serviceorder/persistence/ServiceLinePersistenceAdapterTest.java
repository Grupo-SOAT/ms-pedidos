package br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence;

import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceLinePersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final ServiceLineJpaRepository repository = mock(ServiceLineJpaRepository.class);
    private final ServiceLinePersistenceAdapter adapter = new ServiceLinePersistenceAdapter(repository);

    @Test
    void should_save_all_lines() {
        when(repository.saveAll(any())).thenAnswer(invocation -> {
            List<ServiceLineEntity> entities = invocation.getArgument(0);
            entities.forEach(entity -> entity.setId(9L));
            return entities;
        });
        ServiceLine line = ServiceLine.create(5L, 3L, "Oleo", BigDecimal.TEN, NOW);

        List<ServiceLine> saved = adapter.saveAll(List.of(line));

        assertThat(saved).usingRecursiveFieldByFieldElementComparator()
                .containsExactly(ServiceLine.restore(9L, 5L, 3L, "Oleo", BigDecimal.TEN,
                        "AWAITING_APPROVAL", NOW, null, null, null));
    }

    @Test
    void should_find_lines_by_service_order_id() {
        ServiceLineEntity entity = ServiceLineEntity.builder()
                .id(9L)
                .serviceOrderId(5L)
                .catalogServiceId(3L)
                .catalogServiceName("Oleo")
                .price(BigDecimal.TEN)
                .status("AWAITING_APPROVAL")
                .createdAt(NOW)
                .build();
        when(repository.findByServiceOrderId(5L)).thenReturn(List.of(entity));

        assertThat(adapter.findByServiceOrderId(5L)).hasSize(1);
    }
}
