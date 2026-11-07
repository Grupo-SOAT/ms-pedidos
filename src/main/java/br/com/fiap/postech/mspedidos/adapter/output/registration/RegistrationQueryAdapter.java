package br.com.fiap.postech.mspedidos.adapter.output.registration;

import br.com.fiap.postech.mspedidos.port.RegistrationCommandPort;
import br.com.fiap.postech.mspedidos.port.persistence.OwnerExistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.VehicleExistencePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Registration service adapter. Existence checks and cascade creation go through HTTP; this service
 * never reads the registration database.
 */
@Component
public class RegistrationQueryAdapter implements OwnerExistencePort, VehicleExistencePort, RegistrationCommandPort {

    private final RestClient restClient;

    public RegistrationQueryAdapter(RestClient.Builder builder,
                                    @Value("${app.mnl.base-url}") String baseUrl,
                                    @Value("${app.internal.api-key:local-internal-key}") String internalApiKey) {
        this.restClient = builder.baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();
    }

    @Override
    public boolean existsById(Long ownerId) {
        return exists("/owners/" + ownerId);
    }

    @Override
    public boolean vehicleExistsById(Long vehicleId) {
        return exists("/vehicles/" + vehicleId);
    }

    @Override
    public Long createOwner(OwnerData owner) {
        return post("/owners", owner);
    }

    @Override
    public Long createVehicle(VehicleData vehicle) {
        return post("/vehicles", vehicle);
    }

    private boolean exists(String path) {
        try {
            restClient.get().uri(path).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            return false;
        }
    }

    private Long post(String path, Object body) {
        try {
            IdResponse response = restClient.post()
                    .uri(path)
                    .body(body)
                    .retrieve()
                    .body(IdResponse.class);
            if (response == null || response.id() == null) {
                throw new IllegalStateException("Registration service returned no id for " + path);
            }
            return response.id();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new RegistrationConflictException(reason(exception.getResponseBodyAsString()));
            }
            throw exception;
        }
    }

    private String reason(String body) {
        try {
            String marker = "\"reason\":\"";
            int start = body.indexOf(marker);
            if (start < 0) {
                return "REGISTRATION_CONFLICT";
            }
            int from = start + marker.length();
            int end = body.indexOf('"', from);
            return end < 0 ? "REGISTRATION_CONFLICT" : body.substring(from, end);
        } catch (Exception ignored) {
            return "REGISTRATION_CONFLICT";
        }
    }

    public record IdResponse(Long id) {
    }
}
