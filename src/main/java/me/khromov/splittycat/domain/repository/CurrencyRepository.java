package me.khromov.splittycat.domain.repository;

import me.khromov.splittycat.domain.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    /**
     * Поиск валюты по коду (без учёта регистра).
     */
    Optional<Currency> findByCodeIgnoreCase(String code);
}
