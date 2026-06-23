package dev.selonick.owomi.currency.dto;

/**
 * Représentation publique d'une devise.
 */
public record CurrencyDTO(
        String code,
        String name,
        String symbol,
        String locale
) {
}
