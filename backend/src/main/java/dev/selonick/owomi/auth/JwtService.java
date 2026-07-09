package dev.selonick.owomi.auth;

import dev.selonick.owomi.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

/**
 * Génération et validation des JSON Web Tokens.
 * Le secret est chargé depuis une variable d'environnement (jwt.secret) — jamais hardcodé.
 */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtService(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-expiration:3600000}") long accessExpiration,
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpiration) {
        this.signingKey = buildSigningKey(jwtSecret);
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(User user) {
        return buildToken(user, accessExpiration);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpiration);
    }

    private String buildToken(User user, long expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(signingKey)
                .compact();
    }

    /** Extrait l'email (subject) du token. Ne jamais logger le token lui-même. */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            // Signature invalide, token malformé ou expiré
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).isBefore(Instant.now());
    }

    private SecretKey buildSigningKey(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must be provided.");
        }

        byte[] decodedSecret;
        try {
            decodedSecret = Decoders.BASE64.decode(jwtSecret);
        } catch (DecodingException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("JWT secret must be a valid Base64 value.", ex);
        }

        if (decodedSecret.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits.");
        }

        return Keys.hmacShaKeyFor(decodedSecret);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
