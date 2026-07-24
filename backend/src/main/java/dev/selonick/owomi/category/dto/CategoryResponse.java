package dev.selonick.owomi.category.dto;

import dev.selonick.owomi.common.enums.TransactionType;

/**
 * Représentation publique d'une catégorie.
 */
public record CategoryResponse(
        Long id,
        String name,
        String icon,
        String color,
        TransactionType type,
        boolean isDefault
) {
}
