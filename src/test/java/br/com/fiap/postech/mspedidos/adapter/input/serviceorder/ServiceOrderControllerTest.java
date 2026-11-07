package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import br.com.fiap.postech.mspedidos.domain.serviceorder.InvalidStatusTransitionException;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrder;
import br.com.fiap.postech.mspedidos.domain.serviceorder.ServiceOrderStatus;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ChangeServiceOrderStatusUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.CreateServiceOrderCascadeUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.CreateServiceOrderUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.RegisterProgressUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceLineUseCase;
import br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceOrderQueryUseCase;
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
    private final ServiceOrderQueryUseCase queryUseCase = mock(ServiceOrderQueryUseCase.class);
    private final ServiceLineUseCase serviceLineUseCase = mock(ServiceLineUseCase.class);
    private final RegisterProgressUseCase progressUseCase = mock(RegisterProgressUseCase.class);

    private MockMvc mockMvc;
    private ServiceOrder order;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ServiceOrderController(createUseCase, cascadeUseCase, changeUseCase,
                        queryUseCase, serviceLineUseCase, progressUseCase, persistencePort))
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
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_find_service_order() throws Exception {
        when(queryUseCase.getById(5L)).thenReturn(order);

        mockMvc.perform(get("/service-orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void should_return_not_found_when_order_does_not_exist() throws Exception {
        when(queryUseCase.getById(9L)).thenThrow(new ServiceOrderNotFoundException(9L));

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
        when(serviceLineUseCase.list(5L, null, null, null)).thenReturn(
                new ServiceLineUseCase.ServiceLinePage(List.of(line), 10, null, true));

        mockMvc.perform(get("/service-orders/5/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(9));
    }

    @Test
    void should_list_orders_and_return_no_content_when_empty() throws Exception {
        when(queryUseCase.list(null, null, null, 10, null)).thenReturn(
                new ServiceOrderQueryUseCase.ServiceOrderPage(List.of(order), 10, null, true));
        when(queryUseCase.list("DELIVERED", null, null, 10, null)).thenReturn(
                new ServiceOrderQueryUseCase.ServiceOrderPage(List.of(), 10, null, true));

        mockMvc.perform(get("/service-orders").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(5))
                .andExpect(jsonPath("$.data[0].statusLabel").value("Recebida"));

        mockMvc.perform(get("/service-orders").param("status", "DELIVERED").param("pageSize", "10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_update_and_delete_order() throws Exception {
        when(queryUseCase.update(eq(5L), eq(1L), eq(2L), eq("Nova"), eq("IN_INSPECTION"), any()))
                .thenReturn(order);
        when(queryUseCase.getById(9L)).thenThrow(new ServiceOrderNotFoundException(9L));

        mockMvc.perform(patch("/service-orders/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"vehicleId\":2,\"description\":\"Nova\","
                                + "\"status\":\"IN_INSPECTION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/service-orders/5"))
                .andExpect(status().isAccepted());

        org.mockito.Mockito.doThrow(new ServiceOrderNotFoundException(99999L))
                .when(queryUseCase).delete(99999L);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/service-orders/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_register_progress() throws Exception {
        mockMvc.perform(post("/service-orders/5/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"START_INSPECTION\"}"))
                .andExpect(status().isAccepted());

        org.mockito.Mockito.verify(progressUseCase)
                .register(eq(5L), eq("START_INSPECTION"), eq(null), any());
    }

    @Test
    void should_create_get_update_and_delete_service_line() throws Exception {
        var line = br.com.fiap.postech.mspedidos.domain.serviceorder.model.ServiceLine.restore(
                9L, 5L, 3L, "Oleo", java.math.BigDecimal.TEN, "AWAITING_APPROVAL", Instant.now(),
                null, null, null);
        when(serviceLineUseCase.create(eq(5L), eq(3L), any())).thenReturn(line);
        when(serviceLineUseCase.get(5L, 9L)).thenReturn(line);
        when(serviceLineUseCase.update(eq(5L), eq(9L), eq(3L), any(), eq("APPROVED"))).thenReturn(line);

        mockMvc.perform(post("/service-orders/5/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"catalogServiceId\":3,\"price\":10.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusLabel").value("Aguardando Aprovação"));

        mockMvc.perform(get("/service-orders/5/services/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));

        mockMvc.perform(patch("/service-orders/5/services/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"catalogServiceId\":3,\"price\":10.00,\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/service-orders/5/services/9"))
                .andExpect(status().isAccepted());
        org.mockito.Mockito.verify(serviceLineUseCase).delete(5L, 9L);
    }

    @Test
    void should_return_no_content_when_service_list_is_empty() throws Exception {
        when(serviceLineUseCase.list(5L, null, 10, null)).thenReturn(
                new ServiceLineUseCase.ServiceLinePage(List.of(), 10, null, true));

        mockMvc.perform(get("/service-orders/5/services").param("pageSize", "10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_map_domain_failures_to_reason_payloads() throws Exception {
        when(queryUseCase.getById(9L)).thenThrow(new ServiceOrderNotFoundException(9L));
        mockMvc.perform(get("/service-orders/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason").value("SERVICE_ORDER_NOT_FOUND"));

        when(serviceLineUseCase.get(5L, 9L)).thenThrow(
                new br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.ServiceLineNotFoundException(9L));
        mockMvc.perform(get("/service-orders/5/services/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason").value("SERVICE_NOT_FOUND"));

        when(serviceLineUseCase.get(5L, 8L)).thenThrow(
                new br.com.fiap.postech.mspedidos.domain.serviceorder.model.InvalidServiceLineException(
                        "INVALID_PRICE"));
        mockMvc.perform(get("/service-orders/5/services/8"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("INVALID_PRICE"));

        org.mockito.Mockito.doThrow(
                new br.com.fiap.postech.mspedidos.domain.serviceorder.usecase.VehicleNotFoundException(9L))
                .when(createUseCase).create(any(), any(), any(), any(), any());
        mockMvc.perform(post("/service-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"vehicleId\":9,\"branchId\":3,\"description\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("SERVICE_ORDER_VEHICLE_NOT_FOUND"));

        org.mockito.Mockito.doThrow(
                new br.com.fiap.postech.mspedidos.adapter.output.catalog.CatalogQueryAdapter
                        .CatalogServiceNotFoundException(9L))
                .when(createUseCase).create(any(), any(), any(), any(), any());
        mockMvc.perform(post("/service-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"vehicleId\":1,\"branchId\":3,\"description\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason").value("CATALOG_SERVICE_NOT_FOUND"));

        org.mockito.Mockito.doThrow(
                new br.com.fiap.postech.mspedidos.adapter.output.registration.RegistrationConflictException(
                        "OWNER_ALREADY_EXISTS"))
                .when(createUseCase).create(any(), any(), any(), any(), any());
        mockMvc.perform(post("/service-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"vehicleId\":1,\"branchId\":3,\"description\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("OWNER_ALREADY_EXISTS"));
    }

    @Test
    void should_map_invalid_transition_to_conflict() throws Exception {
        when(changeUseCase.changeStatus(eq(5L), eq(ServiceOrderStatus.APPROVED), any()))
                .thenThrow(new InvalidStatusTransitionException(
                        ServiceOrderStatus.PENDING, ServiceOrderStatus.APPROVED));

        mockMvc.perform(patch("/service-orders/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("STATUS_CHANGE_NOT_ALLOWED"));
    }
}
