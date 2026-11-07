package br.com.fiap.postech.mspedidos.domain.serviceorder;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle states of a service order and the transitions allowed between them.
 */
public enum ServiceOrderStatus {

    PENDING("Recebida"),
    IN_INSPECTION("Em Diagnóstico"),
    AWAITING_APPROVAL("Aguardando Aprovação"),
    PARTIALLY_REJECTED("Parcialmente Rejeitada"),
    CANCELLED("Cancelada"),
    APPROVED("Aprovada"),
    IN_PROGRESS("Em Andamento"),
    COMPLETED("Finalizada"),
    DELIVERED("Entregue");

    private final String label;

    ServiceOrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean canTransitionTo(ServiceOrderStatus target) {
        return allowedTargets().contains(target);
    }

    private Set<ServiceOrderStatus> allowedTargets() {
        return switch (this) {
            case PENDING -> EnumSet.of(IN_INSPECTION, CANCELLED);
            case IN_INSPECTION -> EnumSet.of(AWAITING_APPROVAL, CANCELLED);
            case AWAITING_APPROVAL -> EnumSet.of(APPROVED, CANCELLED, PARTIALLY_REJECTED);
            case PARTIALLY_REJECTED -> EnumSet.of(IN_INSPECTION, CANCELLED);
            case APPROVED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED -> EnumSet.of(DELIVERED);
            case CANCELLED -> EnumSet.of(DELIVERED);
            case DELIVERED -> EnumSet.noneOf(ServiceOrderStatus.class);
        };
    }
}
