package dev.selonick.owomi.auth;

import dev.selonick.owomi.user.Role;
import dev.selonick.owomi.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Test
    @DisplayName("generateAccessToken : crée un token valide contenant l'email")
    void generateAccessToken_ShouldCreateValidToken() {
        JwtService jwtService = new JwtService(VALID_SECRET, 3_600_000, 604_800_000);
        User user = buildUser("test@owomi.dev");

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@owomi.dev");
        assertThat(jwtService.isTokenValid(token, new CustomUserDetails(user))).isTrue();
    }

    @Test
    @DisplayName("generateRefreshToken : applique une expiration plus longue que l'access token")
    void generateRefreshToken_ShouldUseRefreshExpiration() {
        JwtService jwtService = new JwtService(VALID_SECRET, 1_000, 10_000);
        User user = buildUser("test@owomi.dev");

        Instant accessExpiration = jwtService.extractExpiration(jwtService.generateAccessToken(user));
        Instant refreshExpiration = jwtService.extractExpiration(jwtService.generateRefreshToken(user));

        assertThat(refreshExpiration).isAfter(accessExpiration);
    }

    @Test
    @DisplayName("isTokenValid : refuse un token appartenant à un autre utilisateur")
    void isTokenValid_WithDifferentUsername_ShouldReturnFalse() {
        JwtService jwtService = new JwtService(VALID_SECRET, 3_600_000, 604_800_000);
        String token = jwtService.generateAccessToken(buildUser("test@owomi.dev"));

        assertThat(jwtService.isTokenValid(token, new CustomUserDetails(buildUser("other@owomi.dev"))))
                .isFalse();
    }

    @Test
    @DisplayName("isTokenValid : refuse un token expiré")
    void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
        JwtService jwtService = new JwtService(VALID_SECRET, -1_000, 604_800_000);
        User user = buildUser("test@owomi.dev");
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token, new CustomUserDetails(user))).isFalse();
    }

    @Test
    @DisplayName("isTokenValid : refuse un token malformé")
    void isTokenValid_WithMalformedToken_ShouldReturnFalse() {
        JwtService jwtService = new JwtService(VALID_SECRET, 3_600_000, 604_800_000);

        assertThat(jwtService.isTokenValid("not-a-jwt", new CustomUserDetails(buildUser("test@owomi.dev"))))
                .isFalse();
    }

    @Test
    @DisplayName("constructor : refuse un secret JWT vide")
    void constructor_WithBlankSecret_ShouldThrow() {
        assertThatThrownBy(() -> new JwtService(" ", 3_600_000, 604_800_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT secret must be provided.");
    }

    @Test
    @DisplayName("constructor : refuse un secret JWT non Base64")
    void constructor_WithInvalidBase64Secret_ShouldThrow() {
        assertThatThrownBy(() -> new JwtService("not base64 !", 3_600_000, 604_800_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT secret must be a valid Base64 value.");
    }

    @Test
    @DisplayName("constructor : refuse un secret JWT inférieur à 256 bits")
    void constructor_WithTooShortSecret_ShouldThrow() {
        String shortSecret = Base64.getEncoder()
                .encodeToString("short-secret".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new JwtService(shortSecret, 3_600_000, 604_800_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT secret must be at least 256 bits.");
    }

    private User buildUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword("$2a$12$hashedpassword");
        user.setRole(Role.USER);
        return user;
    }
}
