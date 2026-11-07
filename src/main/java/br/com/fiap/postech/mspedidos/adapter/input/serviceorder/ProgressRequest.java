package br.com.fiap.postech.mspedidos.adapter.input.serviceorder;

public record ProgressRequest(
        String action,
        String additionalInfo,
        Long relatedServiceId
) {
}
