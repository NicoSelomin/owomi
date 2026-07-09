package dev.selonick.owomi.transaction;

import dev.selonick.owomi.category.Category;
import dev.selonick.owomi.category.CategoryRepository;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.transaction.dto.TransactionPageResponse;
import dev.selonick.owomi.transaction.dto.TransactionRequest;
import dev.selonick.owomi.transaction.dto.TransactionResponse;
import dev.selonick.owomi.user.Role;
import dev.selonick.owomi.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("findAll : retourne une page de transactions filtrée par utilisateur")
    void findAll_ShouldReturnTransactionPage() {
        UUID userId = UUID.randomUUID();
        Transaction transaction = buildTransaction(1L, buildUser(userId), buildCategory(10L, TransactionType.EXPENSE));
        TransactionResponse response = response(transaction);

        when(transactionRepository.findByUserAndFilters(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(TransactionType.EXPENSE),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(transaction), PageRequest.of(0, 20), 1));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        TransactionPageResponse result = transactionService.findAll(
                userId,
                TransactionType.EXPENSE,
                null,
                null,
                null,
                0,
                20
        );

        assertThat(result.content()).containsExactly(response);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findAll : catégorie filtrée inaccessible → RESOURCE_NOT_FOUND")
    void findAll_InaccessibleCategoryFilter_ShouldThrowNotFound() {
        UUID userId = UUID.randomUUID();

        when(categoryRepository.findAccessibleById(99L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findAll(userId, null, 99L, null, null, 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(transactionRepository, never()).findByUserAndFilters(
                any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("findAll : période invalide → VALIDATION_ERROR")
    void findAll_InvalidPeriod_ShouldThrowValidationError() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> transactionService.findAll(
                userId,
                null,
                null,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1),
                0,
                20
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("findAll : pagination invalide → VALIDATION_ERROR")
    void findAll_InvalidPagination_ShouldThrowValidationError() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> transactionService.findAll(userId, null, null, null, null, -1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> transactionService.findAll(userId, null, null, null, null, 0, 101))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("findById : transaction propriétaire → retourne le DTO")
    void findById_OwnedTransaction_ShouldReturnResponse() {
        UUID userId = UUID.randomUUID();
        Transaction transaction = buildTransaction(1L, buildUser(userId), buildCategory(10L, TransactionType.EXPENSE));
        TransactionResponse response = response(transaction);

        when(transactionRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        assertThat(transactionService.findById(1L, userId)).isEqualTo(response);
    }

    @Test
    @DisplayName("findById : transaction absente ou non propriétaire → RESOURCE_NOT_FOUND")
    void findById_NotOwned_ShouldThrowNotFound() {
        UUID userId = UUID.randomUUID();

        when(transactionRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById(1L, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("create : requête valide → crée avec l'utilisateur authentifié")
    void create_ValidRequest_ShouldCreateTransactionForAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        Category category = buildCategory(10L, TransactionType.EXPENSE);
        Transaction saved = buildTransaction(1L, user, category);
        TransactionResponse response = response(saved);
        TransactionRequest request = request(TransactionType.EXPENSE, 10L, LocalDate.now(), new BigDecimal("150.50"));

        when(categoryRepository.findAccessibleById(10L, userId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
        when(transactionMapper.toResponse(saved)).thenReturn(response);

        TransactionResponse result = transactionService.create(request, user);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction created = transactionCaptor.getValue();
        assertThat(created.getAmount()).isEqualByComparingTo("150.50");
        assertThat(created.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(created.getCategory()).isSameAs(category);
        assertThat(created.getUser()).isSameAs(user);
        assertThat(created.getNote()).isEqualTo("note");
    }

    @Test
    @DisplayName("create : montant invalide → AMOUNT_INVALID")
    void create_InvalidAmount_ShouldThrowAmountInvalid() {
        User user = buildUser(UUID.randomUUID());
        TransactionRequest request = request(TransactionType.EXPENSE, 10L, LocalDate.now(), BigDecimal.ZERO);

        assertThatThrownBy(() -> transactionService.create(request, user))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AMOUNT_INVALID);
    }

    @Test
    @DisplayName("create : date future → FUTURE_DATE")
    void create_FutureDate_ShouldThrowFutureDate() {
        User user = buildUser(UUID.randomUUID());
        TransactionRequest request = request(
                TransactionType.EXPENSE,
                10L,
                LocalDate.now().plusDays(1),
                BigDecimal.ONE
        );

        assertThatThrownBy(() -> transactionService.create(request, user))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUTURE_DATE);
    }

    @Test
    @DisplayName("create : catégorie inaccessible → RESOURCE_NOT_FOUND")
    void create_InaccessibleCategory_ShouldThrowNotFound() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        TransactionRequest request = request(TransactionType.EXPENSE, 99L, LocalDate.now(), BigDecimal.ONE);

        when(categoryRepository.findAccessibleById(99L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(request, user))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("create : type catégorie incompatible → VALIDATION_ERROR")
    void create_CategoryTypeMismatch_ShouldThrowValidationError() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        Category category = buildCategory(10L, TransactionType.INCOME);
        TransactionRequest request = request(TransactionType.EXPENSE, 10L, LocalDate.now(), BigDecimal.ONE);

        when(categoryRepository.findAccessibleById(10L, userId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> transactionService.create(request, user))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("update : transaction propriétaire → met à jour les champs")
    void update_OwnedTransaction_ShouldUpdateFields() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        Category category = buildCategory(10L, TransactionType.EXPENSE);
        Transaction transaction = buildTransaction(1L, user, category);
        TransactionResponse response = response(transaction);
        TransactionRequest request = request(TransactionType.EXPENSE, 10L, LocalDate.now(), new BigDecimal("42.00"));

        when(transactionRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findAccessibleById(10L, userId)).thenReturn(Optional.of(category));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        TransactionResponse result = transactionService.update(1L, request, userId);

        assertThat(result).isEqualTo(response);
        assertThat(transaction.getAmount()).isEqualByComparingTo("42.00");
        assertThat(transaction.getCategory()).isSameAs(category);
        assertThat(transaction.getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("update : transaction absente ou non propriétaire → RESOURCE_NOT_FOUND")
    void update_NotOwned_ShouldThrowNotFound() {
        UUID userId = UUID.randomUUID();
        TransactionRequest request = request(TransactionType.EXPENSE, 10L, LocalDate.now(), BigDecimal.ONE);

        when(transactionRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.update(1L, request, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("delete : transaction propriétaire → supprime")
    void delete_OwnedTransaction_ShouldDelete() {
        UUID userId = UUID.randomUUID();
        Transaction transaction = buildTransaction(1L, buildUser(userId), buildCategory(10L, TransactionType.EXPENSE));

        when(transactionRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(transaction));

        transactionService.delete(1L, userId);

        verify(transactionRepository).delete(transaction);
    }

    @Test
    @DisplayName("delete : transaction absente ou non propriétaire → RESOURCE_NOT_FOUND")
    void delete_NotOwned_ShouldThrowNotFound() {
        UUID userId = UUID.randomUUID();

        when(transactionRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(1L, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private TransactionRequest request(
            TransactionType type,
            Long categoryId,
            LocalDate date,
            BigDecimal amount
    ) {
        return new TransactionRequest(amount, type, categoryId, date, " note ");
    }

    private Transaction buildTransaction(Long id, User user, Category category) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setAmount(new BigDecimal("150.50"));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setNote("note");
        transaction.setDate(LocalDate.now());
        transaction.setCategory(category);
        transaction.setUser(user);
        return transaction;
    }

    private Category buildCategory(Long id, TransactionType type) {
        Category category = new Category();
        category.setId(id);
        category.setName("Transport");
        category.setIcon("car-outline");
        category.setColor("#654321");
        category.setType(type);
        return category;
    }

    private TransactionResponse response(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getNote(),
                transaction.getDate(),
                null
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
