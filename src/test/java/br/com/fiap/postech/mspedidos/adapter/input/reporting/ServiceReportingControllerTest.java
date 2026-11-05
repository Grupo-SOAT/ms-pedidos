package br.com.fiap.postech.mspedidos.adapter.input.reporting;

import br.com.fiap.postech.mspedidos.domain.reporting.CalculateServiceAverageTimeUseCase;
import br.com.fiap.postech.mspedidos.domain.reporting.ServiceAverageTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceReportingControllerTest {

    private final CalculateServiceAverageTimeUseCase useCase = mock(CalculateServiceAverageTimeUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ServiceReportingController(useCase)).build();
    }

    @Test
    void should_return_all_average_times() throws Exception {
        when(useCase.all()).thenReturn(List.of(
                new ServiceAverageTime(1L, "Oleo", 2, 1, 4.0, 2.0, 3.0, 1.0)));

        mockMvc.perform(get("/reports/services-average-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Oleo"));
    }

    @Test
    void should_return_average_time_for_catalog_service() throws Exception {
        when(useCase.byCatalogServiceId(7L)).thenReturn(Optional.of(
                new ServiceAverageTime(7L, "Troca", 1, 1, 4.0, 2.0, 3.0, 1.0)));

        mockMvc.perform(get("/reports/catalog/services/7/average-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void should_return_not_found_when_catalog_service_has_no_lines() throws Exception {
        when(useCase.byCatalogServiceId(9L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/reports/catalog/services/9/average-time"))
                .andExpect(status().isNotFound());
    }
}
