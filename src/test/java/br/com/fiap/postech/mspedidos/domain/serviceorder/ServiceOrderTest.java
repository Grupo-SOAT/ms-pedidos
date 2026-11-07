package br.com.fiap.postech.mspedidos.domain.serviceorder;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceOrderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-01T11:00:00Z");

    @Test
    void should_create_pending_order() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);

        assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.PENDING);
        assertThat(order.getClientId()).isEqualTo(1L);
        assertThat(order.getVehicleId()).isEqualTo(2L);
        assertThat(order.getBranchId()).isEqualTo(3L);
        assertThat(order.getDescription()).isEqualTo("Revisao");
        assertThat(order.getCreatedAt()).isEqualTo(NOW);
        assertThat(order.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void should_reject_missing_required_fields() {
        assertThatThrownBy(() -> ServiceOrder.create(null, 2L, 3L, "x", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ServiceOrder.create(1L, null, 3L, "x", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ServiceOrder.create(1L, 2L, null, "x", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ServiceOrder.create(1L, 2L, 3L, null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_walk_the_happy_path_with_timestamps() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);

        order.transitionTo(ServiceOrderStatus.IN_INSPECTION, LATER);
        order.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, LATER);
        order.transitionTo(ServiceOrderStatus.APPROVED, LATER);
        order.transitionTo(ServiceOrderStatus.IN_PROGRESS, LATER);
        order.transitionTo(ServiceOrderStatus.COMPLETED, LATER);
        order.transitionTo(ServiceOrderStatus.DELIVERED, LATER);

        assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.DELIVERED);
        assertThat(order.getInspectedAt()).isEqualTo(LATER);
        assertThat(order.getApprovedAt()).isEqualTo(LATER);
        assertThat(order.getStartedAt()).isEqualTo(LATER);
        assertThat(order.getCompletedAt()).isEqualTo(LATER);
        assertThat(order.getDeliveredAt()).isEqualTo(LATER);
        assertThat(order.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void should_allow_cancellation_and_partial_rejection_paths() {
        ServiceOrder cancelled = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);
        cancelled.transitionTo(ServiceOrderStatus.CANCELLED, LATER);
        assertThat(cancelled.getCancelledAt()).isEqualTo(LATER);

        ServiceOrder partial = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);
        partial.transitionTo(ServiceOrderStatus.IN_INSPECTION, LATER);
        partial.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, LATER);
        partial.transitionTo(ServiceOrderStatus.PARTIALLY_REJECTED, LATER);
        assertThat(partial.getPartiallyRejectedAt()).isEqualTo(LATER);
        partial.transitionTo(ServiceOrderStatus.IN_INSPECTION, LATER);
        assertThat(partial.getStatus()).isEqualTo(ServiceOrderStatus.IN_INSPECTION);
    }

    @Test
    void should_reject_invalid_transition() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);

        assertThatThrownBy(() -> order.transitionTo(ServiceOrderStatus.APPROVED, LATER))
                .isInstanceOfSatisfying(InvalidStatusTransitionException.class, exception -> {
                    assertThat(exception.getMessage()).isEqualTo("Status change not allowed: PENDING -> APPROVED");
                    assertThat(exception.getSource()).isEqualTo(ServiceOrderStatus.PENDING);
                    assertThat(exception.getTarget()).isEqualTo(ServiceOrderStatus.APPROVED);
                });
    }

    @Test
    void should_ignore_transition_to_same_status() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);

        order.transitionTo(ServiceOrderStatus.PENDING, LATER);

        assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.PENDING);
        assertThat(order.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void should_reject_null_target() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);

        assertThatThrownBy(() -> order.transitionTo(null, LATER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_apply_estimated_amount() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);

        order.applyEstimatedAmount(BigDecimal.TEN, LATER);

        assertThat(order.getEstimatedAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(order.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void should_restore_all_fields() {
        ServiceOrder order = ServiceOrder.restore(9L, 1L, 2L, 3L, "Revisao",
                ServiceOrderStatus.APPROVED, BigDecimal.ONE, NOW, LATER, LATER, LATER,
                LATER, null, null, null, null, null);

        assertThat(order.getId()).isEqualTo(9L);
        assertThat(order.getStatus()).isEqualTo(ServiceOrderStatus.APPROVED);
        assertThat(order.getInspectedAt()).isEqualTo(LATER);
        assertThat(order.getApprovedAt()).isEqualTo(LATER);
        assertThat(order.getRejectedAt()).isEqualTo(LATER);
    }

    @Test
    void should_expose_status_labels() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);
        assertThat(order.getStatusLabel()).isEqualTo("Recebida");
        order.transitionTo(ServiceOrderStatus.IN_INSPECTION, LATER);
        assertThat(order.getStatusLabel()).isEqualTo("Em Diagnóstico");
        order.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, LATER);
        assertThat(order.getStatusLabel()).isEqualTo("Aguardando Aprovação");
        order.transitionTo(ServiceOrderStatus.APPROVED, LATER);
        assertThat(order.getStatusLabel()).isEqualTo("Aprovada");
    }

    @Test
    void should_update_details_partially() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);

        order.updateDetails(9L, null, "Nova", LATER);
        assertThat(order.getClientId()).isEqualTo(9L);
        assertThat(order.getVehicleId()).isEqualTo(2L);
        assertThat(order.getDescription()).isEqualTo("Nova");

        order.updateDetails(null, 8L, "   ", LATER);
        assertThat(order.getVehicleId()).isEqualTo(8L);
        assertThat(order.getDescription()).isEqualTo("Nova");
    }
}
