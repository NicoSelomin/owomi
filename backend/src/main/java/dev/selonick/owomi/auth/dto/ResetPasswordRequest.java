package dev.selonick.owomi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Application d'un nouveau mot de passe à partir d'un token de réinitialisation.
 * La force du mot de passe est validée comme à l'inscription (source de vérité serveur).
 */
public record ResetPasswordRequest(

        @NotBlank(message = "Le jeton est obligatoire.")
        String token,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir au moins 8 caractères.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir une majuscule, une minuscule et un chiffre."
        )
        String newPassword
) {
}
