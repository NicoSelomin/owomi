package dev.selonick.owomi.category;

import dev.selonick.owomi.auth.CustomUserDetails;
import dev.selonick.owomi.category.dto.CategoryRequest;
import dev.selonick.owomi.category.dto.CategoryResponse;
import dev.selonick.owomi.common.api.ApiResponse;
import dev.selonick.owomi.common.enums.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * Endpoints catégories protégés par JWT.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Catégories", description = "Gestion des catégories de transactions")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Liste des catégories accessibles")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findAll(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) TransactionType type) {
        List<CategoryResponse> categories = categoryService.findAll(principal.getId(), type);
        return ResponseEntity.ok(ApiResponse.success(categories, "Catégories récupérées."));
    }

    @Operation(summary = "Détail d'une catégorie accessible")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> findById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        CategoryResponse category = categoryService.findById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(category, "Catégorie récupérée."));
    }

    @Operation(summary = "Création d'une catégorie personnelle")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.create(request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Catégorie créée avec succès."));
    }

    @Operation(summary = "Mise à jour d'une catégorie personnelle")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.update(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(category, "Catégorie mise à jour avec succès."));
    }

    @Operation(summary = "Suppression d'une catégorie personnelle")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        categoryService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Catégorie supprimée avec succès."));
    }
}
