package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

import java.util.List;

public record PaginatedServiceLineResponse(
        List<ServiceOrderCascadeResponse.ServiceLineResponse> data,
        int pageSize,
        String cursor,
        boolean isLast
) {
}
