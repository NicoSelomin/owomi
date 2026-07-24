package dev.selonick.owomi.config;

import dev.selonick.owomi.auth.CustomUserDetailsService;
import dev.selonick.owomi.auth.JwtAuthenticationFilter;
import dev.selonick.owomi.auth.JwtService;
import dev.selonick.owomi.category.CategoryController;
import dev.selonick.owomi.category.CategoryService;
import dev.selonick.owomi.common.HealthController;
import dev.selonick.owomi.currency.CurrencyController;
import dev.selonick.owomi.currency.CurrencyService;
import dev.selonick.owomi.dashboard.DashboardController;
import dev.selonick.owomi.dashboard.DashboardService;
import dev.selonick.owomi.transaction.TransactionController;
import dev.selonick.owomi.transaction.TransactionService;
import dev.selonick.owomi.transaction.export.TransactionExportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        HealthController.class,
        CurrencyController.class,
        CategoryController.class,
        TransactionController.class,
        DashboardController.class
})
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private CurrencyService currencyService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private TransactionExportService transactionExportService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @DisplayName("PasswordEncoder : utilise BCrypt")
    void passwordEncoder_ShouldUseBcrypt() {
        String encoded = passwordEncoder.encode("Password1!");

        org.assertj.core.api.Assertions.assertThat(encoded).startsWith("$2");
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("Password1!", encoded))
                .isTrue();
    }

    @Test
    @DisplayName("SecurityConfig : laisse l'endpoint health public et ajoute les headers de sécurité")
    void publicEndpoint_ShouldReturnSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/health").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
    }

    @Test
    @DisplayName("SecurityConfig : protège les endpoints non publics avec une réponse JSON 401")
    void protectedEndpoint_WithoutToken_ShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/protected").secure(true))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("SecurityConfig : laisse /api/currencies public")
    void currenciesEndpoint_WithoutToken_ShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/currencies").secure(true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Devises récupérées."));
    }

    @Test
    @DisplayName("SecurityConfig : protège /api/categories sans token")
    void categoriesEndpoint_WithoutToken_ShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/categories").secure(true))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("SecurityConfig : protège /api/transactions sans token")
    void transactionsEndpoint_WithoutToken_ShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/transactions").secure(true))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("SecurityConfig : protège /api/transactions/export/csv sans token")
    void transactionsExportEndpoint_WithoutToken_ShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/transactions/export/csv").secure(true))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("SecurityConfig : protège /api/transactions/export/csv avec token invalide")
    void transactionsExportEndpoint_InvalidToken_ShouldReturnUnauthorizedJson() throws Exception {
        when(jwtService.extractEmail("invalid.jwt.token")).thenThrow(new IllegalArgumentException("invalid token"));

        mockMvc.perform(get("/api/transactions/export/csv")
                        .secure(true)
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("SecurityConfig : protège /api/dashboard/summary sans token")
    void dashboardSummaryEndpoint_WithoutToken_ShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").secure(true))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("SecurityConfig : protège /api/dashboard/monthly-balances sans token")
    void dashboardMonthlyBalancesEndpoint_WithoutToken_ShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/dashboard/monthly-balances").secure(true))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));
    }

    @Test
    @DisplayName("SecurityConfig : protège /api/dashboard/category-expenses sans token")
    void dashboardCategoryExpensesEndpoint_WithoutToken_ShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/dashboard/category-expenses").secure(true))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TOKEN_INVALID"));
    }
}
