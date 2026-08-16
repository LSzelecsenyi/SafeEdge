package com.safeedge.historical.repository;

import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricalAhOfferRepository extends JpaRepository<HistoricalAhOfferEntity, Long> {

	Optional<HistoricalAhOfferEntity> findByHistoricalMatch_IdAndQuoteSourceAndObservationType(
			Long historicalMatchId,
			HistoricalQuoteSource quoteSource,
			HistoricalObservationType observationType);

}
