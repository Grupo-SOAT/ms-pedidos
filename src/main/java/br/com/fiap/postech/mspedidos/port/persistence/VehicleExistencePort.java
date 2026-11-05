package br.com.fiap.postech.mspedidos.port.persistence;

public interface VehicleExistencePort {

    boolean vehicleExistsById(Long vehicleId);
}
