package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ServiceOrderCascadeResponse(
        ServiceOrderResponse order,
        List<ServiceLineResponse> services
) {

    public record ServiceLineResponse(
            Long id,
            Long catalogServiceId,
            String catalogServiceName,
            BigDecimal price,
            String status,
            Instant createdAt
    ) {
    }
}
