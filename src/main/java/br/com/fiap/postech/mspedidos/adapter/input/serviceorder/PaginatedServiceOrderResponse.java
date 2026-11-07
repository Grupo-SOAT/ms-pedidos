package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import java.util.List;

public record PaginatedServiceOrderResponse(
        List<ServiceOrderResponse> data,
        int pageSize,
        String cursor,
        boolean isLast
) {
}
