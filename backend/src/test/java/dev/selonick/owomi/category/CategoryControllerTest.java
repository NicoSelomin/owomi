package dev.selonick.owomi.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.selonick.owomi.auth.CustomUserDetails;
import dev.selonick.owomi.auth.CustomUserDetailsService;
import dev.selonick.owomi.auth.JwtService;
import dev.selonick.owomi.category.dto.CategoryRequest;
import dev.selonick.owomi.category.dto.CategoryResponse;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.common.exception.GlobalExceptionHandler;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

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
    @DisplayName("GET /api/categories : retourne les catégories accessibles")
    void findAll_ShouldReturnCategoriesApiResponse() throws Exception {
        User user = authenticate();
        when(categoryService.findAll(user.getId(), null)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Transport"))
                .andExpect(jsonPath("$.data[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.data[0].isDefault").value(false))
                .andExpect(jsonPath("$.message").value("Catégories récupérées."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/categories : filtre par type")
    void findAll_WithType_ShouldDelegateTypeFilter() throws Exception {
        User user = authenticate();
        when(categoryService.findAll(user.getId(), TransactionType.EXPENSE)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/categories").param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value("EXPENSE"));

        verify(categoryService).findAll(user.getId(), TransactionType.EXPENSE);
    }

    @Test
    @DisplayName("GET /api/categories : type invalide → VALIDATION_ERROR")
    void findAll_InvalidType_ShouldReturnValidationError() throws Exception {
        authenticate();

        mockMvc.perform(get("/api/categories").param("type", "BAD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Données invalides."));
    }

    @Test
    @DisplayName("GET /api/categories/{id} : retourne une catégorie")
    void findById_ShouldReturnCategoryApiResponse() throws Exception {
        User user = authenticate();
        when(categoryService.findById(1L, user.getId())).thenReturn(response());

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Transport"))
                .andExpect(jsonPath("$.message").value("Catégorie récupérée."));
    }

    @Test
    @DisplayName("POST /api/categories : crée une catégorie")
    void create_ValidRequest_ShouldReturnCreatedApiResponse() throws Exception {
        User user = authenticate();
        CategoryRequest request = request();
        when(categoryService.create(request, user)).thenReturn(response());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Transport"))
                .andExpect(jsonPath("$.message").value("Catégorie créée avec succès."));
    }

    @Test
    @DisplayName("POST /api/categories : requête invalide → VALIDATION_ERROR")
    void create_InvalidRequest_ShouldReturnValidationError() throws Exception {
        authenticate();
        CategoryRequest request = new CategoryRequest("", "", "blue", null);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details").isArray());
    }

    @Test
    @DisplayName("POST /api/categories : doublon métier → VALIDATION_ERROR")
    void create_DuplicateName_ShouldReturnBusinessError() throws Exception {
        User user = authenticate();
        CategoryRequest request = request();
        when(categoryService.create(request, user))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Une catégorie avec ce nom existe déjà."));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Une catégorie avec ce nom existe déjà."));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} : met à jour une catégorie")
    void update_ValidRequest_ShouldReturnOkApiResponse() throws Exception {
        User user = authenticate();
        CategoryRequest request = request();
        when(categoryService.update(1L, request, user.getId())).thenReturn(response());

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Catégorie mise à jour avec succès."));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} : catégorie par défaut → CATEGORY_IS_DEFAULT")
    void update_DefaultCategory_ShouldReturnBusinessError() throws Exception {
        User user = authenticate();
        CategoryRequest request = request();
        when(categoryService.update(1L, request, user.getId()))
                .thenThrow(new BusinessException(ErrorCode.CATEGORY_IS_DEFAULT));

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_IS_DEFAULT"));
    }

    @Test
    @DisplayName("GET /api/categories/{id} : catégorie introuvable → RESOURCE_NOT_FOUND")
    void findById_NotFound_ShouldReturnBusinessError() throws Exception {
        User user = authenticate();
        when(categoryService.findById(99L, user.getId()))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} : supprime une catégorie")
    void delete_ValidRequest_ShouldReturnOkApiResponse() throws Exception {
        User user = authenticate();

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Catégorie supprimée avec succès."));

        verify(categoryService).delete(1L, user.getId());
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} : catégorie utilisée → CATEGORY_HAS_TRANSACTIONS")
    void delete_UsedCategory_ShouldReturnBusinessError() throws Exception {
        User user = authenticate();
        doThrow(new BusinessException(ErrorCode.CATEGORY_HAS_TRANSACTIONS))
                .when(categoryService).delete(1L, user.getId());

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CATEGORY_HAS_TRANSACTIONS"));
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

    private CategoryRequest request() {
        return new CategoryRequest("Transport", "car-outline", "#654321", TransactionType.EXPENSE);
    }

    private CategoryResponse response() {
        return new CategoryResponse(1L, "Transport", "car-outline", "#654321", TransactionType.EXPENSE, false);
    }
}
