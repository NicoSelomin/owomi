package dev.selonick.owomi.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    /**
     * Charge l'utilisateur avec sa devise (JOIN FETCH) afin d'éviter toute
     * LazyInitializationException lorsque l'entité est utilisée hors transaction
     * (ex : mapping du principal authentifié dans un contrôleur).
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.currency WHERE u.email = :email")
    Optional<User> findByEmailWithCurrency(@Param("email") String email);

    boolean existsByEmail(String email);
}
