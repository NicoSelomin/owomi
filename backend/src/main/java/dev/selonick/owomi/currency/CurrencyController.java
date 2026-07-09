package dev.selonick.owomi.currency;

import dev.selonick.owomi.common.api.ApiResponse;
import dev.selonick.owomi.currency.dto.CurrencyDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint public des devises supportées.
 */
@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
@Tag(name = "Devises", description = "Devises supportées par OWOMI")
public class CurrencyController {

    private final CurrencyService currencyService;

    @Operation(summary = "Liste des devises supportées")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CurrencyDTO>>> findAll() {
        List<CurrencyDTO> currencies = currencyService.findAll();
        return ResponseEntity.ok(ApiResponse.success(currencies, "Devises récupérées."));
    }
}
