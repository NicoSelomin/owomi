package dev.selonick.owomi.category.dto;

import dev.selonick.owomi.common.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Requête de création ou modification d'une catégorie personnelle.
 */
public record CategoryRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères.")
        String name,

        @NotBlank(message = "L'icône est obligatoire.")
        @Size(max = 50, message = "L'icône ne peut pas dépasser 50 caractères.")
        String icon,

        @NotBlank(message = "La couleur est obligatoire.")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "La couleur doit être au format hexadécimal #RRGGBB.")
        String color,

        @NotNull(message = "Le type est obligatoire.")
        TransactionType type
) {
}
