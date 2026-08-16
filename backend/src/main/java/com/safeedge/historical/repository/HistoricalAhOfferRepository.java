package com.safeedge.historical.repository;

import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistoricalAhOfferRepository extends JpaRepository<HistoricalAhOfferEntity, Long> {

	Optional<HistoricalAhOfferEntity> findByHistoricalMatch_IdAndQuoteSourceAndObservationType(
			Long historicalMatchId,
			HistoricalQuoteSource quoteSource,
			HistoricalObservationType observationType);

	@Query("""
			select new com.safeedge.historical.repository.HistoricalLeagueSeasonQuoteCount(
				m.canonicalCompetition, m.seasonStartYear, m.seasonEndYear, o.quoteSource, count(distinct m.id))
			from HistoricalAhOfferEntity o
			join o.historicalMatch m
			where m.source = :source
			group by m.canonicalCompetition, m.seasonStartYear, m.seasonEndYear, o.quoteSource
			""")
	List<HistoricalLeagueSeasonQuoteCount> countDistinctMatchesWithQuoteByLeagueSeason(
			@Param("source") HistoricalSource source);

	@Query("""
			select new com.safeedge.historical.repository.HistoricalLeagueSeasonCount(
				m.canonicalCompetition, m.seasonStartYear, m.seasonEndYear, count(distinct m.id))
			from HistoricalAhOfferEntity o
			join o.historicalMatch m
			where m.source = :source
			group by m.canonicalCompetition, m.seasonStartYear, m.seasonEndYear
			""")
	List<HistoricalLeagueSeasonCount> countDistinctMatchesWithAnyQuoteByLeagueSeason(
			@Param("source") HistoricalSource source);

}
