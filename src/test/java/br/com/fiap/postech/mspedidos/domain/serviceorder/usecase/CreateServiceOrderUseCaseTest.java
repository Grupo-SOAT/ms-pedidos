package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderEvent;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.port.message.ServiceOrderEventPublisherPort;
import br.com.fiap.postech.mspedidos.port.persistence.OwnerExistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.VehicleExistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateServiceOrderUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final ServiceOrderPersistencePort persistencePort = mock(ServiceOrderPersistencePort.class);
    private final OwnerExistencePort ownerPort = mock(OwnerExistencePort.class);
    private final VehicleExistencePort vehiclePort = mock(VehicleExistencePort.class);
    private final ServiceOrderEventPublisherPort publisher = mock(ServiceOrderEventPublisherPort.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final CreateServiceOrderUseCase useCase =
            new CreateServiceOrderUseCase(persistencePort, ownerPort, vehiclePort, publisher, clock);

    @BeforeEach
    void setUp() {
        when(persistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void should_create_order_and_publish_created_event() {
        when(ownerPort.existsById(1L)).thenReturn(true);
        when(vehiclePort.vehicleExistsById(2L)).thenReturn(true);

        ServiceOrder created = useCase.create(1L, 2L, 3L, "Revisao", "corr-1");

        assertThat(created)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW));

        ArgumentCaptor<ServiceOrderEvent> captor = ArgumentCaptor.forClass(ServiceOrderEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(CreateServiceOrderUseCase.CREATED_EVENT);
        assertThat(captor.getValue().branchId()).isEqualTo(3L);
        assertThat(captor.getValue().correlationId()).isEqualTo("corr-1");
        assertThat(captor.getValue().eventId()).isNotBlank();
    }

    @Test
    void should_reject_missing_branch() {
        assertThatThrownBy(() -> useCase.create(1L, 2L, null, "Revisao", "corr"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("branchId");
        verify(persistencePort, never()).save(any());
    }

    @Test
    void should_reject_unknown_owner() {
        when(ownerPort.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.create(1L, 2L, 3L, "Revisao", "corr"))
                .isInstanceOf(OwnerNotFoundException.class);
    }

    @Test
    void should_reject_unknown_vehicle() {
        when(ownerPort.existsById(1L)).thenReturn(true);
        when(vehiclePort.vehicleExistsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.create(1L, 2L, 3L, "Revisao", "corr"))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void should_persist_before_publishing() {
        when(ownerPort.existsById(1L)).thenReturn(true);
        when(vehiclePort.vehicleExistsById(2L)).thenReturn(true);
        when(persistencePort.save(any())).thenAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            order.assignId(99L);
            return order;
        });

        useCase.create(1L, 2L, 3L, "Revisao", "corr");

        ArgumentCaptor<ServiceOrderEvent> captor = ArgumentCaptor.forClass(ServiceOrderEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().serviceOrderId()).isEqualTo(99L);
        assertThat(captor.getValue().status()).isEqualTo(ServiceOrderStatus.PENDING);
    }
}
