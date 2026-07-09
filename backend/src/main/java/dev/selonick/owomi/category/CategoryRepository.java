package dev.selonick.owomi.category;

import dev.selonick.owomi.common.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
        SELECT c FROM Category c
        WHERE c.user IS NULL OR c.user.id = :userId
        ORDER BY c.isDefault DESC, c.name ASC
        """)
    List<Category> findAccessibleByUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT c FROM Category c
        WHERE c.type = :type
        AND (c.user IS NULL OR c.user.id = :userId)
        ORDER BY c.isDefault DESC, c.name ASC
        """)
    List<Category> findAccessibleByUserIdAndType(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type
    );

    @Query("""
        SELECT c FROM Category c
        WHERE c.id = :id
        AND (c.user IS NULL OR c.user.id = :userId)
        """)
    Optional<Category> findAccessibleById(
            @Param("id") Long id,
            @Param("userId") UUID userId
    );

    @Query("""
        SELECT COUNT(c) > 0 FROM Category c
        WHERE LOWER(c.name) = LOWER(:name)
        AND (
            (:userId IS NULL AND c.user IS NULL)
            OR c.user.id = :userId
        )
        """)
    boolean existsByNameForOwner(
            @Param("name") String name,
            @Param("userId") UUID userId
    );

    @Query("""
        SELECT COUNT(c) > 0 FROM Category c
        WHERE LOWER(c.name) = LOWER(:name)
        AND c.id <> :excludedId
        AND c.user.id = :userId
        """)
    boolean existsByNameForOwnerExcludingId(
            @Param("name") String name,
            @Param("userId") UUID userId,
            @Param("excludedId") Long excludedId
    );

    @Query("""
        SELECT COUNT(c) > 0 FROM Category c
        WHERE c.id = :id
        AND c.user.id = :userId
        """)
    boolean existsByIdAndOwner(
            @Param("id") Long id,
            @Param("userId") UUID userId
    );

    boolean existsByIdAndIsDefaultTrue(Long id);
}
