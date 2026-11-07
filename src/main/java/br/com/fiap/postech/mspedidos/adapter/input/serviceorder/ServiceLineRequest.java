package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import java.math.BigDecimal;

public record ServiceLineRequest(
        Long serviceOrderId,
        Long catalogServiceId,
        BigDecimal price,
        String status
) {
}
