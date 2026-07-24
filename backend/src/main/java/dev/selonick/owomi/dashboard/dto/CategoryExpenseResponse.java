package dev.selonick.owomi.dashboard.dto;

import java.math.BigDecimal;

/**
 * Dépenses agrégées par catégorie.
 */
public record CategoryExpenseResponse(
        Long categoryId,
        String categoryName,
        String categoryColor,
        BigDecimal totalAmount,
        long transactionCount
) {
}
