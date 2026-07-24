package dev.selonick.owomi.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Synthèse financière d'une période pour l'utilisateur authentifié.
 */
public record DashboardSummaryResponse(
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal balance,
        long transactionCount,
        LocalDate startDate,
        LocalDate endDate
) {
}
