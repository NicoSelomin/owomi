package dev.selonick.owomi.transaction;

import dev.selonick.owomi.category.Category;
import dev.selonick.owomi.category.CategoryRepository;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.transaction.dto.TransactionPageResponse;
import dev.selonick.owomi.transaction.dto.TransactionRequest;
import dev.selonick.owomi.transaction.dto.TransactionResponse;
import dev.selonick.owomi.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Gestion des transactions de l'utilisateur authentifié.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String INVALID_CATEGORY_TYPE_MESSAGE =
            "La catégorie ne correspond pas au type de transaction.";
    private static final String INVALID_PERIOD_MESSAGE =
            "La date de début doit être antérieure ou égale à la date de fin.";
    private static final String INVALID_PAGE_MESSAGE =
            "La page doit être supérieure ou égale à 0.";
    private static final String INVALID_SIZE_MESSAGE =
            "La taille doit être comprise entre 1 et 100.";

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public TransactionPageResponse findAll(
            UUID userId,
            TransactionType type,
            Long categoryId,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        validateFilters(startDate, endDate, page, size);
        if (categoryId != null) {
            findAccessibleCategory(categoryId, userId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactions = transactionRepository.findByUserAndFilters(
                userId,
                type,
                categoryId,
                startDate,
                endDate,
                pageable
        );

        return new TransactionPageResponse(
                transactions.getContent().stream()
                        .map(transactionMapper::toResponse)
                        .toList(),
                transactions.getNumber(),
                transactions.getSize(),
                transactions.getTotalElements(),
                transactions.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id, UUID userId) {
        Transaction transaction = findOwnedTransaction(id, userId);
        return transactionMapper.toResponse(transaction);
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request, User user) {
        validateRequest(request);
        Category category = findAccessibleCategory(request.categoryId(), user.getId());
        validateCategoryType(category, request.type());

        Transaction transaction = new Transaction();
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(category);
        transaction.setDate(request.date());
        transaction.setNote(normalizeNote(request.note()));
        transaction.setUser(user);

        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request, UUID userId) {
        validateRequest(request);
        Transaction transaction = findOwnedTransaction(id, userId);
        Category category = findAccessibleCategory(request.categoryId(), userId);
        validateCategoryType(category, request.type());

        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setCategory(category);
        transaction.setDate(request.date());
        transaction.setNote(normalizeNote(request.note()));

        return transactionMapper.toResponse(transaction);
    }

    @Transactional
    public void delete(Long id, UUID userId) {
        Transaction transaction = findOwnedTransaction(id, userId);
        transactionRepository.delete(transaction);
    }

    private void validateFilters(LocalDate startDate, LocalDate endDate, int page, int size) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, INVALID_PERIOD_MESSAGE);
        }
        if (page < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, INVALID_PAGE_MESSAGE);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, INVALID_SIZE_MESSAGE);
        }
    }

    private void validateRequest(TransactionRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.AMOUNT_INVALID);
        }
        if (request.date() != null && request.date().isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.FUTURE_DATE);
        }
    }

    private Transaction findOwnedTransaction(Long id, UUID userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Category findAccessibleCategory(Long categoryId, UUID userId) {
        return categoryRepository.findAccessibleById(categoryId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateCategoryType(Category category, TransactionType transactionType) {
        if (category.getType() != transactionType) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, INVALID_CATEGORY_TYPE_MESSAGE);
        }
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
