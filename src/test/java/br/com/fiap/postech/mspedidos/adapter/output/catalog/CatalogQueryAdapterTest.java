package br.com.fiap.postech.mspedidos.adapter.output.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogQueryAdapterTest {

    private MockRestServiceServer server;
    private CatalogQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new CatalogQueryAdapter(builder, "http://localhost:8080", "local-key");
    }

    @Test
    void should_map_catalog_service() {
        server.expect(requestTo("http://localhost:8080/catalog/services/7"))
                .andRespond(withSuccess("""
                        {"id":7,"name":"Troca de oleo","basePrice":100.00}
                        """, MediaType.APPLICATION_JSON));

        var result = adapter.findById(7L);

        assertThat(result).usingRecursiveComparison().isEqualTo(
                new br.com.fiap.postech.mspedidos.port.CatalogQueryPort.CatalogServiceInfo(
                        7L, "Troca de oleo", new BigDecimal("100.00")));
    }

    @Test
    void should_reject_empty_response() {
        server.expect(requestTo("http://localhost:8080/catalog/services/9"))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> adapter.findById(9L))
                .isInstanceOf(CatalogQueryAdapter.CatalogServiceNotFoundException.class);
    }

    @Test
    void should_map_not_found_to_catalog_exception() {
        server.expect(requestTo("http://localhost:8080/catalog/services/99999"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.findById(99999L))
                .isInstanceOf(CatalogQueryAdapter.CatalogServiceNotFoundException.class);
    }
}
