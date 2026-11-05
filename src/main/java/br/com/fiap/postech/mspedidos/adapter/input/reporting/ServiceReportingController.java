package br.com.fiap.postech.mspedidos.adapter.input.reporting;

import br.com.fiap.postech.mspedidos.domain.reporting.CalculateServiceAverageTimeUseCase;
import br.com.fiap.postech.mspedidos.domain.reporting.ServiceAverageTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ServiceReportingController {

    private final CalculateServiceAverageTimeUseCase useCase;

    public ServiceReportingController(CalculateServiceAverageTimeUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/services-average-time")
    public ResponseEntity<List<ServiceAverageTime>> all() {
        return ResponseEntity.ok(useCase.all());
    }

    @GetMapping("/catalog/services/{id}/average-time")
    public ResponseEntity<ServiceAverageTime> byCatalogService(@PathVariable Long id) {
        return useCase.byCatalogServiceId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
