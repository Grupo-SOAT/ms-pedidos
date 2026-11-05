package br.com.fiap.postech.mspedidos.adapter.input.message;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ChangeServiceOrderStatusUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ExecutionEventListenerTest {

    private final ChangeServiceOrderStatusUseCase useCase = mock(ChangeServiceOrderStatusUseCase.class);
    private final ExecutionEventListener listener = new ExecutionEventListener(new ObjectMapper(), useCase);

    @Test
    void should_move_order_to_in_progress_on_start() {
        listener.consume("""
                {"eventId":"evt-1","type":"EXECUTION_STARTED","serviceOrderId":7,"correlationId":"corr-1"}
                """);

        verify(useCase).changeStatus(7L, ServiceOrderStatus.IN_PROGRESS, "corr-1");
    }

    @Test
    void should_move_order_to_completed_on_completion() {
        listener.consume("""
                {"eventId":"evt-2","type":"EXECUTION_COMPLETED","serviceOrderId":8,"correlationId":"corr-2"}
                """);

        verify(useCase).changeStatus(8L, ServiceOrderStatus.COMPLETED, "corr-2");
    }

    @Test
    void should_ignore_unknown_execution_event() {
        listener.consume("""
                {"eventId":"evt-3","type":"EXECUTION_CANCELLED","serviceOrderId":7}
                """);

        verify(useCase, never()).changeStatus(any(), any(), any());
    }

    @Test
    void should_reject_malformed_payload() {
        assertThatThrownBy(() -> listener.consume("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
