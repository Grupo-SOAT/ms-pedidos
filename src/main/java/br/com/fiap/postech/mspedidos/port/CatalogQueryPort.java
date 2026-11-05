package br.com.fiap.postech.mspedidos.port;

import java.math.BigDecimal;

public interface CatalogQueryPort {

    CatalogServiceInfo findById(Long catalogServiceId);

    record CatalogServiceInfo(Long id, String name, BigDecimal basePrice) {
    }
}
