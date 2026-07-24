package dev.selonick.owomi.transaction.dto;

import java.util.List;

/**
 * Page stable de transactions, sans exposer le type Spring Data Page.
 */
public record TransactionPageResponse(
        List<TransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
