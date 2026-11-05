package br.com.fiap.postech.mspedidos.domain.serviceorder.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceLineTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T11:00:00Z");

    @Test
    void should_create_awaiting_approval_line() {
        ServiceLine line = ServiceLine.create(5L, 3L, "Oleo", BigDecimal.TEN, NOW);

        assertThat(line).usingRecursiveComparison()
                .isEqualTo(ServiceLine.restore(null, 5L, 3L, "Oleo", BigDecimal.TEN,
                        "AWAITING_APPROVAL", NOW, null, null, null));
    }

    @Test
    void should_expose_restored_timestamps_and_id() {
        ServiceLine line = ServiceLine.restore(9L, 5L, 3L, "Oleo", BigDecimal.TEN,
                "COMPLETED", NOW, LATER, LATER, LATER);

        assertThat(line.getId()).isEqualTo(9L);
        assertThat(line.getServiceOrderId()).isEqualTo(5L);
        assertThat(line.getCatalogServiceId()).isEqualTo(3L);
        assertThat(line.getCatalogServiceName()).isEqualTo("Oleo");
        assertThat(line.getPrice()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(line.getStatus()).isEqualTo("COMPLETED");
        assertThat(line.getCreatedAt()).isEqualTo(NOW);
        assertThat(line.getApprovedAt()).isEqualTo(LATER);
        assertThat(line.getStartedAt()).isEqualTo(LATER);
        assertThat(line.getCompletedAt()).isEqualTo(LATER);
    }

    @Test
    void should_assign_id() {
        ServiceLine line = ServiceLine.create(5L, 3L, "Oleo", BigDecimal.TEN, NOW);

        line.assignId(9L);

        assertThat(line.getId()).isEqualTo(9L);
    }

    @Test
    void should_reject_missing_required_fields() {
        assertThatThrownBy(() -> ServiceLine.create(null, 3L, "Oleo", BigDecimal.TEN, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ServiceLine.create(5L, null, "Oleo", BigDecimal.TEN, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ServiceLine.create(5L, 3L, "Oleo", null, NOW))
                .isInstanceOf(NullPointerException.class);
    }
}
