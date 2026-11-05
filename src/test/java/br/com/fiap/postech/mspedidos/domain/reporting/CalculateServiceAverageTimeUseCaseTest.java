package br.com.fiap.postech.mspedidos.domain.reporting;

import br.com.fiap.postech.mspedidos.port.ServiceLineQueryPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalculateServiceAverageTimeUseCaseTest {

    private static final Instant T0 = Instant.parse("2026-01-01T08:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-01T09:00:00Z");

    private final ServiceLineQueryPort queryPort = mock(ServiceLineQueryPort.class);
    private final CalculateServiceAverageTimeUseCase useCase = new CalculateServiceAverageTimeUseCase(queryPort);

    @Test
    void should_return_average_for_catalog_service() {
        when(queryPort.findByCatalogServiceId(7L)).thenReturn(List.of(
                new ServiceLineSnapshot(7L, "Troca", T0, T0, T0, T1)));

        var result = useCase.byCatalogServiceId(7L);

        assertThat(result).isPresent();
        assertThat(result.get().totalCompleted()).isEqualTo(1);
        assertThat(result.get().name()).isEqualTo("Troca");
    }

    @Test
    void should_return_empty_when_catalog_service_has_no_lines() {
        when(queryPort.findByCatalogServiceId(9L)).thenReturn(List.of());

        assertThat(useCase.byCatalogServiceId(9L)).isEmpty();
    }

    @Test
    void should_group_all_lines_by_catalog_service_id_ordered() {
        when(queryPort.findAll()).thenReturn(List.of(
                new ServiceLineSnapshot(2L, "Freio", T0, T0, T0, T1),
                new ServiceLineSnapshot(1L, "Oleo", T0, T0, T0, T1),
                new ServiceLineSnapshot(2L, "Freio", T0, null, null, null)));

        var result = useCase.all();

        assertThat(result).extracting(ServiceAverageTime::id).containsExactly(1L, 2L);
        assertThat(result.get(1).totalCreated()).isEqualTo(2);
        assertThat(result.get(1).totalCompleted()).isEqualTo(1);
    }
}
