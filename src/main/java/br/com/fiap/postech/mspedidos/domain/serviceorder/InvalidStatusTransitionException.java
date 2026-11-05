package br.com.fiap.postech.mspedidos.domain.serviceorder;

public class InvalidStatusTransitionException extends RuntimeException {

    private final ServiceOrderStatus source;
    private final ServiceOrderStatus target;

    public InvalidStatusTransitionException(ServiceOrderStatus source, ServiceOrderStatus target) {
        super("Status change not allowed: " + source + " -> " + target);
        this.source = source;
        this.target = target;
    }

    public ServiceOrderStatus getSource() {
        return source;
    }

    public ServiceOrderStatus getTarget() {
        return target;
    }
}
