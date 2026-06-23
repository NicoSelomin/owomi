package dev.selonick.owomi.user;

import dev.selonick.owomi.currency.Currency;
import dev.selonick.owomi.currency.dto.CurrencyDTO;
import dev.selonick.owomi.user.dto.UserDTO;
import org.mapstruct.Mapper;

/**
 * Mapping entités -> DTO (MapStruct, intégration Spring).
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(User user);

    CurrencyDTO toDto(Currency currency);
}
