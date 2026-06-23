package dev.selonick.owomi.user.dto;

import dev.selonick.owomi.currency.dto.CurrencyDTO;

import java.util.UUID;

/**
 * Représentation publique d'un utilisateur (sans le hash du mot de passe).
 */
public record UserDTO(
        UUID id,
        String name,
        String email,
        CurrencyDTO currency
) {
}
