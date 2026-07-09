package dev.selonick.owomi.transaction;

import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.transaction.projection.CategoryExpenseSummary;
import dev.selonick.owomi.transaction.projection.MonthlyBalanceSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
        SELECT t FROM Transaction t
        JOIN FETCH t.category
        WHERE t.id = :id
        AND t.user.id = :userId
        """)
    Optional<Transaction> findByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") UUID userId
    );

    @Query("""
        SELECT t FROM Transaction t
        JOIN FETCH t.category
        WHERE t.user.id = :userId
        AND (:type IS NULL OR t.type = :type)
        AND (:categoryId IS NULL OR t.category.id = :categoryId)
        AND (:startDate IS NULL OR t.date >= :startDate)
        AND (:endDate IS NULL OR t.date <= :endDate)
        ORDER BY t.date DESC, t.createdAt DESC
        """)
    Page<Transaction> findByUserAndFilters(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(t) > 0 FROM Transaction t
        WHERE t.id = :id
        AND t.user.id = :userId
        """)
    boolean existsByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") UUID userId
    );

    @Query("""
        SELECT COUNT(t) > 0 FROM Transaction t
        WHERE t.category.id = :categoryId
        AND t.user.id = :userId
        """)
    boolean existsByCategoryIdAndUserId(
            @Param("categoryId") Long categoryId,
            @Param("userId") UUID userId
    );

    long countByCategoryId(Long categoryId);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
        AND t.type = :type
        AND t.date BETWEEN :startDate AND :endDate
        """)
    BigDecimal sumAmountByUserAndTypeAndPeriod(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN t.type = dev.selonick.owomi.common.enums.TransactionType.INCOME THEN t.amount
                ELSE -t.amount
            END
        ), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
        AND t.date BETWEEN :startDate AND :endDate
        """)
    BigDecimal calculateBalanceByUserAndPeriod(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.user.id = :userId
        AND t.date BETWEEN :startDate AND :endDate
        """)
    long countByUserIdAndPeriod(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT
            c.id AS categoryId,
            c.name AS categoryName,
            c.color AS categoryColor,
            COALESCE(SUM(t.amount), 0) AS totalAmount,
            COUNT(t) AS transactionCount
        FROM Transaction t
        JOIN t.category c
        WHERE t.user.id = :userId
        AND t.type = dev.selonick.owomi.common.enums.TransactionType.EXPENSE
        AND t.date BETWEEN :startDate AND :endDate
        GROUP BY c.id, c.name, c.color
        ORDER BY COALESCE(SUM(t.amount), 0) DESC
        """)
    List<CategoryExpenseSummary> findExpenseTotalsByCategory(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT
            YEAR(t.date) AS year,
            MONTH(t.date) AS month,
            COALESCE(SUM(CASE
                WHEN t.type = dev.selonick.owomi.common.enums.TransactionType.INCOME THEN t.amount
                ELSE 0
            END), 0) AS incomeTotal,
            COALESCE(SUM(CASE
                WHEN t.type = dev.selonick.owomi.common.enums.TransactionType.EXPENSE THEN t.amount
                ELSE 0
            END), 0) AS expenseTotal,
            COALESCE(SUM(CASE
                WHEN t.type = dev.selonick.owomi.common.enums.TransactionType.INCOME THEN t.amount
                ELSE -t.amount
            END), 0) AS balance
        FROM Transaction t
        WHERE t.user.id = :userId
        AND t.date BETWEEN :startDate AND :endDate
        GROUP BY YEAR(t.date), MONTH(t.date)
        ORDER BY YEAR(t.date), MONTH(t.date)
        """)
    List<MonthlyBalanceSummary> findMonthlyBalance(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
