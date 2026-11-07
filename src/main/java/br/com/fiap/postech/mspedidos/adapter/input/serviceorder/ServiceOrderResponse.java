package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;

import java.math.BigDecimal;
import java.time.Instant;

public record ServiceOrderResponse(
        Long id,
        Long clientId,
        Long vehicleId,
        Long branchId,
        String description,
        String status,
        String statusLabel,
        BigDecimal estimatedAmount,
        Instant createdAt,
        Instant updatedAt
) {

    public static ServiceOrderResponse from(ServiceOrder order) {
        return new ServiceOrderResponse(
                order.getId(),
                order.getClientId(),
                order.getVehicleId(),
                order.getBranchId(),
                order.getDescription(),
                order.getStatus().name(),
                order.getStatusLabel(),
                order.getEstimatedAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
