package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

public class ServiceOrderNotFoundException extends RuntimeException {

    public ServiceOrderNotFoundException(Long id) {
        super("Service order not found for id: " + id);
    }
}
