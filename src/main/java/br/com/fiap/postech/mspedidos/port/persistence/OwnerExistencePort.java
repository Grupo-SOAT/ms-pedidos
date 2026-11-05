package br.com.fiap.postech.mspedidos.port.persistence;

public interface OwnerExistencePort {

    boolean existsById(Long ownerId);
}
