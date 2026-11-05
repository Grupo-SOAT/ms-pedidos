package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;
import br.com.fiap.postech.mspedidos.port.CatalogQueryPort;
import br.com.fiap.postech.mspedidos.port.RegistrationCommandPort;
import br.com.fiap.postech.mspedidos.port.ServiceLinePersistencePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateServiceOrderCascadeUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final CreateServiceOrderUseCase createOrder = mock(CreateServiceOrderUseCase.class);
    private final RegistrationCommandPort registration = mock(RegistrationCommandPort.class);
    private final CatalogQueryPort catalog = mock(CatalogQueryPort.class);
    private final ServiceLinePersistencePort linePort = mock(ServiceLinePersistencePort.class);
    private final CreateServiceOrderCascadeUseCase useCase = new CreateServiceOrderCascadeUseCase(
            createOrder, registration, catalog, linePort, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void should_create_owner_vehicle_order_and_service_lines() {
        when(registration.createOwner(any())).thenReturn(11L);
        when(registration.createVehicle(any())).thenReturn(22L);
        ServiceOrder order = ServiceOrder.create(11L, 22L, 3L, "Revisao", NOW);
        order.assignId(5L);
        when(createOrder.create(11L, 22L, 3L, "Revisao", "corr")).thenReturn(order);
        when(catalog.findById(7L)).thenReturn(new CatalogQueryPort.CatalogServiceInfo(7L, "Oleo", BigDecimal.TEN));
        when(linePort.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(new CreateServiceOrderCascadeUseCase.ServiceOrderCascadeCommand(
                null, null, 3L, "Revisao", "corr",
                new RegistrationCommandPort.OwnerData("Joao", "31058167049", "CPF", "1199", "joao@email.com"),
                new RegistrationCommandPort.VehicleData(null, "ABC1D23", "Fiat", "Uno", 2020, "Branco"),
                List.of(7L)));

        assertThat(result.order()).isSameAs(order);
        assertThat(result.services()).hasSize(1);
        assertThat(result.services().get(0).getCatalogServiceName()).isEqualTo("Oleo");
        verify(registration).createOwner(any());
        verify(registration).createVehicle(any());
        verify(createOrder).create(11L, 22L, 3L, "Revisao", "corr");
    }

    @Test
    void should_use_existing_owner_and_vehicle_ids() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);
        order.assignId(5L);
        when(createOrder.create(1L, 2L, 3L, "Revisao", "corr")).thenReturn(order);
        when(linePort.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(new CreateServiceOrderCascadeUseCase.ServiceOrderCascadeCommand(
                1L, 2L, 3L, "Revisao", "corr", null, null, List.of()));

        assertThat(result.services()).isEmpty();
        verify(registration, never()).createOwner(any());
        verify(registration, never()).createVehicle(any());
    }

    @Test
    void should_reject_missing_branch() {
        assertThatThrownBy(() -> useCase.execute(new CreateServiceOrderCascadeUseCase.ServiceOrderCascadeCommand(
                1L, 2L, null, "Revisao", "corr", null, null, List.of())))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_treat_null_catalog_list_as_empty() {
        ServiceOrder order = ServiceOrder.create(1L, 2L, 3L, "Revisao", NOW);
        order.assignId(5L);
        when(createOrder.create(1L, 2L, 3L, "Revisao", "corr")).thenReturn(order);
        when(linePort.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var command = new CreateServiceOrderCascadeUseCase.ServiceOrderCascadeCommand(
                1L, 2L, 3L, "Revisao", "corr", null, null, null);

        assertThat(command.catalogServiceIds()).isEmpty();
        assertThat(useCase.execute(command).services()).isEmpty();
    }
}
