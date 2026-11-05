package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderEvent;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.port.message.ServiceOrderEventPublisherPort;
import br.com.fiap.postech.mspedidos.port.monitoring.ServiceOrderMetricsPort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangeServiceOrderStatusUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final ServiceOrderPersistencePort persistencePort = mock(ServiceOrderPersistencePort.class);
    private final ServiceOrderEventPublisherPort publisher = mock(ServiceOrderEventPublisherPort.class);
    private final ServiceOrderMetricsPort metrics = mock(ServiceOrderMetricsPort.class);
    private final br.com.fiap.postech.mspedidos.port.ServiceLineQueryPort lineQueryPort =
            mock(br.com.fiap.postech.mspedidos.port.ServiceLineQueryPort.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ChangeServiceOrderStatusUseCase useCase =
            new ChangeServiceOrderStatusUseCase(persistencePort, publisher, metrics, lineQueryPort, clock);

    @BeforeEach
    void setUp() {
        when(persistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void should_change_status_and_publish_event() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);
        order.assignId(5L);
        when(persistencePort.findById(5L)).thenReturn(Optional.of(order));

        ServiceOrder updated = useCase.changeStatus(5L, ServiceOrderStatus.IN_INSPECTION, "corr-1");

        assertThat(updated.getStatus()).isEqualTo(ServiceOrderStatus.IN_INSPECTION);
        assertThat(updated.getInspectedAt()).isEqualTo(NOW);
        verify(metrics).recordStatusTransition(ServiceOrderStatus.PENDING, ServiceOrderStatus.IN_INSPECTION);

        ArgumentCaptor<ServiceOrderEvent> captor = ArgumentCaptor.forClass(ServiceOrderEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(ChangeServiceOrderStatusUseCase.STATUS_CHANGED_EVENT);
        assertThat(captor.getValue().correlationId()).isEqualTo("corr-1");
    }

    @Test
    void should_reject_unknown_service_order() {
        when(persistencePort.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.changeStatus(9L, ServiceOrderStatus.IN_INSPECTION, "corr"))
                .isInstanceOf(ServiceOrderNotFoundException.class);
    }

    @Test
    void should_reject_invalid_transition_without_saving() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);
        order.assignId(5L);
        when(persistencePort.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.changeStatus(5L, ServiceOrderStatus.APPROVED, "corr"))
                .isInstanceOf(br.com.fiap.postech.mspedidos.domain.serviceorder.InvalidStatusTransitionException.class);
        verify(persistencePort, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void should_publish_budget_requested_when_inspection_is_completed() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);
        order.assignId(5L);
        order.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
        when(persistencePort.findById(5L)).thenReturn(Optional.of(order));
        when(lineQueryPort.findByServiceOrderId(5L)).thenReturn(java.util.List.of(
                new br.com.fiap.postech.mspedidos.domain.reporting.ServiceLineSnapshot(
                        7L, "Oleo", NOW, null, null, null)));

        useCase.changeStatus(5L, ServiceOrderStatus.AWAITING_APPROVAL, "corr");

        org.mockito.ArgumentCaptor<br.com.fiap.postech.mspedidos.domain.serviceorder.BudgetRequestedEvent> captor =
                org.mockito.ArgumentCaptor.forClass(
                        br.com.fiap.postech.mspedidos.domain.serviceorder.BudgetRequestedEvent.class);
        verify(publisher).publishBudgetRequested(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().payload().items())
                .extracting(br.com.fiap.postech.mspedidos.domain.serviceorder.BudgetRequestedEvent.Item::catalogServiceId)
                .containsExactly(7L);
    }

    @Test
    void should_apply_budget_and_move_to_awaiting_approval() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);
        order.assignId(5L);
        order.transitionTo(ServiceOrderStatus.IN_INSPECTION, NOW);
        when(persistencePort.findById(5L)).thenReturn(Optional.of(order));

        ServiceOrder updated = useCase.applyBudget(5L, BigDecimal.valueOf(250), "corr-2");

        assertThat(updated.getStatus()).isEqualTo(ServiceOrderStatus.AWAITING_APPROVAL);
        assertThat(updated.getEstimatedAmount()).isEqualByComparingTo(BigDecimal.valueOf(250));

        verify(publisher, org.mockito.Mockito.never()).publishBudgetRequested(any());
    }

    @Test
    void should_reject_budget_for_unknown_service_order() {
        when(persistencePort.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.applyBudget(9L, BigDecimal.TEN, "corr"))
                .isInstanceOf(ServiceOrderNotFoundException.class);
    }
}
