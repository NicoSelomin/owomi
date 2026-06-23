package dev.selonick.owomi.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de requête portant un refresh token (utilisé par /refresh et /logout).
 */
public record TokenRequest(

        @NotBlank(message = "Le refresh token est obligatoire.")
        String refreshToken
) {
}
