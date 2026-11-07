package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

import br.com.fiap.postech.mspedidos.domain.serviceorder.model.InvalidServiceLineException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine;
import br.com.fiap.postech.mspedidos.port.CatalogQueryPort;
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

class ServiceLineUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final ServiceOrderPersistencePort orderPort = mock(ServiceOrderPersistencePort.class);
    private final ServiceLinePersistencePort linePort = mock(ServiceLinePersistencePort.class);
    private final CatalogQueryPort catalogPort = mock(CatalogQueryPort.class);
    private final ServiceLineUseCase useCase = new ServiceLineUseCase(
            orderPort, linePort, catalogPort, Clock.fixed(NOW, ZoneOffset.UTC));

    private ServiceLine line(long id, String status) {
        return ServiceLine.restore(id, 1L, 2L, "Oleo", BigDecimal.TEN, status, NOW, null, null, null);
    }

    @Test
    void should_create_line_with_informed_price() {
        when(orderPort.existsById(1L)).thenReturn(true);
        when(catalogPort.findById(2L))
                .thenReturn(new CatalogQueryPort.CatalogServiceInfo(2L, "Oleo", BigDecimal.ONE));
        when(linePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceLine created = useCase.create(1L, 2L, BigDecimal.TEN);

        assertThat(created.getPrice()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(created.getStatus()).isEqualTo("AWAITING_APPROVAL");
    }

    @Test
    void should_create_line_with_catalog_price_when_price_is_null() {
        when(orderPort.existsById(1L)).thenReturn(true);
        when(catalogPort.findById(2L))
                .thenReturn(new CatalogQueryPort.CatalogServiceInfo(2L, "Oleo", BigDecimal.ONE));
        when(linePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(useCase.create(1L, 2L, null).getPrice()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void should_reject_create_when_order_or_price_is_invalid() {
        when(orderPort.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> useCase.create(9L, 1L, BigDecimal.ONE))
                .isInstanceOf(ServiceOrderNotFoundException.class);

        when(orderPort.existsById(1L)).thenReturn(true);
        when(catalogPort.findById(2L))
                .thenReturn(new CatalogQueryPort.CatalogServiceInfo(2L, "Oleo", BigDecimal.ONE));
        assertThatThrownBy(() -> useCase.create(1L, 2L, BigDecimal.valueOf(-1)))
                .isInstanceOf(InvalidServiceLineException.class)
                .hasMessage("INVALID_PRICE");
    }

    @Test
    void should_update_line_details_and_status() {
        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByIdAndServiceOrderId(5L, 1L)).thenReturn(Optional.of(line(5L, "AWAITING_APPROVAL")));
        when(catalogPort.findById(3L))
                .thenReturn(new CatalogQueryPort.CatalogServiceInfo(3L, "Freio", BigDecimal.ONE));
        when(linePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceLine updated = useCase.update(1L, 5L, 3L, BigDecimal.TEN, "APPROVED");

        assertThat(updated.getCatalogServiceId()).isEqualTo(3L);
        assertThat(updated.getPrice()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(updated.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void should_update_only_price_when_catalog_is_absent() {
        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByIdAndServiceOrderId(5L, 1L)).thenReturn(Optional.of(line(5L, "AWAITING_APPROVAL")));
        when(linePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceLine updated = useCase.update(1L, 5L, null, BigDecimal.ONE, null);

        assertThat(updated.getPrice()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(updated.getCatalogServiceId()).isEqualTo(2L);
    }

    @Test
    void should_reject_update_of_missing_order_or_line() {
        when(orderPort.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> useCase.update(9L, 1L, null, null, null))
                .isInstanceOf(ServiceOrderNotFoundException.class);

        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByIdAndServiceOrderId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.update(1L, 9L, null, null, null))
                .isInstanceOf(ServiceLineNotFoundException.class);
    }

    @Test
    void should_get_line_and_reject_missing_order_or_line() {
        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByIdAndServiceOrderId(5L, 1L)).thenReturn(Optional.of(line(5L, "PENDING")));
        assertThat(useCase.get(1L, 5L).getId()).isEqualTo(5L);

        when(orderPort.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> useCase.get(9L, 5L))
                .isInstanceOf(ServiceOrderNotFoundException.class);
        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByIdAndServiceOrderId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.get(1L, 9L))
                .isInstanceOf(ServiceLineNotFoundException.class);
    }

    @Test
    void should_delete_line() {
        ServiceLine line = line(5L, "PENDING");
        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByIdAndServiceOrderId(5L, 1L)).thenReturn(Optional.of(line));

        useCase.delete(1L, 5L);

        verify(linePort).delete(line);
    }

    @Test
    void should_list_with_filters_cursor_and_pagination() {
        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByServiceOrderId(1L)).thenReturn(List.of(
                line(1L, "PENDING"), line(2L, "PENDING"), line(3L, "PENDING")));

        var page = useCase.list(1L, null, 2, null);
        var cursorPage = useCase.list(1L, 2L, 10, null);
        var afterCursor = useCase.list(1L, null, 10, "2");

        assertThat(page.data()).extracting(ServiceLine::getId).containsExactly(1L, 2L);
        assertThat(page.isLast()).isFalse();
        assertThat(page.cursor()).isEqualTo("2");
        assertThat(cursorPage.data()).extracting(ServiceLine::getId).containsExactly(2L);
        assertThat(afterCursor.data()).extracting(ServiceLine::getId).containsExactly(3L);
    }

    @Test
    void should_use_default_page_size_and_reject_missing_order_on_list() {
        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByServiceOrderId(1L)).thenReturn(List.of());
        assertThat(useCase.list(1L, null, 0, "bad").pageSize()).isEqualTo(10);

        when(orderPort.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> useCase.list(9L, null, 10, null))
                .isInstanceOf(ServiceOrderNotFoundException.class);
    }

    @Test
    void should_reject_delete_of_missing_line_without_calling_delete() {
        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByIdAndServiceOrderId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete(1L, 9L))
                .isInstanceOf(ServiceLineNotFoundException.class);
        verify(linePort, never()).delete(any());
    }

    @Test
    void should_filter_list_by_service_id_and_skip_unsaved_lines() {
        ServiceLine first = line(1L, "PENDING");
        ServiceLine second = line(2L, "PENDING");
        ServiceLine unsaved = ServiceLine.create(1L, 2L, "Oleo", BigDecimal.TEN, NOW);
        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByServiceOrderId(1L)).thenReturn(List.of(first, second, unsaved));

        assertThat(useCase.list(1L, 1L, 10, null).data())
                .extracting(ServiceLine::getId).containsExactly(1L);
        assertThat(useCase.list(1L, null, 10, null).data()).hasSize(2);
    }

    @Test
    void should_mark_last_true_when_page_exactly_fits() {
        when(orderPort.existsById(1L)).thenReturn(true);
        when(linePort.findByServiceOrderId(1L)).thenReturn(List.of(
                line(1L, "PENDING"), line(2L, "PENDING")));

        var page = useCase.list(1L, null, 2, null);

        assertThat(page.data()).hasSize(2);
        assertThat(page.isLast()).isTrue();
        assertThat(page.cursor()).isNull();
    }
}
