package dev.selonick.owomi.auth;

import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cycle de vie des refresh tokens : persistance, vérification, révocation.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    /** Persiste un refresh token fraîchement généré pour un utilisateur. */
    @Transactional
    public RefreshToken create(User user, String token) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(jwtService.extractExpiration(token));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }

    /** Vérifie qu'un refresh token existe, n'est ni révoqué ni expiré. */
    @Transactional(readOnly = true)
    public RefreshToken verify(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID,
                        "Refresh token invalide."));

        if (!refreshToken.isActive()) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED,
                    "Refresh token expiré ou révoqué. Veuillez vous reconnecter.");
        }
        return refreshToken;
    }

    /** Révoque tous les refresh tokens actifs de l'utilisateur (déconnexion). */
    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    /**
     * Déconnexion : révoque tous les tokens de l'utilisateur associé au token fourni.
     * Idempotent — ne lève pas d'erreur si le token est inconnu.
     */
    @Transactional
    public void logout(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(rt -> refreshTokenRepository.revokeAllByUser(rt.getUser()));
    }
}
