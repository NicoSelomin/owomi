package dev.selonick.owomi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Demande de renvoi de l'email de vérification. Validée côté serveur.
 */
public record ResendVerificationRequest(

        @NotBlank(message = "L'email est obligatoire.")
        @Email(message = "Format d'email invalide.")
        @Size(max = 255, message = "L'email ne peut pas dépasser 255 caractères.")
        String email
) {
}
