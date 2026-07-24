package dev.selonick.owomi.dashboard.dto;

import java.math.BigDecimal;

/**
 * Balance agrégée par mois.
 */
public record MonthlyBalanceResponse(
        int year,
        int month,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal balance
) {
}
