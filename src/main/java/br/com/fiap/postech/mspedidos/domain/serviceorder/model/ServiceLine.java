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
    private final Long catalogServiceId;
    private final String catalogServiceName;
    private final BigDecimal price;
    private final String status;
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
