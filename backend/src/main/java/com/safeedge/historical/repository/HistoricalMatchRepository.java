package com.safeedge.historical.repository;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistoricalMatchRepository extends JpaRepository<HistoricalMatchEntity, Long> {

	Optional<HistoricalMatchEntity> findBySourceAndCanonicalCompetitionAndSeasonStartYearAndSeasonEndYearAndMatchDateAndSourceHomeTeamNameAndSourceAwayTeamName(
			HistoricalSource source,
			CanonicalCompetition canonicalCompetition,
			Integer seasonStartYear,
			Integer seasonEndYear,
			LocalDate matchDate,
			String sourceHomeTeamName,
			String sourceAwayTeamName);

	@Query("""
			select new com.safeedge.historical.repository.HistoricalLeagueSeasonCount(
				m.canonicalCompetition, m.seasonStartYear, m.seasonEndYear, count(m))
			from HistoricalMatchEntity m
			where m.source = :source
			group by m.canonicalCompetition, m.seasonStartYear, m.seasonEndYear
			""")
	List<HistoricalLeagueSeasonCount> countMatchesByLeagueSeason(@Param("source") HistoricalSource source);

}
