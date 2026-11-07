package br.com.fiap.postech.mspedidos.adapter.output.registration;

import br.com.fiap.postech.mspedidos.adapter.output.registration.RegistrationQueryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;

class RegistrationQueryAdapterTest {

    private MockRestServiceServer server;
    private RegistrationQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new RegistrationQueryAdapter(builder, "http://localhost:8080", "local-key");
    }

    @Test
    void should_report_existing_owner() {
        server.expect(requestTo("http://localhost:8080/owners/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(adapter.existsById(1L)).isTrue();
        server.verify();
    }

    @Test
    void should_report_missing_owner() {
        server.expect(requestTo("http://localhost:8080/owners/2"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adapter.existsById(2L)).isFalse();
    }

    @Test
    void should_report_existing_vehicle() {
        server.expect(requestTo("http://localhost:8080/vehicles/3"))
                .andRespond(withStatus(HttpStatus.OK));

        assertThat(adapter.vehicleExistsById(3L)).isTrue();
    }

    @Test
    void should_report_missing_vehicle() {
        server.expect(requestTo("http://localhost:8080/vehicles/4"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adapter.vehicleExistsById(4L)).isFalse();
    }

    @Test
    void should_create_owner_and_return_id() {
        server.expect(requestTo("http://localhost:8080/owners"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":77}", MediaType.APPLICATION_JSON));

        Long id = adapter.createOwner(new br.com.fiap.postech.mspedidos.port.RegistrationCommandPort.OwnerData(
                "Joao", "31058167049", "CPF", "1199", "joao@email.com"));

        assertThat(id).isEqualTo(77L);
    }

    @Test
    void should_create_vehicle_and_return_id() {
        server.expect(requestTo("http://localhost:8080/vehicles"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":88}", MediaType.APPLICATION_JSON));

        Long id = adapter.createVehicle(new br.com.fiap.postech.mspedidos.port.RegistrationCommandPort.VehicleData(
                1L, "ABC1D23", "Fiat", "Uno", 2020, "Branco"));

        assertThat(id).isEqualTo(88L);
    }

    @Test
    void should_reject_registration_response_without_id() {
        server.expect(requestTo("http://localhost:8080/owners"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.createOwner(
                new br.com.fiap.postech.mspedidos.port.RegistrationCommandPort.OwnerData(
                        "Joao", "31058167049", "CPF", "1199", "joao@email.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_propagate_registration_conflict_reason() {
        server.expect(requestTo("http://localhost:8080/owners"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"reason\":\"OWNER_ALREADY_EXISTS\"}"));

        assertThatThrownBy(() -> adapter.createOwner(
                new br.com.fiap.postech.mspedidos.port.RegistrationCommandPort.OwnerData(
                        "Joao", "31058167049", "CPF", "1199", "joao@email.com")))
                .isInstanceOf(br.com.fiap.postech.mspedidos.adapter.output.registration
                        .RegistrationConflictException.class)
                .hasMessage("OWNER_ALREADY_EXISTS");
    }

    @Test
    void should_use_generic_reason_when_conflict_body_has_no_reason() {
        server.expect(requestTo("http://localhost:8080/owners"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}"));

        assertThatThrownBy(() -> adapter.createOwner(
                new br.com.fiap.postech.mspedidos.port.RegistrationCommandPort.OwnerData(
                        "Joao", "31058167049", "CPF", "1199", "joao@email.com")))
                .isInstanceOf(br.com.fiap.postech.mspedidos.adapter.output.registration
                        .RegistrationConflictException.class)
                .hasMessage("REGISTRATION_CONFLICT");
    }
}
