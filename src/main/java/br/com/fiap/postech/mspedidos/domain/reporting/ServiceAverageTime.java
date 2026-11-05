package br.com.fiap.postech.mspedidos.domain.reporting;

public record ServiceAverageTime(
        Long id,
        String name,
        long totalCreated,
        long totalCompleted,
        double averageTimeBetweenCreateAndComplete,
        double averageTimeBetweenStartAndComplete,
        double averageTimeBetweenApproveAndComplete,
        double averageTimeAwaitingBudgetApproval
) {
}
