package dev.selonick.owomi.user;

import dev.selonick.owomi.auth.CustomUserDetails;
import dev.selonick.owomi.common.response.ApiResponse;
import dev.selonick.owomi.user.dto.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints utilisateur (protégés).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateur", description = "Profil de l'utilisateur authentifié")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserMapper userMapper;

    @Operation(summary = "Profil de l'utilisateur authentifié")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> me(
            @AuthenticationPrincipal CustomUserDetails principal) {
        UserDTO dto = userMapper.toDto(principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(dto, "Profil récupéré."));
    }
}
