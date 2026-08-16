package com.safeedge.historical.repository;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalSource;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricalMatchRepository extends JpaRepository<HistoricalMatchEntity, Long> {

	Optional<HistoricalMatchEntity> findBySourceAndCanonicalCompetitionAndSeasonStartYearAndSeasonEndYearAndMatchDateAndSourceHomeTeamNameAndSourceAwayTeamName(
			HistoricalSource source,
			CanonicalCompetition canonicalCompetition,
			Integer seasonStartYear,
			Integer seasonEndYear,
			LocalDate matchDate,
			String sourceHomeTeamName,
			String sourceAwayTeamName);

}
