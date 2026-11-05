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

class PaymentEventListenerTest {

    private final ChangeServiceOrderStatusUseCase useCase = mock(ChangeServiceOrderStatusUseCase.class);
    private final PaymentEventListener listener = new PaymentEventListener(new ObjectMapper(), useCase);

    @Test
    void should_cancel_service_order_on_payment_rejected() {
        listener.consume("""
                {"eventId":"evt-1","type":"PAYMENT_REJECTED","serviceOrderId":7,"correlationId":"corr-1"}
                """, "7");

        verify(useCase).changeStatus(7L, ServiceOrderStatus.CANCELLED, "corr-1");
    }

    @Test
    void should_cancel_service_order_on_payment_timeout() {
        listener.consume("""
                {"eventId":"evt-2","type":"PAYMENT_TIMEOUT","serviceOrderId":8,"correlationId":"corr-2"}
                """, "8");

        verify(useCase).changeStatus(8L, ServiceOrderStatus.CANCELLED, "corr-2");
    }

    @Test
    void should_ignore_payment_approved() {
        listener.consume("""
                {"eventId":"evt-3","type":"PAYMENT_APPROVED","serviceOrderId":7}
                """, "7");

        verify(useCase, never()).changeStatus(any(), any(), any());
    }

    @Test
    void should_reject_malformed_payload() {
        assertThatThrownBy(() -> listener.consume("not-json", "7"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
