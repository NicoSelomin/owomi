package dev.selonick.owomi.transaction;

import dev.selonick.owomi.auth.CustomUserDetails;
import dev.selonick.owomi.common.api.ApiResponse;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.transaction.dto.TransactionPageResponse;
import dev.selonick.owomi.transaction.dto.TransactionRequest;
import dev.selonick.owomi.transaction.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoints transactions protégés par JWT.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Gestion des transactions financières")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Liste paginée des transactions de l'utilisateur")
    @GetMapping
    public ResponseEntity<ApiResponse<TransactionPageResponse>> findAll(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        TransactionPageResponse transactions = transactionService.findAll(
                principal.getId(),
                type,
                categoryId,
                startDate,
                endDate,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(transactions, "Transactions récupérées."));
    }

    @Operation(summary = "Détail d'une transaction")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> findById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        TransactionResponse transaction = transactionService.findById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(transaction, "Transaction récupérée."));
    }

    @Operation(summary = "Création d'une transaction")
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse transaction = transactionService.create(request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(transaction, "Transaction créée avec succès."));
    }

    @Operation(summary = "Mise à jour d'une transaction")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse transaction = transactionService.update(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(transaction, "Transaction mise à jour avec succès."));
    }

    @Operation(summary = "Suppression d'une transaction")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        transactionService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Transaction supprimée avec succès."));
    }
}
