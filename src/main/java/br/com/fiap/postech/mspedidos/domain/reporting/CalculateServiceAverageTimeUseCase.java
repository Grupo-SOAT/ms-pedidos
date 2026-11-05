package br.com.fiap.postech.mspedidos.domain.reporting;

import br.com.fiap.postech.mspedidos.port.ServiceLineQueryPort;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CalculateServiceAverageTimeUseCase {

    private final ServiceLineQueryPort queryPort;

    public CalculateServiceAverageTimeUseCase(ServiceLineQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Optional<ServiceAverageTime> byCatalogServiceId(Long catalogServiceId) {
        List<ServiceLineSnapshot> lines = queryPort.findByCatalogServiceId(catalogServiceId);
        if (lines.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ServiceAverageTimeCalculator.calculate(
                catalogServiceId, lines.get(0).catalogServiceName(), lines));
    }

    public List<ServiceAverageTime> all() {
        Map<Long, List<ServiceLineSnapshot>> byCatalogId = queryPort.findAll().stream()
                .collect(Collectors.groupingBy(ServiceLineSnapshot::catalogServiceId));
        return byCatalogId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> ServiceAverageTimeCalculator.calculate(
                        entry.getKey(), entry.getValue().get(0).catalogServiceName(), entry.getValue()))
                .sorted(Comparator.comparing(ServiceAverageTime::id))
                .toList();
    }
}
