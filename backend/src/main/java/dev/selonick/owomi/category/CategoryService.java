package dev.selonick.owomi.category;

import dev.selonick.owomi.category.dto.CategoryRequest;
import dev.selonick.owomi.category.dto.CategoryResponse;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.transaction.TransactionRepository;
import dev.selonick.owomi.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gestion des catégories accessibles à l'utilisateur authentifié.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final String DUPLICATE_NAME_MESSAGE = "Une catégorie avec ce nom existe déjà.";

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll(UUID userId, TransactionType type) {
        List<Category> categories = type == null
                ? categoryRepository.findAccessibleByUserId(userId)
                : categoryRepository.findAccessibleByUserIdAndType(userId, type);

        return categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id, UUID userId) {
        Category category = findAccessibleCategory(id, userId);
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request, User user) {
        String name = request.name().trim();
        ensureNameIsAvailable(name, user.getId());

        Category category = new Category();
        category.setName(name);
        category.setIcon(request.icon().trim());
        category.setColor(request.color().trim());
        category.setType(request.type());
        category.setDefault(false);
        category.setUser(user);

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request, UUID userId) {
        Category category = findAccessibleCategory(id, userId);
        rejectDefaultCategory(category);
        ensureOwnedByUser(category, userId);

        String name = request.name().trim();
        if (categoryRepository.existsByNameForOwnerExcludingId(name, userId, id)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, DUPLICATE_NAME_MESSAGE);
        }

        category.setName(name);
        category.setIcon(request.icon().trim());
        category.setColor(request.color().trim());
        category.setType(request.type());

        return categoryMapper.toResponse(category);
    }

    @Transactional
    public void delete(Long id, UUID userId) {
        Category category = findAccessibleCategory(id, userId);
        rejectDefaultCategory(category);
        ensureOwnedByUser(category, userId);

        if (transactionRepository.existsByCategoryIdAndUserId(id, userId)) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_TRANSACTIONS);
        }

        categoryRepository.delete(category);
    }

    private Category findAccessibleCategory(Long id, UUID userId) {
        return categoryRepository.findAccessibleById(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void ensureOwnedByUser(Category category, UUID userId) {
        if (category.getUser() == null || !userId.equals(category.getUser().getId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private void ensureNameIsAvailable(String name, UUID userId) {
        if (categoryRepository.existsByNameForOwner(name, userId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, DUPLICATE_NAME_MESSAGE);
        }
    }

    private void rejectDefaultCategory(Category category) {
        if (category.isDefault()) {
            throw new BusinessException(ErrorCode.CATEGORY_IS_DEFAULT);
        }
    }
}
