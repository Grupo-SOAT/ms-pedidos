package br.com.fiap.postech.mspedidos.domain.serviceorder.usecase;

public class ServiceLineNotFoundException extends RuntimeException {

    public ServiceLineNotFoundException(Long id) {
        super("Service line not found for id: " + id);
    }
}
