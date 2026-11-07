package br.com.fiap.postech.mspedidos.domain.serviceorder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Service order aggregate. Owns its lifecycle and rejects transitions that are not part of the
 * workshop state machine.
 */
public class ServiceOrder {

    private Long id;
    private Long clientId;
    private Long vehicleId;
    private final Long branchId;
    private String description;
    private ServiceOrderStatus status;
    private BigDecimal estimatedAmount;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant inspectedAt;
    private Instant approvedAt;
    private Instant rejectedAt;
    private Instant cancelledAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant deliveredAt;
    private Instant partiallyRejectedAt;

    private ServiceOrder(
            Long id,
            Long clientId,
            Long vehicleId,
            Long branchId,
            String description,
            ServiceOrderStatus status,
            BigDecimal estimatedAmount,
            Instant createdAt
    ) {
        this.id = id;
        this.clientId = clientId;
        this.vehicleId = vehicleId;
        this.branchId = branchId;
        this.description = description;
        this.status = status;
        this.estimatedAmount = estimatedAmount;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static ServiceOrder create(
            Long clientId,
            Long vehicleId,
            Long branchId,
            String description,
            Instant now
    ) {
        Objects.requireNonNull(clientId, "clientId is required");
        Objects.requireNonNull(vehicleId, "vehicleId is required");
        Objects.requireNonNull(branchId, "branchId is required");
        Objects.requireNonNull(description, "description is required");
        return new ServiceOrder(null, clientId, vehicleId, branchId, description,
                ServiceOrderStatus.PENDING, null, now);
    }

    public static ServiceOrder restore(
            Long id,
            Long clientId,
            Long vehicleId,
            Long branchId,
            String description,
            ServiceOrderStatus status,
            BigDecimal estimatedAmount,
            Instant createdAt,
            Instant updatedAt,
            Instant inspectedAt,
            Instant approvedAt,
            Instant rejectedAt,
            Instant cancelledAt,
            Instant startedAt,
            Instant completedAt,
            Instant deliveredAt,
            Instant partiallyRejectedAt
    ) {
        ServiceOrder order = new ServiceOrder(id, clientId, vehicleId, branchId, description, status,
                estimatedAmount, createdAt);
        order.updatedAt = updatedAt;
        order.inspectedAt = inspectedAt;
        order.approvedAt = approvedAt;
        order.rejectedAt = rejectedAt;
        order.cancelledAt = cancelledAt;
        order.startedAt = startedAt;
        order.completedAt = completedAt;
        order.deliveredAt = deliveredAt;
        order.partiallyRejectedAt = partiallyRejectedAt;
        return order;
    }

    public void transitionTo(ServiceOrderStatus target, Instant now) {
        Objects.requireNonNull(target, "target status is required");
        if (target == status) {
            return;
        }
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(status, target);
        }

        this.status = target;
        this.updatedAt = now;
        switch (target) {
            case IN_INSPECTION -> inspectedAt = now;
            case APPROVED -> approvedAt = now;
            case CANCELLED -> cancelledAt = now;
            case IN_PROGRESS -> startedAt = now;
            case COMPLETED -> completedAt = now;
            case DELIVERED -> deliveredAt = now;
            case PARTIALLY_REJECTED -> partiallyRejectedAt = now;
            default -> {
            }
        }
    }

    public void updateDetails(Long clientId, Long vehicleId, String description, Instant now) {
        if (clientId != null) {
            this.clientId = clientId;
        }
        if (vehicleId != null) {
            this.vehicleId = vehicleId;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
        this.updatedAt = now;
    }

    public String getStatusLabel() {
        return status.label();
    }

    public void applyEstimatedAmount(BigDecimal amount, Instant now) {
        this.estimatedAmount = amount;
        this.updatedAt = now;
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getClientId() {
        return clientId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public String getDescription() {
        return description;
    }

    public ServiceOrderStatus getStatus() {
        return status;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getInspectedAt() {
        return inspectedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getPartiallyRejectedAt() {
        return partiallyRejectedAt;
    }
}
