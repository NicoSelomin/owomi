package dev.selonick.owomi.currency;

import dev.selonick.owomi.currency.dto.CurrencyDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private CurrencyMapper currencyMapper;

    @InjectMocks
    private CurrencyService currencyService;

    @Test
    @DisplayName("findAll : retourne les devises triées par code et mappées en DTO")
    void findAll_ShouldReturnDtosOrderedByCode() {
        Currency eur = new Currency("EUR", "Euro", "€", "fr-FR");
        Currency xof = new Currency("XOF", "Franc CFA UEMOA", "FCFA", "fr-BJ");
        CurrencyDTO eurDto = new CurrencyDTO("EUR", "Euro", "€", "fr-FR");
        CurrencyDTO xofDto = new CurrencyDTO("XOF", "Franc CFA UEMOA", "FCFA", "fr-BJ");

        when(currencyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(eur, xof));
        when(currencyMapper.toDto(eur)).thenReturn(eurDto);
        when(currencyMapper.toDto(xof)).thenReturn(xofDto);

        List<CurrencyDTO> result = currencyService.findAll();

        assertThat(result).containsExactly(eurDto, xofDto);

        InOrder inOrder = inOrder(currencyRepository, currencyMapper);
        inOrder.verify(currencyRepository).findAllByOrderByCodeAsc();
        inOrder.verify(currencyMapper).toDto(eur);
        inOrder.verify(currencyMapper).toDto(xof);
    }
}
