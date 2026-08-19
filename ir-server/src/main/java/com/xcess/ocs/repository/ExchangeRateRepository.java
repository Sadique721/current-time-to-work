package com.xcess.ocs.repository;

import com.xcess.ocs.entity.ExchangeRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Page<ExchangeRate> findAllByOrderByValidFromDescIdDesc(Pageable pageable);

    @Query("SELECT e FROM ExchangeRate e WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(e.baseCurrency) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.targetCurrency) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.source) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:validFrom IS NULL OR e.validFrom = :validFrom) AND " +
           "e.isDeleted = false " +
           "ORDER BY e.validFrom DESC, e.id DESC")
    Page<ExchangeRate> searchExchangeRates(@Param("searchTerm") String searchTerm,
                                            @Param("validFrom") LocalDate validFrom,
                                            Pageable pageable);

    List<ExchangeRate> findByBaseCurrencyAndValidFrom(String baseCurrency, LocalDate validFrom);

    @Query("SELECT MAX(e.validFrom) FROM ExchangeRate e WHERE e.baseCurrency = :baseCurrency AND e.isDeleted = false")
    LocalDate findMaxValidFromByBaseCurrency(@Param("baseCurrency") String baseCurrency);
}
