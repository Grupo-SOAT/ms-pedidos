package br.com.fiap.postech.mspedidos.domain.serviceorder.model;

public class InvalidServiceLineException extends RuntimeException {

    private final String reason;

    public InvalidServiceLineException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
