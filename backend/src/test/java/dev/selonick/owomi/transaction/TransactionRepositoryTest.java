package dev.selonick.owomi.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionRepositoryTest {

    @Test
    @DisplayName("findByUserAndFilters : pagination filtrée avec JOIN FETCH utilise un countQuery explicite")
    void findByUserAndFilters_ShouldDeclareExplicitCountQuery() throws NoSuchMethodException {
        Query query = findByUserAndFiltersQuery();

        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("JOIN FETCH t.category")
                .contains("t.user.id = :userId")
                .contains("t.type = COALESCE(:type, t.type)")
                .contains("t.category.id = COALESCE(:categoryId, t.category.id)")
                .contains("t.date >= COALESCE(:startDate, t.date)")
                .contains("t.date <= COALESCE(:endDate, t.date)")
                .doesNotContain(":startDate IS NULL")
                .doesNotContain(":endDate IS NULL");
        assertThat(query.countQuery())
                .isNotBlank()
                .contains("SELECT COUNT(t) FROM Transaction t")
                .contains("t.user.id = :userId")
                .contains("t.type = COALESCE(:type, t.type)")
                .contains("t.category.id = COALESCE(:categoryId, t.category.id)")
                .contains("t.date >= COALESCE(:startDate, t.date)")
                .contains("t.date <= COALESCE(:endDate, t.date)")
                .doesNotContain(":startDate IS NULL")
                .doesNotContain(":endDate IS NULL")
                .doesNotContain("FETCH");
    }

    @Test
    @DisplayName("findByUserAndFilters : aucun filtre optionnel garde le filtre utilisateur obligatoire")
    void findByUserAndFilters_WithoutOptionalFilters_ShouldStillFilterByUserId() throws NoSuchMethodException {
        Query query = findByUserAndFiltersQuery();

        assertThat(query.value())
                .contains("WHERE t.user.id = :userId")
                .contains("COALESCE(:type, t.type)")
                .contains("COALESCE(:categoryId, t.category.id)")
                .contains("COALESCE(:startDate, t.date)")
                .contains("COALESCE(:endDate, t.date)");
    }

    @Test
    @DisplayName("findByUserAndFilters : dates valides restent paramétrées")
    void findByUserAndFilters_WithDateFilters_ShouldUseParameterizedDates() throws NoSuchMethodException {
        Query query = findByUserAndFiltersQuery();

        assertThat(query.value())
                .contains("t.date >= COALESCE(:startDate, t.date)")
                .contains("t.date <= COALESCE(:endDate, t.date)")
                .doesNotContain(":startDate IS NULL")
                .doesNotContain(":endDate IS NULL");
    }

    @Test
    @DisplayName("findByUserAndFilters : type reste paramétré")
    void findByUserAndFilters_WithTypeFilter_ShouldUseParameterizedType() throws NoSuchMethodException {
        Query query = findByUserAndFiltersQuery();

        assertThat(query.value())
                .contains("t.type = COALESCE(:type, t.type)")
                .doesNotContain("type = '")
                .doesNotContain("type='");
    }

    @Test
    @DisplayName("findByUserAndFilters : categoryId reste paramétré")
    void findByUserAndFilters_WithCategoryFilter_ShouldUseParameterizedCategoryId() throws NoSuchMethodException {
        Query query = findByUserAndFiltersQuery();

        assertThat(query.value())
                .contains("t.category.id = COALESCE(:categoryId, t.category.id)")
                .doesNotContain("category_id = ")
                .doesNotContain("category.id = '");
    }

    @Test
    @DisplayName("findByUserAndFilters : combinaison type, catégorie et dates reste filtrée par utilisateur")
    void findByUserAndFilters_WithCombinedFilters_ShouldKeepAllPredicatesAndUserIsolation() throws NoSuchMethodException {
        Query query = findByUserAndFiltersQuery();

        assertThat(query.value())
                .containsSubsequence(
                        "WHERE t.user.id = :userId",
                        "AND t.type = COALESCE(:type, t.type)",
                        "AND t.category.id = COALESCE(:categoryId, t.category.id)",
                        "AND t.date >= COALESCE(:startDate, t.date)",
                        "AND t.date <= COALESCE(:endDate, t.date)"
                );
    }

    @Test
    @DisplayName("findByUserAndFilters : isolation entre utilisateurs imposée par userId")
    void findByUserAndFilters_ShouldIsolateTwoUsersWithMandatoryUserIdPredicate() throws NoSuchMethodException {
        Query query = findByUserAndFiltersQuery();

        assertThat(query.value())
                .contains("t.user.id = :userId")
                .doesNotContain("OR t.user.id")
                .doesNotContain("t.user.id = COALESCE");
        assertThat(query.countQuery())
                .contains("t.user.id = :userId")
                .doesNotContain("OR t.user.id")
                .doesNotContain("t.user.id = COALESCE");
    }

    @Test
    @DisplayName("findByUserAndFilters : requête statique sans concaténation SQL")
    void findByUserAndFilters_ShouldUseStaticParameterizedQuery() throws NoSuchMethodException {
        Query query = findByUserAndFiltersQuery();

        assertThat(query.value())
                .doesNotContain("${")
                .doesNotContain("||")
                .doesNotContain("concat(");
        assertThat(query.countQuery())
                .doesNotContain("${")
                .doesNotContain("||")
                .doesNotContain("concat(");
    }

    private Query findByUserAndFiltersQuery() throws NoSuchMethodException {
        Method method = TransactionRepository.class.getDeclaredMethod(
                "findByUserAndFilters",
                UUID.class,
                dev.selonick.owomi.common.enums.TransactionType.class,
                Long.class,
                LocalDate.class,
                LocalDate.class,
                org.springframework.data.domain.Pageable.class
        );

        return method.getAnnotation(Query.class);
    }
}
