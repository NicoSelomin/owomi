package dev.selonick.owomi.transaction;

import dev.selonick.owomi.category.CategoryMapper;
import dev.selonick.owomi.transaction.dto.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapping des transactions vers leur représentation API.
 */
@Mapper(componentModel = "spring", uses = CategoryMapper.class)
public interface TransactionMapper {

    @Mapping(target = "category", source = "category")
    TransactionResponse toResponse(Transaction transaction);
}
