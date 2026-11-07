package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

public record UpdateServiceOrderRequest(
        Long clientId,
        Long vehicleId,
        String description,
        String status
) {
}
