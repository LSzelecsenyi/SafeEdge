package com.safeedge.historical.service;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalAhCoverageReport;
import com.safeedge.historical.domain.HistoricalAhLeagueSeasonCoverage;
import com.safeedge.historical.domain.HistoricalAhQuoteSourceCoverage;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.repository.HistoricalAhOfferRepository;
import com.safeedge.historical.repository.HistoricalLeagueSeasonCount;
import com.safeedge.historical.repository.HistoricalLeagueSeasonQuoteCount;
import com.safeedge.historical.repository.HistoricalMatchRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only AH coverage over persisted {@code historical_match} /
 * {@code historical_ah_offer}. Does not fetch source files.
 */
@Service
public class HistoricalAhCoverageService {

	private record LeagueSeasonKey(CanonicalCompetition competition, int startYear, int endYear) {
	}

	private final HistoricalMatchRepository matchRepository;
	private final HistoricalAhOfferRepository offerRepository;
	private final Clock clock;

	public HistoricalAhCoverageService(
			HistoricalMatchRepository matchRepository, HistoricalAhOfferRepository offerRepository, Clock clock) {
		this.matchRepository = matchRepository;
		this.offerRepository = offerRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public HistoricalAhCoverageReport report() {
		return report(HistoricalSource.FOOTBALL_DATA_UK);
	}

	@Transactional(readOnly = true)
	public HistoricalAhCoverageReport report(HistoricalSource source) {
		List<HistoricalLeagueSeasonCount> matchCounts = matchRepository.countMatchesByLeagueSeason(source);
		Map<LeagueSeasonKey, Long> anyQuoteCounts = indexCounts(
				offerRepository.countDistinctMatchesWithAnyQuoteByLeagueSeason(source));
		Map<LeagueSeasonKey, Map<HistoricalQuoteSource, Long>> quoteCounts = indexQuoteCounts(
				offerRepository.countDistinctMatchesWithQuoteByLeagueSeason(source));
		List<HistoricalAhLeagueSeasonCoverage> rows = new ArrayList<>();
		for (HistoricalLeagueSeasonCount matchCount : matchCounts) {
			LeagueSeasonKey key = new LeagueSeasonKey(
					matchCount.competition(), matchCount.seasonStartYear(), matchCount.seasonEndYear());
			int totalMatches = Math.toIntExact(matchCount.matchCount());
			int matchesWithAnyQuote = Math.toIntExact(anyQuoteCounts.getOrDefault(key, 0L));
			Map<HistoricalQuoteSource, Long> bySource = quoteCounts.getOrDefault(key, Map.of());
			List<HistoricalAhQuoteSourceCoverage> sourceCoverages = new ArrayList<>();
			HistoricalQuoteSource bestSource = null;
			BigDecimal bestRate = BigDecimal.ZERO;
			for (HistoricalQuoteSource quoteSource : HistoricalQuoteSource.values()) {
				int matchesWithQuote = Math.toIntExact(bySource.getOrDefault(quoteSource, 0L));
				BigDecimal rate = coverageRate(matchesWithQuote, totalMatches);
				sourceCoverages.add(new HistoricalAhQuoteSourceCoverage(
						quoteSource, totalMatches, matchesWithQuote, rate));
				if (rate.compareTo(bestRate) > 0) {
					bestSource = quoteSource;
					bestRate = rate;
				}
			}
			rows.add(new HistoricalAhLeagueSeasonCoverage(
					matchCount.competition(),
					new FootballSeason(matchCount.seasonStartYear(), matchCount.seasonEndYear()),
					totalMatches,
					matchesWithAnyQuote,
					coverageRate(matchesWithAnyQuote, totalMatches),
					bestSource,
					bestRate,
					sourceCoverages));
		}
		rows.sort(Comparator.comparing(HistoricalAhLeagueSeasonCoverage::competition)
				.thenComparing(row -> row.season().startYear()));
		return new HistoricalAhCoverageReport(clock.instant(), source, rows);
	}

	static BigDecimal coverageRate(int matchesWithQuote, int totalMatches) {
		if (totalMatches == 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(matchesWithQuote).divide(BigDecimal.valueOf(totalMatches), MathContext.DECIMAL128);
	}

	private static Map<LeagueSeasonKey, Long> indexCounts(List<HistoricalLeagueSeasonCount> rows) {
		Map<LeagueSeasonKey, Long> indexed = new HashMap<>();
		for (HistoricalLeagueSeasonCount row : rows) {
			indexed.put(
					new LeagueSeasonKey(row.competition(), row.seasonStartYear(), row.seasonEndYear()),
					row.matchCount());
		}
		return indexed;
	}

	private static Map<LeagueSeasonKey, Map<HistoricalQuoteSource, Long>> indexQuoteCounts(
			List<HistoricalLeagueSeasonQuoteCount> rows) {
		Map<LeagueSeasonKey, Map<HistoricalQuoteSource, Long>> indexed = new HashMap<>();
		for (HistoricalLeagueSeasonQuoteCount row : rows) {
			LeagueSeasonKey key = new LeagueSeasonKey(row.competition(), row.seasonStartYear(), row.seasonEndYear());
			indexed.computeIfAbsent(key, ignored -> new EnumMap<>(HistoricalQuoteSource.class))
					.put(row.quoteSource(), row.matchCount());
		}
		return indexed;
	}
}
