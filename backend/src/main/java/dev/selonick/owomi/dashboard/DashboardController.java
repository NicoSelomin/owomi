package dev.selonick.owomi.dashboard;

import dev.selonick.owomi.auth.CustomUserDetails;
import dev.selonick.owomi.common.api.ApiResponse;
import dev.selonick.owomi.dashboard.dto.CategoryExpenseResponse;
import dev.selonick.owomi.dashboard.dto.DashboardSummaryResponse;
import dev.selonick.owomi.dashboard.dto.MonthlyBalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints dashboard protégés par JWT.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Statistiques financières de l'utilisateur authentifié")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Synthèse financière de la période")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        DashboardSummaryResponse summary = dashboardService.getSummary(principal.getId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary, "Synthèse du dashboard récupérée."));
    }

    @Operation(summary = "Balances mensuelles de la période")
    @GetMapping("/monthly-balances")
    public ResponseEntity<ApiResponse<List<MonthlyBalanceResponse>>> monthlyBalances(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<MonthlyBalanceResponse> balances =
                dashboardService.getMonthlyBalances(principal.getId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(balances, "Balances mensuelles récupérées."));
    }

    @Operation(summary = "Dépenses par catégorie de la période")
    @GetMapping("/category-expenses")
    public ResponseEntity<ApiResponse<List<CategoryExpenseResponse>>> categoryExpenses(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CategoryExpenseResponse> expenses =
                dashboardService.getCategoryExpenses(principal.getId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(expenses, "Dépenses par catégorie récupérées."));
    }
}
