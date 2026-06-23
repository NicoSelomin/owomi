package dev.selonick.owomi.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de santé public : permet de vérifier que l'application répond.
 * Renvoie volontairement une charge utile simple (non enveloppée dans ApiResponse).
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Vérification de l'état de l'application")
public class HealthController {

    @Value("${app.version:1.0.0}")
    private String version;

    @Operation(summary = "État de l'application")
    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", "OWOMI", version);
    }

    /** Réponse de santé : l'ordre des champs reflète le contrat {status, app, version}. */
    public record HealthResponse(String status, String app, String version) {
    }
}
