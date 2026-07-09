package dev.selonick.owomi.category;

import dev.selonick.owomi.common.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMappingTest {

    @Test
    @DisplayName("Category : mappe la table categories avec unicité et index utilisateur")
    void category_ShouldMapTableConstraintsAndIndexes() {
        Table table = Category.class.getAnnotation(Table.class);

        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("categories");
        assertThat(table.uniqueConstraints()).singleElement()
                .satisfies(constraint -> {
                    assertThat(constraint.name()).isEqualTo("uq_category_name_user");
                    assertThat(constraint.columnNames()).containsExactly("name", "user_id");
                });
        assertThat(table.indexes()).singleElement()
                .satisfies(index -> {
                    assertThat(index.name()).isEqualTo("idx_categories_user");
                    assertThat(index.columnList()).isEqualTo("user_id");
                });
    }

    @Test
    @DisplayName("Category : mappe TransactionType en EnumType.STRING")
    void category_ShouldMapTypeAsStringEnum() throws NoSuchFieldException {
        Field type = Category.class.getDeclaredField("type");
        Enumerated enumerated = type.getAnnotation(Enumerated.class);
        Column column = type.getAnnotation(Column.class);

        assertThat(type.getType()).isEqualTo(TransactionType.class);
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(column.length()).isEqualTo(10);
        assertThat(column.nullable()).isFalse();
    }

    @Test
    @DisplayName("Category : mappe isDefault sur la colonne is_default")
    void category_ShouldMapIsDefaultColumn() throws NoSuchFieldException {
        Field isDefault = Category.class.getDeclaredField("isDefault");
        Column column = isDefault.getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo("is_default");
        assertThat(column.nullable()).isFalse();
    }

    @Test
    @DisplayName("Category : conserve une relation utilisateur nullable")
    void category_ShouldMapNullableUserRelation() throws NoSuchFieldException {
        Field user = Category.class.getDeclaredField("user");
        ManyToOne manyToOne = user.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = user.getAnnotation(JoinColumn.class);

        assertThat(manyToOne).isNotNull();
        assertThat(joinColumn.name()).isEqualTo("user_id");
        assertThat(joinColumn.nullable()).isTrue();
    }
}
