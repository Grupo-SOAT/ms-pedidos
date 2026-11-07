package br.com.fiap.postech.mspedidos.config;

import br.com.fiap.postech.mspedidos.domain.reporting.CalculateServiceAverageTimeUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ChangeServiceOrderStatusUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.CreateServiceOrderCascadeUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.CreateServiceOrderUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.RegisterProgressUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceLineUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceOrderQueryUseCase;
import br.com.fiap.postech.mspedidos.port.message.ServiceOrderEventPublisherPort;
import br.com.fiap.postech.mspedidos.port.monitoring.ServiceOrderMetricsPort;
import br.com.fiap.postech.mspedidos.port.persistence.OwnerExistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import br.com.fiap.postech.mspedidos.port.CatalogQueryPort;
import br.com.fiap.postech.mspedidos.port.RegistrationCommandPort;
import br.com.fiap.postech.mspedidos.port.ServiceLinePersistencePort;
import br.com.fiap.postech.mspedidos.port.ServiceLineQueryPort;
import br.com.fiap.postech.mspedidos.port.persistence.VehicleExistencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean
    public CreateServiceOrderUseCase createServiceOrderUseCase(
            ServiceOrderPersistencePort persistencePort,
            OwnerExistencePort ownerExistencePort,
            VehicleExistencePort vehicleExistencePort,
            ServiceOrderEventPublisherPort eventPublisherPort,
            Clock clock
    ) {
        return new CreateServiceOrderUseCase(persistencePort, ownerExistencePort, vehicleExistencePort,
                eventPublisherPort, clock);
    }

    @Bean
    public CreateServiceOrderCascadeUseCase createServiceOrderCascadeUseCase(
            CreateServiceOrderUseCase createServiceOrderUseCase,
            RegistrationCommandPort registrationCommandPort,
            CatalogQueryPort catalogQueryPort,
            ServiceLinePersistencePort serviceLinePersistencePort,
            Clock clock
    ) {
        return new CreateServiceOrderCascadeUseCase(createServiceOrderUseCase, registrationCommandPort,
                catalogQueryPort, serviceLinePersistencePort, clock);
    }

    @Bean
    public ServiceOrderQueryUseCase serviceOrderQueryUseCase(
            ServiceOrderPersistencePort persistencePort,
            OwnerExistencePort ownerExistencePort,
            VehicleExistencePort vehicleExistencePort,
            Clock clock
    ) {
        return new ServiceOrderQueryUseCase(persistencePort, ownerExistencePort,
                vehicleExistencePort, clock);
    }

    @Bean
    public ServiceLineUseCase serviceLineUseCase(
            ServiceOrderPersistencePort serviceOrderPersistencePort,
            ServiceLinePersistencePort serviceLinePersistencePort,
            CatalogQueryPort catalogQueryPort,
            Clock clock
    ) {
        return new ServiceLineUseCase(serviceOrderPersistencePort, serviceLinePersistencePort,
                catalogQueryPort, clock);
    }

    @Bean
    public RegisterProgressUseCase registerProgressUseCase(
            ServiceOrderPersistencePort serviceOrderPersistencePort,
            ServiceLinePersistencePort serviceLinePersistencePort,
            ChangeServiceOrderStatusUseCase changeServiceOrderStatusUseCase,
            Clock clock
    ) {
        return new RegisterProgressUseCase(serviceOrderPersistencePort, serviceLinePersistencePort,
                changeServiceOrderStatusUseCase, clock);
    }

    @Bean
    public CalculateServiceAverageTimeUseCase calculateServiceAverageTimeUseCase(
            ServiceLineQueryPort queryPort
    ) {
        return new CalculateServiceAverageTimeUseCase(queryPort);
    }

    @Bean
    public ChangeServiceOrderStatusUseCase changeServiceOrderStatusUseCase(
            ServiceOrderPersistencePort persistencePort,
            ServiceOrderEventPublisherPort eventPublisherPort,
            ServiceOrderMetricsPort metricsPort,
            ServiceLineQueryPort serviceLineQueryPort,
            Clock clock
    ) {
        return new ChangeServiceOrderStatusUseCase(persistencePort, eventPublisherPort, metricsPort,
                serviceLineQueryPort, clock);
    }
}
