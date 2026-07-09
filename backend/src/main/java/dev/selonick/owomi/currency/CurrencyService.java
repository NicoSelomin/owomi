package dev.selonick.owomi.currency;

import dev.selonick.owomi.currency.dto.CurrencyDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lecture des devises de référence supportées par OWOMI.
 */
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    @Transactional(readOnly = true)
    public List<CurrencyDTO> findAll() {
        return currencyRepository.findAllByOrderByCodeAsc().stream()
                .map(currencyMapper::toDto)
                .toList();
    }
}
