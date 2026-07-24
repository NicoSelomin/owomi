package dev.selonick.owomi.currency;

import dev.selonick.owomi.common.exception.GlobalExceptionHandler;
import dev.selonick.owomi.currency.dto.CurrencyDTO;
import dev.selonick.owomi.auth.CustomUserDetailsService;
import dev.selonick.owomi.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CurrencyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrencyService currencyService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    @DisplayName("GET /api/currencies : retourne les devises dans une ApiResponse")
    void findAll_ShouldReturnCurrenciesApiResponse() throws Exception {
        when(currencyService.findAll()).thenReturn(List.of(
                new CurrencyDTO("EUR", "Euro", "€", "fr-FR"),
                new CurrencyDTO("XOF", "Franc CFA UEMOA", "FCFA", "fr-BJ")
        ));

        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("EUR"))
                .andExpect(jsonPath("$.data[0].name").value("Euro"))
                .andExpect(jsonPath("$.data[0].symbol").value("€"))
                .andExpect(jsonPath("$.data[0].locale").value("fr-FR"))
                .andExpect(jsonPath("$.data[1].code").value("XOF"))
                .andExpect(jsonPath("$.message").value("Devises récupérées."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
