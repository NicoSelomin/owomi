package dev.selonick.owomi.auth.dto;

import dev.selonick.owomi.user.dto.UserDTO;

/**
 * Réponse renvoyée après inscription / connexion / rafraîchissement.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserDTO user
) {
}
