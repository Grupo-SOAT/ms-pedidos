package br.com.fiap.postech.mspedidos.domain.reporting;

import java.time.Instant;

/**
 * Immutable view of a service line used by the average time report.
 */
public record ServiceLineSnapshot(
        Long catalogServiceId,
        String catalogServiceName,
        Instant createdAt,
        Instant approvedAt,
        Instant startedAt,
        Instant completedAt
) {
}
