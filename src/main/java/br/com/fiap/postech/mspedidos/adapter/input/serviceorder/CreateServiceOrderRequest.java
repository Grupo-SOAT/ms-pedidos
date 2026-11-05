package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

public record CreateServiceOrderRequest(
        Long clientId,
        Long vehicleId,
        Long branchId,
        String description
) {
}
