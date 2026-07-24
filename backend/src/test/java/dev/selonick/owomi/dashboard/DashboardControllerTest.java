package dev.selonick.owomi.dashboard;

import dev.selonick.owomi.auth.CustomUserDetails;
import dev.selonick.owomi.auth.CustomUserDetailsService;
import dev.selonick.owomi.auth.JwtService;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.common.exception.GlobalExceptionHandler;
import dev.selonick.owomi.dashboard.dto.CategoryExpenseResponse;
import dev.selonick.owomi.dashboard.dto.DashboardSummaryResponse;
import dev.selonick.owomi.dashboard.dto.MonthlyBalanceResponse;
import dev.selonick.owomi.user.Role;
import dev.selonick.owomi.user.User;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

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
    @DisplayName("GET /api/dashboard/summary : retourne une ApiResponse uniforme")
    void summary_ShouldReturnApiResponse() throws Exception {
        User user = authenticate();
        DashboardSummaryResponse response = new DashboardSummaryResponse(
                new BigDecimal("1000.00"),
                new BigDecimal("250.00"),
                new BigDecimal("750.00"),
                4L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );
        when(dashboardService.getSummary(user.getId(), null, null)).thenReturn(response);

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.incomeTotal").value(1000.00))
                .andExpect(jsonPath("$.data.expenseTotal").value(250.00))
                .andExpect(jsonPath("$.data.balance").value(750.00))
                .andExpect(jsonPath("$.data.transactionCount").value(4))
                .andExpect(jsonPath("$.data.startDate").value("2026-01-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-01-31"))
                .andExpect(jsonPath("$.message").value("Synthèse du dashboard récupérée."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/dashboard/summary : dates valides transmises au service")
    void summary_WithDates_ShouldDelegateDates() throws Exception {
        User user = authenticate();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        when(dashboardService.getSummary(user.getId(), startDate, endDate))
                .thenReturn(new DashboardSummaryResponse(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0L,
                        startDate,
                        endDate
                ));

        mockMvc.perform(get("/api/dashboard/summary")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(dashboardService).getSummary(user.getId(), startDate, endDate);
    }

    @Test
    @DisplayName("GET /api/dashboard/summary : date invalide → VALIDATION_ERROR")
    void summary_InvalidDate_ShouldReturnValidationError() throws Exception {
        authenticate();

        mockMvc.perform(get("/api/dashboard/summary")
                        .param("startDate", "not-a-date")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/dashboard/summary : période métier invalide → VALIDATION_ERROR")
    void summary_InvalidBusinessPeriod_ShouldReturnValidationError() throws Exception {
        User user = authenticate();
        when(dashboardService.getSummary(user.getId(), LocalDate.of(2026, 1, 1), null))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "La date de début et la date de fin doivent être fournies ensemble."));

        mockMvc.perform(get("/api/dashboard/summary")
                        .param("startDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/dashboard/monthly-balances : retourne les balances mensuelles")
    void monthlyBalances_ShouldReturnApiResponse() throws Exception {
        User user = authenticate();
        when(dashboardService.getMonthlyBalances(user.getId(), null, null))
                .thenReturn(List.of(new MonthlyBalanceResponse(
                        2026,
                        1,
                        new BigDecimal("1000.00"),
                        new BigDecimal("250.00"),
                        new BigDecimal("750.00")
                )));

        mockMvc.perform(get("/api/dashboard/monthly-balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].year").value(2026))
                .andExpect(jsonPath("$.data[0].month").value(1))
                .andExpect(jsonPath("$.data[0].incomeTotal").value(1000.00))
                .andExpect(jsonPath("$.message").value("Balances mensuelles récupérées."));
    }

    @Test
    @DisplayName("GET /api/dashboard/category-expenses : retourne les dépenses par catégorie")
    void categoryExpenses_ShouldReturnApiResponse() throws Exception {
        User user = authenticate();
        when(dashboardService.getCategoryExpenses(user.getId(), null, null))
                .thenReturn(List.of(new CategoryExpenseResponse(
                        10L,
                        "Transport",
                        "#654321",
                        new BigDecimal("250.00"),
                        3L
                )));

        mockMvc.perform(get("/api/dashboard/category-expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].categoryId").value(10))
                .andExpect(jsonPath("$.data[0].categoryName").value("Transport"))
                .andExpect(jsonPath("$.data[0].categoryColor").value("#654321"))
                .andExpect(jsonPath("$.data[0].totalAmount").value(250.00))
                .andExpect(jsonPath("$.data[0].transactionCount").value(3))
                .andExpect(jsonPath("$.message").value("Dépenses par catégorie récupérées."));
    }

    @Test
    @DisplayName("GET /api/dashboard/summary : userId public ignoré, principal utilisé")
    void summary_WithUserIdParam_ShouldStillUsePrincipalUserId() throws Exception {
        User user = authenticate();
        UUID maliciousUserId = UUID.randomUUID();
        when(dashboardService.getSummary(user.getId(), null, null))
                .thenReturn(new DashboardSummaryResponse(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0L,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)
                ));

        mockMvc.perform(get("/api/dashboard/summary")
                        .param("userId", maliciousUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(dashboardService).getSummary(user.getId(), null, null);
    }

    private User authenticate() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Nicodème");
        user.setEmail("test@owomi.dev");
        user.setPassword("$2a$12$hashedpassword");
        user.setRole(Role.USER);
        user.setEmailVerified(true);

        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        ));
        return user;
    }
}
