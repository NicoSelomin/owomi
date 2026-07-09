package dev.selonick.owomi.auth;

import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("create : persiste un refresh token avec expiration")
    void create_ShouldPersistRefreshTokenWithExpiration() {
        User user = buildUser();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(jwtService.extractExpiration("refresh-token")).thenReturn(expiresAt);
        when(refreshTokenRepository.save(org.mockito.ArgumentMatchers.any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.create(user, "refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getToken()).isEqualTo("refresh-token");
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("verify : token actif → retourne le refresh token")
    void verify_ActiveToken_ShouldReturnRefreshToken() {
        RefreshToken token = buildToken(Instant.now().plusSeconds(3600), false);
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.verify("refresh-token");

        assertThat(result).isSameAs(token);
    }

    @Test
    @DisplayName("verify : token inconnu → BusinessException TOKEN_INVALID")
    void verify_UnknownToken_ShouldThrowTokenInvalid() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verify("unknown"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    @DisplayName("verify : token expiré → BusinessException TOKEN_EXPIRED")
    void verify_ExpiredToken_ShouldThrowTokenExpired() {
        RefreshToken token = buildToken(Instant.now().minusSeconds(1), false);
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.verify("refresh-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("verify : token révoqué → BusinessException TOKEN_EXPIRED")
    void verify_RevokedToken_ShouldThrowTokenExpired() {
        RefreshToken token = buildToken(Instant.now().plusSeconds(3600), true);
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.verify("refresh-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("logout : token connu → révoque tous les refresh tokens de l'utilisateur")
    void logout_KnownToken_ShouldRevokeAllForUser() {
        RefreshToken token = buildToken(Instant.now().plusSeconds(3600), false);
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(token));

        refreshTokenService.logout("refresh-token");

        verify(refreshTokenRepository).revokeAllByUser(token.getUser());
    }

    @Test
    @DisplayName("logout : token inconnu → ne fait rien")
    void logout_UnknownToken_ShouldDoNothing() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        refreshTokenService.logout("unknown");

        verify(refreshTokenRepository, never()).revokeAllByUser(org.mockito.ArgumentMatchers.any());
    }

    private RefreshToken buildToken(Instant expiresAt, boolean revoked) {
        RefreshToken token = new RefreshToken();
        token.setUser(buildUser());
        token.setToken("refresh-token");
        token.setExpiresAt(expiresAt);
        token.setRevoked(revoked);
        return token;
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Nicodème");
        user.setEmail("test@owomi.dev");
        user.setPassword("$2a$12$hashedpassword");
        return user;
    }
}
