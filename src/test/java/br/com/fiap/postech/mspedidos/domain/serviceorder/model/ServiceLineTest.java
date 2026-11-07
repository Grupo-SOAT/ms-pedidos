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

    @Test
    void should_follow_service_lifecycle() {
        ServiceLine line = ServiceLine.create(5L, 3L, "Oleo", BigDecimal.TEN, NOW);

        line.approve(NOW);
        assertThat(line.getStatus()).isEqualTo("APPROVED");
        assertThat(line.getStatusLabel()).isEqualTo("Aprovada");
        line.start(NOW);
        assertThat(line.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(line.getStartedAt()).isEqualTo(NOW);
        line.complete(NOW);
        assertThat(line.getStatus()).isEqualTo("COMPLETED");
        assertThat(line.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void should_cancel_service_from_approved() {
        ServiceLine line = ServiceLine.create(5L, 3L, "Oleo", BigDecimal.TEN, NOW);
        line.approve(NOW);

        line.cancel(NOW);

        assertThat(line.getStatus()).isEqualTo("CANCELLED");
        assertThat(line.getStatusLabel()).isEqualTo("Cancelada");
    }

    @Test
    void should_reject_invalid_transition() {
        ServiceLine line = ServiceLine.create(5L, 3L, "Oleo", BigDecimal.TEN, NOW);

        assertThatThrownBy(() -> line.complete(NOW))
                .isInstanceOf(InvalidServiceLineException.class)
                .hasMessage("SERVICE_STATUS_CHANGE_NOT_ALLOWED");
    }

    @Test
    void should_ignore_same_or_null_status() {
        ServiceLine line = ServiceLine.create(5L, 3L, "Oleo", BigDecimal.TEN, NOW);

        line.moveTo("AWAITING_APPROVAL", NOW);
        line.moveTo(null, NOW);

        assertThat(line.getStatus()).isEqualTo("AWAITING_APPROVAL");
    }

    @Test
    void should_update_details_and_reject_negative_price() {
        ServiceLine line = ServiceLine.create(5L, 3L, "Oleo", BigDecimal.TEN, NOW);

        line.updateDetails(4L, "Freio", BigDecimal.ONE);
        assertThat(line.getCatalogServiceId()).isEqualTo(4L);
        assertThat(line.getCatalogServiceName()).isEqualTo("Freio");
        assertThat(line.getPrice()).isEqualByComparingTo(BigDecimal.ONE);

        assertThatThrownBy(() -> line.updateDetails(null, null, BigDecimal.valueOf(-1)))
                .isInstanceOf(InvalidServiceLineException.class);
    }

    @Test
    void should_expose_labels_for_all_statuses() {
        assertThat(ServiceLine.restore(1L, 5L, 3L, "Oleo", BigDecimal.ONE, "PENDING",
                NOW, null, null, null).getStatusLabel()).isEqualTo("Pendente");
        assertThat(ServiceLine.restore(1L, 5L, 3L, "Oleo", BigDecimal.ONE, "IN_PROGRESS",
                NOW, null, null, null).getStatusLabel()).isEqualTo("Em Andamento");
        assertThat(ServiceLine.restore(1L, 5L, 3L, "Oleo", BigDecimal.ONE, "COMPLETED",
                NOW, null, null, null).getStatusLabel()).isEqualTo("Finalizada");
        assertThat(ServiceLine.restore(1L, 5L, 3L, "Oleo", BigDecimal.ONE, "UNKNOWN",
                NOW, null, null, null).getStatusLabel()).isEqualTo("UNKNOWN");
    }

    @Test
    void should_reject_negative_price_on_create() {
        assertThatThrownBy(() -> ServiceLine.create(5L, 3L, "Oleo", BigDecimal.valueOf(-1), NOW))
                .isInstanceOf(InvalidServiceLineException.class)
                .hasMessage("INVALID_PRICE");
    }

    @Test
    void should_cover_in_progress_transitions_and_timestamps() {
        ServiceLine line = ServiceLine.create(5L, 3L, "Oleo", BigDecimal.TEN, NOW);
        line.approve(LATER);
        assertThat(line.getApprovedAt()).isEqualTo(LATER);

        ServiceLine completing = ServiceLine.restore(1L, 5L, 3L, "Oleo", BigDecimal.ONE,
                "IN_PROGRESS", NOW, NOW, NOW, null);
        completing.complete(LATER);
        assertThat(completing.getCompletedAt()).isEqualTo(LATER);

        ServiceLine cancelling = ServiceLine.restore(2L, 5L, 3L, "Oleo", BigDecimal.ONE,
                "IN_PROGRESS", NOW, NOW, NOW, null);
        cancelling.cancel(LATER);
        assertThat(cancelling.getStatus()).isEqualTo("CANCELLED");

        ServiceLine approvedCancel = ServiceLine.restore(3L, 5L, 3L, "Oleo", BigDecimal.ONE,
                "APPROVED", NOW, NOW, null, null);
        approvedCancel.cancel(LATER);
        assertThat(approvedCancel.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void should_reject_transition_from_terminal_or_unknown_status() {
        ServiceLine delivered = ServiceLine.restore(1L, 5L, 3L, "Oleo", BigDecimal.ONE,
                "DELIVERED", NOW, null, null, null);

        assertThatThrownBy(() -> delivered.moveTo("IN_PROGRESS", NOW))
                .isInstanceOf(InvalidServiceLineException.class);
    }

    @Test
    void should_accept_zero_price_on_update() {
        ServiceLine line = ServiceLine.create(5L, 3L, "Oleo", BigDecimal.TEN, NOW);

        line.updateDetails(null, null, BigDecimal.ZERO);

        assertThat(line.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
