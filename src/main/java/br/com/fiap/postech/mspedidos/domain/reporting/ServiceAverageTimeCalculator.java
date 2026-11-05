package br.com.fiap.postech.mspedidos.domain.reporting;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Computes average execution times from service line timestamps.
 */
public final class ServiceAverageTimeCalculator {

    private static final double MILLIS_PER_HOUR = 3_600_000.0;

    private ServiceAverageTimeCalculator() {
    }

    public static ServiceAverageTime calculate(Long catalogServiceId, String name, List<ServiceLineSnapshot> lines) {
        long totalCompleted = lines.stream()
                .filter(line -> line.completedAt() != null)
                .count();
        return new ServiceAverageTime(
                catalogServiceId,
                name,
                lines.size(),
                totalCompleted,
                average(lines, ServiceLineSnapshot::createdAt, ServiceLineSnapshot::completedAt),
                average(lines, ServiceLineSnapshot::startedAt, ServiceLineSnapshot::completedAt),
                average(lines, ServiceLineSnapshot::approvedAt, ServiceLineSnapshot::completedAt),
                average(lines, ServiceLineSnapshot::createdAt, ServiceLineSnapshot::approvedAt)
        );
    }

    private static double average(
            List<ServiceLineSnapshot> lines,
            java.util.function.Function<ServiceLineSnapshot, Instant> start,
            java.util.function.Function<ServiceLineSnapshot, Instant> end
    ) {
        return lines.stream()
                .map(line -> hoursBetween(start.apply(line), end.apply(line)))
                .flatMap(Optional::stream)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private static Optional<Double> hoursBetween(Instant start, Instant end) {
        if (start == null || end == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(start, end).toMillis() / MILLIS_PER_HOUR);
    }
}
