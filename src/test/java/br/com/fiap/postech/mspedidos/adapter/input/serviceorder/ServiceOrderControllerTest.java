package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import br.com.fiap.postech.mspedidos.domain.serviceorder.InvalidStatusTransitionException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ChangeServiceOrderStatusUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.CreateServiceOrderCascadeUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.CreateServiceOrderUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.OwnerNotFoundException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceOrderNotFoundException;
import br.com.fiap.postech.mspedidos.port.ServiceLinePersistencePort;
import br.com.fiap.postech.mspedidos.port.persistence.ServiceOrderPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceOrderControllerTest {

    private final CreateServiceOrderUseCase createUseCase = mock(CreateServiceOrderUseCase.class);
    private final CreateServiceOrderCascadeUseCase cascadeUseCase = mock(CreateServiceOrderCascadeUseCase.class);
    private final ChangeServiceOrderStatusUseCase changeUseCase = mock(ChangeServiceOrderStatusUseCase.class);
    private final ServiceOrderPersistencePort persistencePort = mock(ServiceOrderPersistencePort.class);
    private final ServiceLinePersistencePort serviceLinePersistencePort = mock(ServiceLinePersistencePort.class);

    private MockMvc mockMvc;
    private ServiceOrder order;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ServiceOrderController(createUseCase, cascadeUseCase, changeUseCase,
                        persistencePort, serviceLinePersistencePort))
                .setControllerAdvice(new ServiceOrderExceptionHandler())
                .build();
        order = ServiceOrder.restore(5L, 1L, 2L, 3L, "Revisao", ServiceOrderStatus.PENDING,
                null, Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T10:00:00Z"),
                null, null, null, null, null, null, null, null);
    }

    @Test
    void should_create_service_order() throws Exception {
        when(createUseCase.create(any(), any(), any(), any(), any())).thenReturn(order);

        mockMvc.perform(post("/service-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "corr-1")
                        .content("{\"clientId\":1,\"vehicleId\":2,\"branchId\":3,\"description\":\"Revisao\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.branchId").value(3));
    }

    @Test
    void should_return_not_found_when_owner_is_missing() throws Exception {
        when(createUseCase.create(any(), any(), any(), any(), any())).thenThrow(new OwnerNotFoundException(1L));

        mockMvc.perform(post("/service-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"vehicleId\":2,\"branchId\":3,\"description\":\"Revisao\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_find_service_order() throws Exception {
        when(persistencePort.findById(5L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/service-orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void should_return_not_found_when_order_does_not_exist() throws Exception {
        when(persistencePort.findById(9L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/service-orders/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_change_status() throws Exception {
        ServiceOrder updated = ServiceOrder.restore(5L, 1L, 2L, 3L, "Revisao",
                ServiceOrderStatus.IN_INSPECTION, null, Instant.now(), Instant.now(),
                Instant.now(), null, null, null, null, null, null, null);
        when(changeUseCase.changeStatus(eq(5L), eq(ServiceOrderStatus.IN_INSPECTION), any())).thenReturn(updated);

        mockMvc.perform(patch("/service-orders/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"in_inspection\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_INSPECTION"));
    }

    @Test
    void should_return_conflict_for_invalid_transition() throws Exception {
        when(changeUseCase.changeStatus(eq(5L), eq(ServiceOrderStatus.APPROVED), any()))
                .thenThrow(new InvalidStatusTransitionException(ServiceOrderStatus.PENDING, ServiceOrderStatus.APPROVED));

        mockMvc.perform(patch("/service-orders/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void should_apply_budget() throws Exception {
        when(changeUseCase.applyBudget(eq(5L), any(), any())).thenReturn(order);

        mockMvc.perform(patch("/service-orders/5/budget")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalAmount\":100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void should_return_bad_request_for_unknown_status() throws Exception {
        mockMvc.perform(patch("/service-orders/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOPE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_create_cascade() throws Exception {
        var line = br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine.restore(
                9L, 5L, 3L, "Oleo", java.math.BigDecimal.TEN, "AWAITING_APPROVAL", Instant.now(),
                null, null, null);
        when(cascadeUseCase.execute(any())).thenReturn(
                new CreateServiceOrderCascadeUseCase.CascadeResult(order, List.of(line)));

        mockMvc.perform(post("/service-orders/cascade")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerId":1,"vehicleId":2,"branchId":3,"description":"Revisao",
                                 "owner":{"name":"Joao","document":"31058167049","documentType":"CPF",
                                          "phone":"1199","email":"joao@email.com"},
                                 "vehicle":{"ownerId":1,"licensePlate":"ABC1D23","brand":"Fiat",
                                            "model":"Uno","year":2020,"color":"Branco"},
                                 "catalogServiceIds":[3]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order.id").value(5))
                .andExpect(jsonPath("$.services[0].catalogServiceId").value(3));

        org.mockito.ArgumentCaptor<CreateServiceOrderCascadeUseCase.ServiceOrderCascadeCommand> captor =
                org.mockito.ArgumentCaptor.forClass(CreateServiceOrderCascadeUseCase.ServiceOrderCascadeCommand.class);
        org.mockito.Mockito.verify(cascadeUseCase).execute(captor.capture());
        assertThat(captor.getValue().owner()).isNotNull();
        assertThat(captor.getValue().vehicle()).isNotNull();
    }

    @Test
    void should_list_service_lines() throws Exception {
        var line = br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine.restore(
                9L, 5L, 3L, "Oleo", java.math.BigDecimal.TEN, "AWAITING_APPROVAL", Instant.now(),
                null, null, null);
        when(serviceLinePersistencePort.findByServiceOrderId(5L)).thenReturn(List.of(line));

        mockMvc.perform(get("/service-orders/5/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9));
    }
}
