package br.com.fiap.postech.mspedidos.adapter.output.reporting.persistence;

import br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence.ServiceLineEntity;
import br.com.fiap.postech.mspedidos.adapter.output.serviceorder.persistence.ServiceLineJpaRepository;
import br.com.fiap.postech.mspedidos.domain.reporting.ServiceLineSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceLineQueryAdapterTest {

    private final ServiceLineJpaRepository repository = mock(ServiceLineJpaRepository.class);
    private final ServiceLineQueryAdapter adapter = new ServiceLineQueryAdapter(repository);

    @Test
    void should_map_all_lines() {
        when(repository.findAll()).thenReturn(List.of(entity(7L)));

        List<ServiceLineSnapshot> result = adapter.findAll();

        assertThat(result).containsExactly(new ServiceLineSnapshot(7L, "Troca",
                Instant.parse("2026-01-01T08:00:00Z"), Instant.parse("2026-01-01T09:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T11:00:00Z")));
    }

    @Test
    void should_map_lines_by_catalog_service_id() {
        when(repository.findByCatalogServiceId(7L)).thenReturn(List.of(entity(7L)));

        assertThat(adapter.findByCatalogServiceId(7L)).hasSize(1);
    }

    private ServiceLineEntity entity(Long catalogServiceId) {
        return ServiceLineEntity.builder()
                .catalogServiceId(catalogServiceId)
                .catalogServiceName("Troca")
                .createdAt(Instant.parse("2026-01-01T08:00:00Z"))
                .approvedAt(Instant.parse("2026-01-01T09:00:00Z"))
                .startedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .completedAt(Instant.parse("2026-01-01T11:00:00Z"))
                .build();
    }
}
