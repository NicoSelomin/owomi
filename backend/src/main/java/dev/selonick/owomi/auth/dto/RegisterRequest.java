package dev.selonick.owomi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Données d'inscription. Validées côté serveur (source de vérité).
 * countryCode est collecté mais non persisté (pas de colonne dédiée en J2).
 */
public record RegisterRequest(

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères.")
        String name,

        @NotBlank(message = "L'email est obligatoire.")
        @Email(message = "Format d'email invalide.")
        @Size(max = 255, message = "L'email ne peut pas dépasser 255 caractères.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 100, message = "Le mot de passe doit contenir au moins 8 caractères.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir une majuscule, une minuscule et un chiffre."
        )
        String password,

        @NotBlank(message = "Le pays est obligatoire.")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Le code pays doit être au format ISO (2 lettres).")
        String countryCode,

        @NotBlank(message = "La devise est obligatoire.")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Le code devise doit être au format ISO (3 lettres).")
        String currencyCode
) {
}
