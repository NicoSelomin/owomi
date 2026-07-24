package dev.selonick.owomi.transaction.dto;

import dev.selonick.owomi.category.dto.CategoryResponse;
import dev.selonick.owomi.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Représentation publique d'une transaction.
 */
public record TransactionResponse(
        Long id,
        BigDecimal amount,
        TransactionType type,
        String note,
        LocalDate date,
        CategoryResponse category
) {
}
