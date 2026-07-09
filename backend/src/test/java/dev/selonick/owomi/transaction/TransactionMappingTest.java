package dev.selonick.owomi.transaction;

import dev.selonick.owomi.category.Category;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMappingTest {

    @Test
    @DisplayName("Transaction : mappe la table transactions avec les index Flyway")
    void transaction_ShouldMapTableIndexes() {
        Table table = Transaction.class.getAnnotation(Table.class);

        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo("transactions");
        assertThat(table.indexes())
                .extracting(index -> index.name() + ":" + index.columnList())
                .containsExactlyInAnyOrder(
                        "idx_transactions_user:user_id",
                        "idx_transactions_user_date:user_id, date DESC",
                        "idx_transactions_category:category_id"
                );
    }

    @Test
    @DisplayName("Transaction : mappe amount en DECIMAL(15,2)")
    void transaction_ShouldMapAmountPrecisionAndScale() throws NoSuchFieldException {
        Field amount = Transaction.class.getDeclaredField("amount");
        Column column = amount.getAnnotation(Column.class);

        assertThat(amount.getType()).isEqualTo(BigDecimal.class);
        assertThat(column.precision()).isEqualTo(15);
        assertThat(column.scale()).isEqualTo(2);
        assertThat(column.nullable()).isFalse();
    }

    @Test
    @DisplayName("Transaction : mappe la date en LocalDate")
    void transaction_ShouldMapDateAsLocalDate() throws NoSuchFieldException {
        Field date = Transaction.class.getDeclaredField("date");
        Column column = date.getAnnotation(Column.class);

        assertThat(date.getType()).isEqualTo(LocalDate.class);
        assertThat(column.nullable()).isFalse();
    }

    @Test
    @DisplayName("Transaction : mappe TransactionType en EnumType.STRING")
    void transaction_ShouldMapTypeAsStringEnum() throws NoSuchFieldException {
        Field type = Transaction.class.getDeclaredField("type");
        Enumerated enumerated = type.getAnnotation(Enumerated.class);
        Column column = type.getAnnotation(Column.class);

        assertThat(type.getType()).isEqualTo(TransactionType.class);
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(column.length()).isEqualTo(10);
        assertThat(column.nullable()).isFalse();
    }

    @Test
    @DisplayName("Transaction : mappe les relations obligatoires vers Category et User")
    void transaction_ShouldMapRequiredRelations() throws NoSuchFieldException {
        assertRequiredRelation("category", Category.class, "category_id");
        assertRequiredRelation("user", User.class, "user_id");
    }

    private void assertRequiredRelation(
            String fieldName,
            Class<?> expectedType,
            String expectedColumn
    ) throws NoSuchFieldException {
        Field field = Transaction.class.getDeclaredField(fieldName);
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

        assertThat(field.getType()).isEqualTo(expectedType);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo(expectedColumn);
        assertThat(joinColumn.nullable()).isFalse();
    }
}
