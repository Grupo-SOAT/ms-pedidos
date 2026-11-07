package br.com.fiap.postech.mspedidos.domain.serviceorder.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A service to be executed inside a service order.
 */
public class ServiceLine {

    private Long id;
    private final Long serviceOrderId;
    private Long catalogServiceId;
    private String catalogServiceName;
    private BigDecimal price;
    private String status;
    private final Instant createdAt;
    private Instant approvedAt;
    private Instant startedAt;
    private Instant completedAt;

    private ServiceLine(Long id, Long serviceOrderId, Long catalogServiceId, String catalogServiceName,
                        BigDecimal price, String status, Instant createdAt,
                        Instant approvedAt, Instant startedAt, Instant completedAt) {
        this.id = id;
        this.serviceOrderId = serviceOrderId;
        this.catalogServiceId = catalogServiceId;
        this.catalogServiceName = catalogServiceName;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public static ServiceLine create(Long serviceOrderId, Long catalogServiceId, String catalogServiceName,
                                     BigDecimal price, Instant now) {
        Objects.requireNonNull(serviceOrderId, "serviceOrderId is required");
        Objects.requireNonNull(catalogServiceId, "catalogServiceId is required");
        Objects.requireNonNull(price, "price is required");
        if (price.signum() < 0) {
            throw new InvalidServiceLineException("INVALID_PRICE");
        }
        return new ServiceLine(null, serviceOrderId, catalogServiceId, catalogServiceName, price,
                "AWAITING_APPROVAL", now, null, null, null);
    }

    public static ServiceLine restore(Long id, Long serviceOrderId, Long catalogServiceId,
                                      String catalogServiceName, BigDecimal price, String status,
                                      Instant createdAt, Instant approvedAt, Instant startedAt,
                                      Instant completedAt) {
        return new ServiceLine(id, serviceOrderId, catalogServiceId, catalogServiceName, price,
                status, createdAt, approvedAt, startedAt, completedAt);
    }

    public void updateDetails(Long catalogServiceId, String catalogServiceName, BigDecimal price) {
        if (catalogServiceId != null) {
            this.catalogServiceId = catalogServiceId;
        }
        if (catalogServiceName != null) {
            this.catalogServiceName = catalogServiceName;
        }
        if (price != null) {
            if (price.signum() < 0) {
                throw new InvalidServiceLineException("INVALID_PRICE");
            }
            this.price = price;
        }
    }

    public void approve(Instant now) {
        transitionTo("APPROVED", now);
        this.approvedAt = now;
    }

    public void start(Instant now) {
        transitionTo("IN_PROGRESS", now);
        this.startedAt = now;
    }

    public void complete(Instant now) {
        transitionTo("COMPLETED", now);
        this.completedAt = now;
    }

    public void cancel(Instant now) {
        transitionTo("CANCELLED", now);
    }

    public void moveTo(String target, Instant now) {
        transitionTo(target, now);
    }

    private void transitionTo(String target, Instant now) {
        if (target == null || target.equals(status)) {
            return;
        }
        boolean allowed = switch (status) {
            case "PENDING", "AWAITING_APPROVAL" -> target.equals("APPROVED") || target.equals("CANCELLED");
            case "APPROVED" -> target.equals("IN_PROGRESS") || target.equals("CANCELLED");
            case "IN_PROGRESS" -> target.equals("COMPLETED") || target.equals("CANCELLED");
            default -> false;
        };
        if (!allowed) {
            throw new InvalidServiceLineException("SERVICE_STATUS_CHANGE_NOT_ALLOWED");
        }
        this.status = target;
        if ("APPROVED".equals(target)) {
            this.approvedAt = now;
        } else if ("IN_PROGRESS".equals(target)) {
            this.startedAt = now;
        } else if ("COMPLETED".equals(target)) {
            this.completedAt = now;
        }
    }

    public String getStatusLabel() {
        return switch (status) {
            case "PENDING" -> "Pendente";
            case "AWAITING_APPROVAL" -> "Aguardando Aprovação";
            case "APPROVED" -> "Aprovada";
            case "IN_PROGRESS" -> "Em Andamento";
            case "COMPLETED" -> "Finalizada";
            case "CANCELLED" -> "Cancelada";
            default -> status;
        };
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getServiceOrderId() {
        return serviceOrderId;
    }

    public Long getCatalogServiceId() {
        return catalogServiceId;
    }

    public String getCatalogServiceName() {
        return catalogServiceName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
