package br.com.fiap.postech.mspedidos.domain.reporting;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceAverageTimeCalculatorTest {

    private static final Instant T0 = Instant.parse("2026-01-01T08:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-01T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T3 = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    void should_compute_totals_and_averages() {
        var line = new ServiceLineSnapshot(7L, "Troca de oleo", T0, T1, T2, T3);

        ServiceAverageTime result = ServiceAverageTimeCalculator.calculate(7L, "Troca de oleo", List.of(line));

        assertThat(result).usingRecursiveComparison().isEqualTo(
                new ServiceAverageTime(7L, "Troca de oleo", 1, 1, 4.0, 2.0, 3.0, 1.0));
    }

    @Test
    void should_ignore_missing_timestamps() {
        var completed = new ServiceLineSnapshot(7L, "Troca", T0, T1, T2, T3);
        var pending = new ServiceLineSnapshot(7L, "Troca", T1, null, null, null);

        ServiceAverageTime result = ServiceAverageTimeCalculator.calculate(
                7L, "Troca", List.of(completed, pending));

        assertThat(result.totalCreated()).isEqualTo(2);
        assertThat(result.totalCompleted()).isEqualTo(1);
        assertThat(result.averageTimeBetweenCreateAndComplete()).isEqualTo(4.0);
        assertThat(result.averageTimeAwaitingBudgetApproval()).isEqualTo(1.0);
    }

    @Test
    void should_return_zero_averages_when_no_timestamps_are_available() {
        var line = new ServiceLineSnapshot(7L, "Troca", null, null, null, null);

        ServiceAverageTime result = ServiceAverageTimeCalculator.calculate(7L, "Troca", List.of(line));

        assertThat(result).usingRecursiveComparison().isEqualTo(
                new ServiceAverageTime(7L, "Troca", 1, 0, 0.0, 0.0, 0.0, 0.0));
    }
}
