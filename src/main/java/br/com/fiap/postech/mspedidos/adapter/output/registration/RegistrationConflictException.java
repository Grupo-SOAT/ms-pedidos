package br.com.fiap.postech.mspedidos.adapter.output.registration;

public class RegistrationConflictException extends RuntimeException {

    private final String reason;

    public RegistrationConflictException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
