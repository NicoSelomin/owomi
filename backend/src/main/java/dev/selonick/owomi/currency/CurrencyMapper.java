package dev.selonick.owomi.currency;

import dev.selonick.owomi.currency.dto.CurrencyDTO;
import org.mapstruct.Mapper;

/**
 * Mapping des devises vers leur représentation API publique.
 */
@Mapper(componentModel = "spring")
public interface CurrencyMapper {

    CurrencyDTO toDto(Currency currency);
}
