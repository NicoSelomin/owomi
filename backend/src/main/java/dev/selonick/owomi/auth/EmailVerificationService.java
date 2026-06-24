package dev.selonick.owomi.auth;

import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.user.User;
import dev.selonick.owomi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vérification d'adresse email : génération du token, envoi de l'email, confirmation, renvoi.
 * Token UUID v4 (SecureRandom via UUID.randomUUID), à usage unique, valable 24h.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    /** Durée de validité d'un token de vérification. */
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);

    /** Intervalle minimal entre deux renvois d'email pour une même adresse (anti-spam). */
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /** Dernier renvoi par email (clé en minuscules) — limitation de débit en mémoire. */
    private final Map<String, Instant> lastResendByEmail = new ConcurrentHashMap<>();

    /** Génère un token de vérification pour l'utilisateur et envoie l'email correspondant. */
    @Transactional
    public void createAndSend(User user) {
        issueAndSend(user);
        log.info("Verification email queued for user: id={}", user.getId());
    }

    /**
     * Renvoie un email de vérification pour l'adresse fournie.
     * Ne révèle jamais si le compte existe ou non (réponse générique côté contrôleur).
     * N'envoie rien si l'email est déjà vérifié ou si le délai anti-spam (1 min) n'est pas écoulé.
     */
    @Transactional
    public void resend(String email) {
        userRepository.findByEmail(email).ifPresentOrElse(
                user -> {
                    if (user.isEmailVerified()) {
                        log.info("Resend verification skipped (already verified): id={}", user.getId());
                        return;
                    }
                    if (isRateLimited(email)) {
                        log.info("Resend verification rate-limited for an email");
                        return;
                    }
                    // L'ancien token est invalidé avant d'en émettre un nouveau (usage unique)
                    tokenRepository.invalidateAllForUser(user);
                    issueAndSend(user);
                    lastResendByEmail.put(normalize(email), Instant.now());
                    log.info("Verification email resent for user: id={}", user.getId());
                },
                () -> log.info("Resend verification requested for unknown email")
        );
    }

    /** Persiste un nouveau token de vérification et déclenche l'envoi de l'email. */
    private void issueAndSend(User user) {
        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(token);
        verificationToken.setExpiresAt(Instant.now().plus(TOKEN_VALIDITY));
        verificationToken.setUsed(false);
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user, token);
    }

    /** Vrai si un renvoi a déjà eu lieu pour cette adresse depuis moins d'une minute. */
    private boolean isRateLimited(String email) {
        Instant last = lastResendByEmail.get(normalize(email));
        return last != null && last.isAfter(Instant.now().minus(RESEND_COOLDOWN));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    /**
     * Confirme une adresse email à partir d'un token.
     * Vérifie l'existence, l'usage unique et l'expiration côté serveur.
     */
    @Transactional
    public void verify(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_TOKEN_INVALID));

        if (verificationToken.isUsed()) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOKEN_INVALID);
        }
        if (verificationToken.isExpired()) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOKEN_EXPIRED);
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        verificationToken.setUsed(true);
        userRepository.save(user);
        tokenRepository.save(verificationToken);

        log.info("Email verified for user: id={}", user.getId());
    }
}
