package dev.selonick.owomi.category;

import dev.selonick.owomi.category.dto.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapping des catégories vers leur représentation API.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "isDefault", expression = "java(category.isDefault())")
    CategoryResponse toResponse(Category category);
}
