package dev.selonick.owomi.transaction.export;

import dev.selonick.owomi.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Projection stable utilisée uniquement pour l'export CSV des transactions.
 */
public record TransactionExportRow(
        LocalDate date,
        TransactionType type,
        String categoryName,
        BigDecimal amount,
        String currencyCode,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
