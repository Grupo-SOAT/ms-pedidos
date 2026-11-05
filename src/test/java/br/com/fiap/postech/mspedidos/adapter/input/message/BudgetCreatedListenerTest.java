package br.com.fiap.postech.mspedidos.adapter.input.message;

import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ChangeServiceOrderStatusUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BudgetCreatedListenerTest {

    private final ChangeServiceOrderStatusUseCase useCase = mock(ChangeServiceOrderStatusUseCase.class);
    private final BudgetCreatedListener listener = new BudgetCreatedListener(new ObjectMapper(), useCase);

    @Test
    void should_apply_budget_created() {
        listener.consume("""
                {"eventId":"evt-1","type":"BUDGET_CREATED","serviceOrderId":7,
                 "correlationId":"corr-1","payload":{"budgetId":99,"totalAmount":250.00,"currency":"BRL"}}
                """, "7");

        verify(useCase).applyBudget(7L, new java.math.BigDecimal("250.00"), "corr-1");
    }

    @Test
    void should_ignore_other_event_types() {
        listener.consume("""
                {"eventId":"evt-2","type":"SOMETHING_ELSE","serviceOrderId":7,"payload":{}}
                """, "7");

        verify(useCase, never()).applyBudget(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void should_ignore_event_without_payload() {
        listener.consume("""
                {"eventId":"evt-3","type":"BUDGET_CREATED","serviceOrderId":7}
                """, "7");

        verify(useCase, never()).applyBudget(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void should_reject_malformed_payload() {
        assertThatThrownBy(() -> listener.consume("not-json", "7"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
