package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import java.math.BigDecimal;
import java.time.Instant;

public record ServiceOrderResponse(
        Long id,
        Long clientId,
        Long vehicleId,
        Long branchId,
        String description,
        String status,
        BigDecimal estimatedAmount,
        Instant createdAt,
        Instant updatedAt
) {
}
