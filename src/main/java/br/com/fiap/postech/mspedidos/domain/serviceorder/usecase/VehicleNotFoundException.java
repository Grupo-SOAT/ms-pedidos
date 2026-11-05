package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(Long vehicleId) {
        super("Vehicle not found for id: " + vehicleId);
    }
}
