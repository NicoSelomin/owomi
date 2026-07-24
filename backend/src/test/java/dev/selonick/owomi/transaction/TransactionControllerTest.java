package dev.selonick.owomi.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.selonick.owomi.auth.CustomUserDetails;
import dev.selonick.owomi.auth.CustomUserDetailsService;
import dev.selonick.owomi.auth.JwtService;
import dev.selonick.owomi.category.dto.CategoryResponse;
import dev.selonick.owomi.common.enums.TransactionType;
import dev.selonick.owomi.common.exception.BusinessException;
import dev.selonick.owomi.common.exception.ErrorCode;
import dev.selonick.owomi.common.exception.GlobalExceptionHandler;
import dev.selonick.owomi.transaction.dto.TransactionPageResponse;
import dev.selonick.owomi.transaction.dto.TransactionRequest;
import dev.selonick.owomi.transaction.dto.TransactionResponse;
import dev.selonick.owomi.transaction.export.TransactionExportCsv;
import dev.selonick.owomi.transaction.export.TransactionExportService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private TransactionExportService transactionExportService;

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
    @DisplayName("GET /api/transactions : retourne une page de transactions")
    void findAll_ShouldReturnTransactionPageApiResponse() throws Exception {
        User user = authenticate();
        TransactionPageResponse page = new TransactionPageResponse(List.of(response()), 0, 20, 1, 1);
        when(transactionService.findAll(user.getId(), null, null, null, null, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].amount").value(150.5))
                .andExpect(jsonPath("$.data.content[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.data.content[0].category.id").value(10))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.message").value("Transactions récupérées."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/transactions : délègue les filtres")
    void findAll_WithFilters_ShouldDelegateFilters() throws Exception {
        User user = authenticate();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        TransactionPageResponse page = new TransactionPageResponse(List.of(response()), 0, 10, 1, 1);
        when(transactionService.findAll(
                user.getId(),
                TransactionType.EXPENSE,
                10L,
                startDate,
                endDate,
                0,
                10
        )).thenReturn(page);

        mockMvc.perform(get("/api/transactions")
                        .param("type", "EXPENSE")
                        .param("categoryId", "10")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(transactionService).findAll(
                user.getId(),
                TransactionType.EXPENSE,
                10L,
                startDate,
                endDate,
                0,
                10
        );
    }

    @Test
    @DisplayName("GET /api/transactions : type invalide → VALIDATION_ERROR")
    void findAll_InvalidType_ShouldReturnValidationError() throws Exception {
        authenticate();

        mockMvc.perform(get("/api/transactions").param("type", "BAD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/transactions : période invalide → VALIDATION_ERROR")
    void findAll_InvalidPeriod_ShouldReturnBusinessError() throws Exception {
        User user = authenticate();
        when(transactionService.findAll(
                user.getId(),
                null,
                null,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1),
                0,
                20
        )).thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR,
                "La date de début doit être antérieure ou égale à la date de fin."));

        mockMvc.perform(get("/api/transactions")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/transactions/export/csv : mois courant par défaut")
    void exportCsv_DefaultPeriod_ShouldReturnCsvAttachment() throws Exception {
        User user = authenticate();
        byte[] csv = "Date,Type,Catégorie,Montant,Devise,Note,Créée le,Modifiée le\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(transactionExportService.exportCsv(user.getId(), null, null, null, null))
                .thenReturn(new TransactionExportCsv("owomi-transactions-2026-01-01-2026-01-31.csv", csv));

        mockMvc.perform(get("/api/transactions/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"owomi-transactions-2026-01-01-2026-01-31.csv\""))
                .andExpect(content().bytes(csv));

        verify(transactionExportService).exportCsv(user.getId(), null, null, null, null);
    }

    @Test
    @DisplayName("GET /api/transactions/export/csv : période et filtres valides")
    void exportCsv_WithFilters_ShouldDelegateFilters() throws Exception {
        User user = authenticate();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        byte[] csv = "Date,Type,Catégorie,Montant,Devise,Note,Créée le,Modifiée le\n2026-01-10,EXPENSE,Transport,12.00,XOF,note,,\n"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(transactionExportService.exportCsv(user.getId(), TransactionType.EXPENSE, 10L, startDate, endDate))
                .thenReturn(new TransactionExportCsv("owomi-transactions-2026-01-01-2026-01-31.csv", csv));

        mockMvc.perform(get("/api/transactions/export/csv")
                        .param("type", "EXPENSE")
                        .param("categoryId", "10")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"owomi-transactions-2026-01-01-2026-01-31.csv\""))
                .andExpect(content().bytes(csv));

        verify(transactionExportService).exportCsv(user.getId(), TransactionType.EXPENSE, 10L, startDate, endDate);
    }

    @Test
    @DisplayName("GET /api/transactions/export/csv : une seule date → VALIDATION_ERROR")
    void exportCsv_PartialPeriod_ShouldReturnValidationError() throws Exception {
        User user = authenticate();
        when(transactionExportService.exportCsv(
                user.getId(),
                null,
                null,
                LocalDate.of(2026, 1, 1),
                null
        )).thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR,
                "La date de début et la date de fin doivent être fournies ensemble."));

        mockMvc.perform(get("/api/transactions/export/csv").param("startDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/transactions/export/csv : période inversée → VALIDATION_ERROR")
    void exportCsv_InvertedPeriod_ShouldReturnValidationError() throws Exception {
        User user = authenticate();
        when(transactionExportService.exportCsv(
                user.getId(),
                null,
                null,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 31)
        )).thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR,
                "La date de début doit être antérieure ou égale à la date de fin."));

        mockMvc.perform(get("/api/transactions/export/csv")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/transactions/export/csv : période supérieure à 24 mois → VALIDATION_ERROR")
    void exportCsv_TooLongPeriod_ShouldReturnValidationError() throws Exception {
        User user = authenticate();
        when(transactionExportService.exportCsv(
                user.getId(),
                null,
                null,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2026, 1, 1)
        )).thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR,
                "La période ne peut pas dépasser 24 mois calendaires."));

        mockMvc.perform(get("/api/transactions/export/csv")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/transactions/export/csv : type invalide → VALIDATION_ERROR")
    void exportCsv_InvalidType_ShouldReturnValidationError() throws Exception {
        authenticate();

        mockMvc.perform(get("/api/transactions/export/csv").param("type", "BAD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/transactions/export/csv : catégorie inaccessible → RESOURCE_NOT_FOUND")
    void exportCsv_InaccessibleCategory_ShouldReturnNotFound() throws Exception {
        User user = authenticate();
        when(transactionExportService.exportCsv(user.getId(), null, 99L, null, null))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/transactions/export/csv").param("categoryId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/transactions/{id} : retourne une transaction")
    void findById_ShouldReturnTransactionApiResponse() throws Exception {
        User user = authenticate();
        when(transactionService.findById(1L, user.getId())).thenReturn(response());

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.message").value("Transaction récupérée."));
    }

    @Test
    @DisplayName("GET /api/transactions/{id} : introuvable → RESOURCE_NOT_FOUND")
    void findById_NotFound_ShouldReturnBusinessError() throws Exception {
        User user = authenticate();
        when(transactionService.findById(99L, user.getId()))
                .thenThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/transactions/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/transactions : crée une transaction")
    void create_ValidRequest_ShouldReturnCreatedApiResponse() throws Exception {
        User user = authenticate();
        TransactionRequest request = request();
        when(transactionService.create(request, user)).thenReturn(response());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.message").value("Transaction créée avec succès."));
    }

    @Test
    @DisplayName("POST /api/transactions : requête invalide → VALIDATION_ERROR")
    void create_InvalidRequest_ShouldReturnValidationError() throws Exception {
        authenticate();
        TransactionRequest request = new TransactionRequest(
                BigDecimal.ZERO,
                null,
                null,
                null,
                "note"
        );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details").isArray());
    }

    @Test
    @DisplayName("POST /api/transactions : Content-Type non supporté → UNSUPPORTED_MEDIA_TYPE")
    void create_UnsupportedMediaType_ShouldReturnApiResponse() throws Exception {
        authenticate();

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"amount\":12.00}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.error.message").value("Type de contenu non supporté."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/transactions : JSON malformé → VALIDATION_ERROR")
    void create_MalformedJson_ShouldReturnValidationError() throws Exception {
        authenticate();

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":12.00,\"type\":\"EXPENSE\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Le corps de la requête est invalide."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/transactions : date future → FUTURE_DATE")
    void create_FutureDate_ShouldReturnBusinessError() throws Exception {
        User user = authenticate();
        TransactionRequest request = request();
        when(transactionService.create(request, user))
                .thenThrow(new BusinessException(ErrorCode.FUTURE_DATE));

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FUTURE_DATE"));
    }

    @Test
    @DisplayName("PUT /api/transactions/{id} : met à jour une transaction")
    void update_ValidRequest_ShouldReturnOkApiResponse() throws Exception {
        User user = authenticate();
        TransactionRequest request = request();
        when(transactionService.update(1L, request, user.getId())).thenReturn(response());

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transaction mise à jour avec succès."));
    }

    @Test
    @DisplayName("PUT /api/transactions/{id} : catégorie incompatible → VALIDATION_ERROR")
    void update_CategoryMismatch_ShouldReturnBusinessError() throws Exception {
        User user = authenticate();
        TransactionRequest request = request();
        when(transactionService.update(1L, request, user.getId()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "La catégorie ne correspond pas au type de transaction."));

        mockMvc.perform(put("/api/transactions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("DELETE /api/transactions/{id} : supprime une transaction")
    void delete_ValidRequest_ShouldReturnOkApiResponse() throws Exception {
        User user = authenticate();

        mockMvc.perform(delete("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transaction supprimée avec succès."));

        verify(transactionService).delete(1L, user.getId());
    }

    @Test
    @DisplayName("DELETE /api/transactions/{id} : introuvable → RESOURCE_NOT_FOUND")
    void delete_NotFound_ShouldReturnBusinessError() throws Exception {
        User user = authenticate();
        doThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))
                .when(transactionService).delete(99L, user.getId());

        mockMvc.perform(delete("/api/transactions/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
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

    private TransactionRequest request() {
        return new TransactionRequest(
                new BigDecimal("150.50"),
                TransactionType.EXPENSE,
                10L,
                LocalDate.of(2026, 1, 10),
                "note"
        );
    }

    private TransactionResponse response() {
        return new TransactionResponse(
                1L,
                new BigDecimal("150.50"),
                TransactionType.EXPENSE,
                "note",
                LocalDate.of(2026, 1, 10),
                new CategoryResponse(10L, "Transport", "car-outline", "#654321", TransactionType.EXPENSE, false)
        );
    }
}
