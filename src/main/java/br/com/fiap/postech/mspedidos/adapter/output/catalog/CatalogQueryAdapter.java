package br.com.fiap.postech.mspedidos.adapter.output.catalog;

import br.com.fiap.postech.mspedidos.port.CatalogQueryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Component
public class CatalogQueryAdapter implements CatalogQueryPort {

    private final RestClient restClient;

    public CatalogQueryAdapter(RestClient.Builder builder,
                               @Value("${app.mnl.base-url:http://localhost:8080}") String baseUrl,
                               @Value("${app.internal.api-key:local-internal-key}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();
    }

    @Override
    public CatalogServiceInfo findById(Long catalogServiceId) {
        try {
            CatalogServiceResponse response = restClient.get()
                    .uri("/catalog/services/{id}", catalogServiceId)
                    .retrieve()
                    .body(CatalogServiceResponse.class);
            if (response == null) {
                throw new CatalogServiceNotFoundException(catalogServiceId);
            }
            return new CatalogServiceInfo(response.id(), response.name(), response.basePrice());
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new CatalogServiceNotFoundException(catalogServiceId);
            }
            throw exception;
        }
    }

    public record CatalogServiceResponse(Long id, String name, BigDecimal basePrice) {
    }

    public static class CatalogServiceNotFoundException extends RuntimeException {
        public CatalogServiceNotFoundException(Long id) {
            super("Catalog service not found for id: " + id);
        }
    }
}
