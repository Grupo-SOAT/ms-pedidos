package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;
import br.com.fiap.postech.mspedidos.port.ServiceLinePersistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

class RegisterProgressUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final ServiceOrderPersistencePort orderPort = mock(ServiceOrderPersistencePort.class);
    private final ServiceLinePersistencePort linePort = mock(ServiceLinePersistencePort.class);
    private final ChangeServiceOrderStatusUseCase changeStatus = mock(ChangeServiceOrderStatusUseCase.class);
    private final RegisterProgressUseCase useCase = new RegisterProgressUseCase(
            orderPort, linePort, changeStatus, Clock.fixed(NOW, ZoneOffset.UTC));

    private ServiceOrder order(ServiceOrderStatus status) {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "OS", NOW);
        order.assignId(5L);
        if (status == ServiceOrderStatus.IN_INSPECTION) {
            order.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
        } else if (status == ServiceOrderStatus.AWAITING_APPROVAL) {
            order.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
            order.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, NOW);
        } else if (status == ServiceOrderStatus.APPROVED) {
            order.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
            order.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, NOW);
            order.transitionTo(ServiceOrderStatus.APPROVED, NOW);
        } else if (status == ServiceOrderStatus.IN_PROGRESS) {
            order.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
            order.transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, NOW);
            order.transitionTo(ServiceOrderStatus.APPROVED, NOW);
            order.transitionTo(ServiceOrderStatus.IN_PROGRESS, NOW);
        }
        return order;
    }

    private ServiceLine line(String status) {
        return ServiceLine.restore(10L, 5L, 2L, "Oleo", BigDecimal.TEN, status, NOW, null, null, null);
    }

    @Test
    void should_delegate_status_actions_to_the_status_use_case() {
        ServiceOrder order = order(ServiceOrderStatus.PENDING);
        when(orderPort.findById(5L)).thenReturn(Optional.of(order));

        useCase.register(5L, "START_INSPECTION", null, "corr");
        useCase.register(5L, "COMPLETE_INSPECTION", null, "corr");
        useCase.register(5L, "DELIVER_VEHICLE", null, "corr");

        verify(changeStatus).changeStatus(5L, ServiceOrderStatus.IN_INSPECTION, "corr");
        verify(changeStatus).changeStatus(5L, ServiceOrderStatus.AWAITING_APPROVAL, "corr");
        verify(changeStatus).changeStatus(5L, ServiceOrderStatus.DELIVERED, "corr");
    }

    @Test
    void should_start_service_and_move_approved_order_to_in_progress() {
        ServiceOrder order = order(ServiceOrderStatus.APPROVED);
        ServiceLine line = line("AWAITING_APPROVAL");
        when(orderPort.findById(5L)).thenReturn(Optional.of(order));
        when(linePort.findByIdAndServiceOrderId(10L, 5L)).thenReturn(Optional.of(line));
        when(linePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.register(5L, "START_SERVICE", 10L, "corr");

        assertThat(line.getStatus()).isEqualTo("IN_PROGRESS");
        verify(linePort).save(line);
        verify(changeStatus).changeStatus(5L, ServiceOrderStatus.IN_PROGRESS, "corr");
    }

    @Test
    void should_start_service_without_moving_order_when_already_in_progress() {
        ServiceOrder order = order(ServiceOrderStatus.IN_PROGRESS);
        ServiceLine line = line("APPROVED");
        when(orderPort.findById(5L)).thenReturn(Optional.of(order));
        when(linePort.findByIdAndServiceOrderId(10L, 5L)).thenReturn(Optional.of(line));
        when(linePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.register(5L, "START_SERVICE", 10L, "corr");

        verify(changeStatus, never()).changeStatus(any(), any(), any());
    }

    @Test
    void should_complete_service_and_complete_order_when_all_lines_are_done() {
        ServiceOrder order = order(ServiceOrderStatus.IN_PROGRESS);
        ServiceLine line = line("IN_PROGRESS");
        when(orderPort.findById(5L)).thenReturn(Optional.of(order));
        when(linePort.findByIdAndServiceOrderId(10L, 5L)).thenReturn(Optional.of(line));
        when(linePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(linePort.findByServiceOrderId(5L)).thenReturn(List.of(line));

        useCase.register(5L, "COMPLETE_SERVICE", 10L, "corr");

        verify(linePort).save(line);
        verify(changeStatus).changeStatus(5L, ServiceOrderStatus.COMPLETED, "corr");
    }

    @Test
    void should_not_complete_order_when_another_line_is_pending() {
        ServiceOrder order = order(ServiceOrderStatus.IN_PROGRESS);
        ServiceLine line = line("IN_PROGRESS");
        ServiceLine pending = ServiceLine.restore(11L, 5L, 2L, "Freio", BigDecimal.ONE,
                "AWAITING_APPROVAL", NOW, null, null, null);
        when(orderPort.findById(5L)).thenReturn(Optional.of(order));
        when(linePort.findByIdAndServiceOrderId(10L, 5L)).thenReturn(Optional.of(line));
        when(linePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(linePort.findByServiceOrderId(5L)).thenReturn(List.of(line, pending));

        useCase.register(5L, "COMPLETE_SERVICE", 10L, "corr");

        verify(changeStatus, never()).changeStatus(any(), any(), any());
    }

    @Test
    void should_cancel_service() {
        ServiceOrder order = order(ServiceOrderStatus.AWAITING_APPROVAL);
        ServiceLine line = line("AWAITING_APPROVAL");
        when(orderPort.findById(5L)).thenReturn(Optional.of(order));
        when(linePort.findByIdAndServiceOrderId(10L, 5L)).thenReturn(Optional.of(line));
        when(linePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.register(5L, "CANCEL_SERVICE", 10L, "corr");

        assertThat(line.getStatus()).isEqualTo("CANCELLED");
        verify(linePort).save(line);
    }

    @Test
    void should_reject_unknown_action_and_missing_inputs() {
        ServiceOrder order = order(ServiceOrderStatus.PENDING);
        when(orderPort.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.register(5L, "NOPE", null, "corr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("UNSUPPORTED_PROGRESS_ACTION");
        assertThatThrownBy(() -> useCase.register(5L, "START_SERVICE", null, "corr"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("RELATED_SERVICE_ID_REQUIRED");
        assertThatThrownBy(() -> useCase.register(5L, "START_SERVICE", 99L, "corr"))
                .isInstanceOf(ServiceLineNotFoundException.class);
    }

    @Test
    void should_reject_missing_order() {
        when(orderPort.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.register(9L, "START_INSPECTION", null, "corr"))
                .isInstanceOf(ServiceOrderNotFoundException.class);
    }

    @Test
    void should_reject_missing_service_when_completing_or_cancelling() {
        ServiceOrder order = order(ServiceOrderStatus.IN_PROGRESS);
        when(orderPort.findById(5L)).thenReturn(Optional.of(order));
        when(linePort.findByIdAndServiceOrderId(99L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.register(5L, "COMPLETE_SERVICE", 99L, "corr"))
                .isInstanceOf(ServiceLineNotFoundException.class);
        assertThatThrownBy(() -> useCase.register(5L, "CANCEL_SERVICE", 99L, "corr"))
                .isInstanceOf(ServiceLineNotFoundException.class);
    }
}
