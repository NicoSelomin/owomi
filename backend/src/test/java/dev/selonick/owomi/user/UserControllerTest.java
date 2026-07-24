package dev.selonick.owomi.user;

import dev.selonick.owomi.auth.CustomUserDetails;
import dev.selonick.owomi.auth.CustomUserDetailsService;
import dev.selonick.owomi.auth.JwtService;
import dev.selonick.owomi.common.exception.GlobalExceptionHandler;
import dev.selonick.owomi.currency.Currency;
import dev.selonick.owomi.currency.dto.CurrencyDTO;
import dev.selonick.owomi.user.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/users/me : retourne le profil authentifié sans mot de passe")
    void me_AuthenticatedUser_ShouldReturnProfileWithoutPassword() throws Exception {
        User user = buildUser();
        CustomUserDetails principal = new CustomUserDetails(user);
        UserDTO dto = new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                new CurrencyDTO("XOF", "Franc CFA UEMOA", "FCFA", "fr-BJ")
        );

        when(userMapper.toDto(user)).thenReturn(dto);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        ));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Nicodème"))
                .andExpect(jsonPath("$.data.email").value("test@owomi.dev"))
                .andExpect(jsonPath("$.data.currency.code").value("XOF"))
                .andExpect(jsonPath("$.data.currency.symbol").value("FCFA"))
                .andExpect(jsonPath("$.data", not(hasKey("password"))))
                .andExpect(jsonPath("$.message").value("Profil récupéré."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Nicodème");
        user.setEmail("test@owomi.dev");
        user.setPassword("$2a$12$hashedpassword");
        user.setRole(Role.USER);
        user.setCurrency(new Currency("XOF", "Franc CFA UEMOA", "FCFA", "fr-BJ"));
        user.setEmailVerified(true);
        return user;
    }
}
