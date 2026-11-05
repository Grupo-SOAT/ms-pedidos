package br.com.fiap.postech.mspedidos.adapter.input.message.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class ExecutionEvent {

    private String eventId;
    private String type;
    private Instant occurredAt;
    private String correlationId;
    private Long serviceOrderId;
    private Long branchId;
}
