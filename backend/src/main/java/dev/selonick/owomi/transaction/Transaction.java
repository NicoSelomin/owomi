package dev.selonick.owomi.transaction;

import dev.selonick.owomi.category.Category;
import dev.selonick.owomi.common.entity.BaseEntity;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transaction financière appartenant à un utilisateur.
 */
@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_user", columnList = "user_id"),
                @Index(name = "idx_transactions_user_date", columnList = "user_id, date DESC"),
                @Index(name = "idx_transactions_category", columnList = "category_id")
        }
)
@Getter
@Setter
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
