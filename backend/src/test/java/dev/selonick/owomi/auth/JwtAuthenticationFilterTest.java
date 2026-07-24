package dev.selonick.owomi.auth;

import dev.selonick.owomi.user.Role;
import dev.selonick.owomi.user.User;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal : sans Authorization, laisse passer sans authentifier")
    void doFilterInternal_WithoutAuthorizationHeader_ShouldContinueWithoutAuthentication()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractEmail(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("doFilterInternal : avec header non Bearer, laisse passer sans authentifier")
    void doFilterInternal_WithNonBearerHeader_ShouldContinueWithoutAuthentication()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractEmail(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("doFilterInternal : token valide, authentifie le SecurityContext")
    void doFilterInternal_WithValidToken_ShouldAuthenticateSecurityContext()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        CustomUserDetails userDetails = new CustomUserDetails(buildUser());

        when(jwtService.extractEmail("valid-token")).thenReturn("test@owomi.dev");
        when(userDetailsService.loadUserByUsername("test@owomi.dev")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isSameAs(userDetails);
    }

    @Test
    @DisplayName("doFilterInternal : token invalide, nettoie le SecurityContext")
    void doFilterInternal_WithInvalidToken_ShouldClearSecurityContext()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("previous", null));

        when(jwtService.extractEmail("invalid-token")).thenThrow(new IllegalArgumentException("Invalid token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("doFilterInternal : contexte déjà authentifié, ne recharge pas l'utilisateur")
    void doFilterInternal_WithExistingAuthentication_ShouldNotReloadUser()
            throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        TestingAuthenticationToken existingAuthentication =
                new TestingAuthenticationToken("existing", null);
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        when(jwtService.extractEmail("valid-token")).thenReturn("test@owomi.dev");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(existingAuthentication);
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setEmail("test@owomi.dev");
        user.setPassword("$2a$12$hashedpassword");
        user.setRole(Role.USER);
        return user;
    }
}
