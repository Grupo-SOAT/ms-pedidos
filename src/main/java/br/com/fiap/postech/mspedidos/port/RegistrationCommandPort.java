package br.com.fiap.postech.mspedidos.port;

public interface RegistrationCommandPort {

    Long createOwner(OwnerData owner);

    Long createVehicle(VehicleData vehicle);

    record OwnerData(String name, String document, String documentType, String phone, String email) {
    }

    record VehicleData(Long ownerId, String licensePlate, String brand, String model,
                       Integer year, String color) {

        public VehicleData withOwnerId(Long resolvedOwnerId) {
            return new VehicleData(resolvedOwnerId, licensePlate, brand, model, year, color);
        }
    }
}
