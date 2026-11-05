package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

public class OwnerNotFoundException extends RuntimeException {

    public OwnerNotFoundException(Long ownerId) {
        super("Owner not found for id: " + ownerId);
    }
}
