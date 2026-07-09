package dev.selonick.owomi.category;

import dev.selonick.owomi.category.dto.CategoryRequest;
import dev.selonick.owomi.category.dto.CategoryResponse;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.transaction.TransactionRepository;
import dev.selonick.owomi.user.Role;
import dev.selonick.owomi.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("findAll : retourne toutes les catégories accessibles")
    void findAll_WithoutType_ShouldReturnAccessibleCategories() {
        UUID userId = UUID.randomUUID();
        Category category = buildCategory(1L, "Transport", TransactionType.EXPENSE, buildUser(userId), false);
        CategoryResponse response = toResponse(category);

        when(categoryRepository.findAccessibleByUserId(userId)).thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        List<CategoryResponse> result = categoryService.findAll(userId, null);

        assertThat(result).containsExactly(response);
        verify(categoryRepository).findAccessibleByUserId(userId);
    }

    @Test
    @DisplayName("findAll : filtre les catégories accessibles par type")
    void findAll_WithType_ShouldReturnAccessibleCategoriesByType() {
        UUID userId = UUID.randomUUID();
        Category category = buildCategory(2L, "Revenus", TransactionType.INCOME, null, true);
        CategoryResponse response = toResponse(category);

        when(categoryRepository.findAccessibleByUserIdAndType(userId, TransactionType.INCOME))
                .thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        List<CategoryResponse> result = categoryService.findAll(userId, TransactionType.INCOME);

        assertThat(result).containsExactly(response);
    }

    @Test
    @DisplayName("findById : catégorie accessible → retourne le DTO")
    void findById_AccessibleCategory_ShouldReturnResponse() {
        UUID userId = UUID.randomUUID();
        Category category = buildCategory(1L, "Transport", TransactionType.EXPENSE, buildUser(userId), false);
        CategoryResponse response = toResponse(category);

        when(categoryRepository.findAccessibleById(1L, userId)).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        assertThat(categoryService.findById(1L, userId)).isEqualTo(response);
    }

    @Test
    @DisplayName("create : crée une catégorie personnelle")
    void create_ValidRequest_ShouldCreateOwnedCategory() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        CategoryRequest request = new CategoryRequest(
                "  Voyage  ",
                "  airplane-outline  ",
                "#123ABC",
                TransactionType.EXPENSE
        );
        Category saved = buildCategory(10L, "Voyage", TransactionType.EXPENSE, user, false);
        CategoryResponse response = toResponse(saved);

        when(categoryRepository.existsByNameForOwner("Voyage", userId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(response);

        CategoryResponse result = categoryService.create(request, user);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        Category created = categoryCaptor.getValue();
        assertThat(created.getName()).isEqualTo("Voyage");
        assertThat(created.getIcon()).isEqualTo("airplane-outline");
        assertThat(created.getColor()).isEqualTo("#123ABC");
        assertThat(created.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(created.isDefault()).isFalse();
        assertThat(created.getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("create : doublon de nom → VALIDATION_ERROR")
    void create_DuplicateName_ShouldThrowValidationError() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        CategoryRequest request = request("Transport");

        when(categoryRepository.existsByNameForOwner("Transport", userId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request, user))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(businessException.getMessage()).isEqualTo("Une catégorie avec ce nom existe déjà.");
                });

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update : catégorie personnelle → met à jour les champs modifiables")
    void update_OwnedCategory_ShouldUpdateCategory() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        Category category = buildCategory(1L, "Transport", TransactionType.EXPENSE, user, false);
        CategoryResponse response = new CategoryResponse(
                1L,
                "Mobilité",
                "car-outline",
                "#654321",
                TransactionType.EXPENSE,
                false
        );

        when(categoryRepository.findAccessibleById(1L, userId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameForOwnerExcludingId("Mobilité", userId, 1L)).thenReturn(false);
        when(categoryMapper.toResponse(category)).thenReturn(response);

        CategoryResponse result = categoryService.update(1L, request("Mobilité"), userId);

        assertThat(result).isEqualTo(response);
        assertThat(category.getName()).isEqualTo("Mobilité");
        assertThat(category.getIcon()).isEqualTo("car-outline");
        assertThat(category.getColor()).isEqualTo("#654321");
        assertThat(category.getUser()).isSameAs(user);
        assertThat(category.isDefault()).isFalse();
    }

    @Test
    @DisplayName("update : catégorie introuvable ou non accessible → RESOURCE_NOT_FOUND")
    void update_NotAccessible_ShouldThrowNotFound() {
        UUID userId = UUID.randomUUID();

        when(categoryRepository.findAccessibleById(99L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L, request("Mobilité"), userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("update : catégorie par défaut → CATEGORY_IS_DEFAULT")
    void update_DefaultCategory_ShouldThrowCategoryIsDefault() {
        UUID userId = UUID.randomUUID();
        Category category = buildCategory(1L, "Transport", TransactionType.EXPENSE, null, true);

        when(categoryRepository.findAccessibleById(1L, userId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.update(1L, request("Mobilité"), userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_IS_DEFAULT);
    }

    @Test
    @DisplayName("update : doublon en excluant l'id courant → VALIDATION_ERROR")
    void update_DuplicateName_ShouldThrowValidationError() {
        UUID userId = UUID.randomUUID();
        Category category = buildCategory(1L, "Transport", TransactionType.EXPENSE, buildUser(userId), false);

        when(categoryRepository.findAccessibleById(1L, userId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameForOwnerExcludingId("Mobilité", userId, 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, request("Mobilité"), userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(businessException.getMessage()).isEqualTo("Une catégorie avec ce nom existe déjà.");
                });
    }

    @Test
    @DisplayName("delete : catégorie personnelle non utilisée → supprime")
    void delete_OwnedUnusedCategory_ShouldDelete() {
        UUID userId = UUID.randomUUID();
        Category category = buildCategory(1L, "Transport", TransactionType.EXPENSE, buildUser(userId), false);

        when(categoryRepository.findAccessibleById(1L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryIdAndUserId(1L, userId)).thenReturn(false);

        categoryService.delete(1L, userId);

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("delete : catégorie par défaut → CATEGORY_IS_DEFAULT")
    void delete_DefaultCategory_ShouldThrowCategoryIsDefault() {
        UUID userId = UUID.randomUUID();
        Category category = buildCategory(1L, "Transport", TransactionType.EXPENSE, null, true);

        when(categoryRepository.findAccessibleById(1L, userId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.delete(1L, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_IS_DEFAULT);

        verify(transactionRepository, never()).existsByCategoryIdAndUserId(1L, userId);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete : catégorie utilisée → CATEGORY_HAS_TRANSACTIONS")
    void delete_UsedCategory_ShouldThrowCategoryHasTransactions() {
        UUID userId = UUID.randomUUID();
        Category category = buildCategory(1L, "Transport", TransactionType.EXPENSE, buildUser(userId), false);

        when(categoryRepository.findAccessibleById(1L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryIdAndUserId(1L, userId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_HAS_TRANSACTIONS);

        verify(categoryRepository, never()).delete(any());
    }

    private CategoryRequest request(String name) {
        return new CategoryRequest(name, "car-outline", "#654321", TransactionType.EXPENSE);
    }

    private Category buildCategory(Long id, String name, TransactionType type, User user, boolean isDefault) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setIcon("car-outline");
        category.setColor("#654321");
        category.setType(type);
        category.setUser(user);
        category.setDefault(isDefault);
        return category;
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.getType(),
                category.isDefault()
        );
    }

    private User buildUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setName("Nicodème");
        user.setEmail("test@owomi.dev");
        user.setPassword("$2a$12$hashedpassword");
        user.setRole(Role.USER);
        user.setEmailVerified(true);
        return user;
    }
}
