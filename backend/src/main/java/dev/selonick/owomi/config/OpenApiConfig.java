package dev.selonick.owomi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Métadonnées exposées par Swagger UI / OpenAPI + schéma de sécurité Bearer JWT.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI owomiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OWOMI API")
                        .description("API de l'application de gestion de budget personnel OWOMI.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("VODOUNNOU Nicodème (Selonick)")
                                .email("nicodeme@selonick.dev"))
                        .license(new License().name("Propriétaire")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token JWT — saisir uniquement le token (sans 'Bearer ').")));
    }
}
