package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.InvalidStatusTransitionException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.port.persistence.OwnerExistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.VehicleExistencePort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceOrderQueryUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final ServiceOrderPersistencePort persistencePort = mock(ServiceOrderPersistencePort.class);
    private final OwnerExistencePort ownerPort = mock(OwnerExistencePort.class);
    private final VehicleExistencePort vehiclePort = mock(VehicleExistencePort.class);
    private final ServiceOrderQueryUseCase useCase = new ServiceOrderQueryUseCase(
            persistencePort, ownerPort, vehiclePort, Clock.fixed(NOW, ZoneOffset.UTC));

    private ServiceOrder order(long id, ServiceOrderStatus status) {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "OS " + id, NOW);
        order.assignId(id);
        order.transitionTo(status == ServiceOrderStatus.PENDING ? ServiceOrderStatus.IN_INSPECTION : status, NOW);
        return order;
    }

    @Test
    void should_list_with_default_filters_and_status_priority() {
        ServiceOrder pending = ServiceOrder.create(1L, 2L, 3L, "pending", NOW);
        pending.assignId(1L);
        ServiceOrder inProgress = ServiceOrder.create(1L, 2L, 3L, "progress", NOW);
        inProgress.assignId(2L);
        inProgress.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
        inProgress.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, NOW);
        inProgress.transitionTo(ServiceOrderStatus.APPROVED, NOW);
        inProgress.transitionTo(ServiceOrderStatus.IN_PROGRESS, NOW);
        ServiceOrder completed = ServiceOrder.create(1L, 2L, 3L, "done", NOW);
        completed.assignId(3L);
        completed.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
        completed.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, NOW);
        completed.transitionTo(ServiceOrderStatus.APPROVED, NOW);
        completed.transitionTo(ServiceOrderStatus.IN_PROGRESS, NOW);
        completed.transitionTo(ServiceOrderStatus.COMPLETED, NOW);
        when(persistencePort.findAll()).thenReturn(List.of(completed, inProgress, pending));

        var page = useCase.list(null, null, null, 10, null);

        assertThat(page.data()).extracting(ServiceOrder::getId).containsExactly(1L, 2L);
        assertThat(page.isLast()).isTrue();
        assertThat(page.cursor()).isNull();
    }

    @Test
    void should_filter_by_status_client_vehicle_and_cursor() {
        ServiceOrder first = ServiceOrder.create(1L, 2L, 3L, "first", NOW);
        first.assignId(1L);
        ServiceOrder second = ServiceOrder.create(1L, 2L, 3L, "second", NOW);
        second.assignId(2L);
        ServiceOrder otherClient = ServiceOrder.create(9L, 8L, 3L, "other", NOW);
        otherClient.assignId(3L);
        when(persistencePort.findAll()).thenReturn(List.of(first, second, otherClient));

        var byStatus = useCase.list("PENDING", 1L, 2L, 10, null);
        var afterCursor = useCase.list("PENDING", 1L, 2L, 10, "1");

        assertThat(byStatus.data()).extracting(ServiceOrder::getId).containsExactly(1L, 2L);
        assertThat(afterCursor.data()).extracting(ServiceOrder::getId).containsExactly(2L);
    }

    @Test
    void should_compute_next_cursor_when_page_is_full() {
        ServiceOrder first = ServiceOrder.create(1L, 2L, 3L, "first", NOW);
        first.assignId(1L);
        ServiceOrder second = ServiceOrder.create(1L, 2L, 3L, "second", NOW);
        second.assignId(2L);
        when(persistencePort.findAll()).thenReturn(List.of(first, second));

        var page = useCase.list("PENDING", 1L, 2L, 1, null);

        assertThat(page.data()).hasSize(1);
        assertThat(page.isLast()).isFalse();
        assertThat(page.cursor()).isEqualTo("1");
    }

    @Test
    void should_ignore_blank_and_invalid_filters() {
        when(persistencePort.findAll()).thenReturn(List.of());
        var page = useCase.list(" ", null, null, 0, "not-a-number");
        assertThat(page.data()).isEmpty();
        assertThat(page.pageSize()).isEqualTo(10);
    }

    @Test
    void should_get_by_id() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "OS", NOW);
        order.assignId(5L);
        when(persistencePort.findById(5L)).thenReturn(Optional.of(order));
        assertThat(useCase.getById(5L)).isSameAs(order);
    }

    @Test
    void should_reject_missing_order() {
        when(persistencePort.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.getById(9L))
                .isInstanceOf(ServiceOrderNotFoundException.class);
    }

    @Test
    void should_update_order_fields_and_status() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "old", NOW);
        order.assignId(5L);
        order.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
        order.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, NOW);
        when(persistencePort.findById(5L)).thenReturn(Optional.of(order));
        when(ownerPort.existsById(7L)).thenReturn(true);
        when(vehiclePort.vehicleExistsById(8L)).thenReturn(true);
        when(persistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrder updated = useCase.update(5L, 7L, 8L, "new", "APPROVED", "corr");

        assertThat(updated.getClientId()).isEqualTo(7L);
        assertThat(updated.getVehicleId()).isEqualTo(8L);
        assertThat(updated.getDescription()).isEqualTo("new");
        assertThat(updated.getStatus()).isEqualTo(ServiceOrderStatus.APPROVED);
    }

    @Test
    void should_reject_unknown_owner_and_vehicle_on_update() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "old", NOW);
        order.assignId(5L);
        when(persistencePort.findById(5L)).thenReturn(Optional.of(order));
        when(ownerPort.existsById(9L)).thenReturn(false);
        when(vehiclePort.vehicleExistsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.update(5L, 9L, null, null, null, "corr"))
                .isInstanceOf(OwnerNotFoundException.class);
        assertThatThrownBy(() -> useCase.update(5L, null, 9L, null, null, "corr"))
                .isInstanceOf(VehicleNotFoundException.class);
        verify(persistencePort, never()).save(any());
    }

    @Test
    void should_reject_invalid_status_transition_on_update() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "old", NOW);
        order.assignId(5L);
        when(persistencePort.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.update(5L, null, null, null, "APPROVED", "corr"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void should_delete_existing_order() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "OS", NOW);
        order.assignId(5L);
        when(persistencePort.findById(5L)).thenReturn(Optional.of(order));

        useCase.delete(5L);

        verify(persistencePort).deleteById(5L);
    }

    @Test
    void should_reject_delete_of_missing_order() {
        when(persistencePort.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.delete(9L))
                .isInstanceOf(ServiceOrderNotFoundException.class);
        verify(persistencePort, never()).deleteById(any());
    }

    @Test
    void should_isolate_each_filter() {
        ServiceOrder first = ServiceOrder.create(1L, 2L, 3L, "first", NOW);
        first.assignId(1L);
        ServiceOrder second = ServiceOrder.create(9L, 8L, 3L, "second", NOW);
        second.assignId(2L);
        ServiceOrder completed = ServiceOrder.create(1L, 2L, 3L, "done", NOW);
        completed.assignId(3L);
        completed.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
        completed.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, NOW);
        completed.transitionTo(ServiceOrderStatus.APPROVED, NOW);
        completed.transitionTo(ServiceOrderStatus.IN_PROGRESS, NOW);
        completed.transitionTo(ServiceOrderStatus.COMPLETED, NOW);
        when(persistencePort.findAll()).thenReturn(List.of(first, second, completed));

        assertThat(useCase.list(null, 9L, null, 10, null).data())
                .extracting(ServiceOrder::getId).containsExactly(2L);
        assertThat(useCase.list(null, null, 8L, 10, null).data())
                .extracting(ServiceOrder::getId).containsExactly(2L);
        assertThat(useCase.list("COMPLETED", null, null, 10, null).data())
                .extracting(ServiceOrder::getId).containsExactly(3L);
    }

    @Test
    void should_order_by_status_priority_not_only_by_id() {
        ServiceOrder inProgress = ServiceOrder.create(1L, 2L, 3L, "progress", NOW);
        inProgress.assignId(1L);
        inProgress.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
        inProgress.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, NOW);
        inProgress.transitionTo(ServiceOrderStatus.APPROVED, NOW);
        inProgress.transitionTo(ServiceOrderStatus.IN_PROGRESS, NOW);
        ServiceOrder pending = ServiceOrder.create(1L, 2L, 3L, "pending", NOW);
        pending.assignId(2L);
        when(persistencePort.findAll()).thenReturn(List.of(inProgress, pending));

        assertThat(useCase.list(null, null, null, 10, null).data())
                .extracting(ServiceOrder::getId).containsExactly(2L, 1L);
    }

    @Test
    void should_mark_last_true_when_page_exactly_fits_and_skip_unsaved_orders() {
        ServiceOrder first = ServiceOrder.create(1L, 2L, 3L, "first", NOW);
        first.assignId(1L);
        ServiceOrder second = ServiceOrder.create(1L, 2L, 3L, "second", NOW);
        second.assignId(2L);
        ServiceOrder unsaved = ServiceOrder.create(1L, 2L, 3L, "unsaved", NOW);
        when(persistencePort.findAll()).thenReturn(List.of(first, second, unsaved));

        var page = useCase.list(null, null, null, 2, null);

        assertThat(page.data()).extracting(ServiceOrder::getId).containsExactly(1L, 2L);
        assertThat(page.isLast()).isTrue();
    }
}
