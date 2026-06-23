package dev.selonick.owomi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Point d'entrée de l'application OWOMI.
 * L'audit JPA est activé pour alimenter automatiquement createdAt / updatedAt (cf. BaseEntity).
 */
@SpringBootApplication
@EnableJpaAuditing
public class OWOMIApplication {

    public static void main(String[] args) {
        SpringApplication.run(OWOMIApplication.class, args);
    }
}
