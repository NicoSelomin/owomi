package dev.selonick.owomi.transaction.dto;

import dev.selonick.owomi.common.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête de création ou modification d'une transaction.
 */
public record TransactionRequest(
        @NotNull(message = "Le montant est obligatoire.")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0.")
        @Digits(integer = 13, fraction = 2, message = "Le montant doit respecter le format 9999999999999.99.")
        BigDecimal amount,

        @NotNull(message = "Le type est obligatoire.")
        TransactionType type,

        @NotNull(message = "La catégorie est obligatoire.")
        Long categoryId,

        @NotNull(message = "La date est obligatoire.")
        LocalDate date,

        @Size(max = 1000, message = "La note ne peut pas dépasser 1000 caractères.")
        String note
) {
}
