package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import java.util.List;

public record ServiceOrderCascadeRequest(
        Long ownerId,
        Long vehicleId,
        Long branchId,
        String description,
        OwnerPayload owner,
        VehiclePayload vehicle,
        List<Long> catalogServiceIds
) {

    public record OwnerPayload(String name, String document, String documentType, String phone, String email) {
    }

    public record VehiclePayload(Long ownerId, String licensePlate, String brand, String model,
                                 Integer year, String color) {
    }
}
