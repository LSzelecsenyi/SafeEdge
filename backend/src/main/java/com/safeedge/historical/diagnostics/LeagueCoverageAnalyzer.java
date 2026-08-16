package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalAhCoverageReport;
import com.safeedge.historical.domain.HistoricalAhLeagueSeasonCoverage;
import com.safeedge.historical.domain.HistoricalAhQuoteSourceCoverage;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only coverage slice for one league and one historical quote source.
 * Does not import data or change evaluation years.
 */
public final class LeagueCoverageAnalyzer {

	public static final List<Integer> DEFAULT_WARMUP_START_YEARS = List.of(2014, 2015, 2016, 2017, 2018);
	public static final List<Integer> DEFAULT_EVALUATION_START_YEARS = List.of(2019, 2020, 2021, 2022, 2023);

	private LeagueCoverageAnalyzer() {
	}

	public static LeagueCoverageSnapshot analyze(
			CanonicalCompetition competition,
			HistoricalQuoteSource quoteSource,
			HistoricalAhCoverageReport coverageReport) {
		return analyze(
				competition,
				quoteSource,
				coverageReport,
				DEFAULT_WARMUP_START_YEARS,
				DEFAULT_EVALUATION_START_YEARS);
	}

	public static LeagueCoverageSnapshot analyze(
			CanonicalCompetition competition,
			HistoricalQuoteSource quoteSource,
			HistoricalAhCoverageReport coverageReport,
			List<Integer> expectedWarmupStartYears,
			List<Integer> expectedEvaluationStartYears) {
		if (competition == null || quoteSource == null) {
			throw new IllegalArgumentException("competition and quoteSource are required");
		}
		if (coverageReport == null) {
			throw new IllegalArgumentException("coverageReport is required");
		}
		List<Integer> warmupYears = List.copyOf(expectedWarmupStartYears);
		List<Integer> evalYears = List.copyOf(expectedEvaluationStartYears);
		Map<Integer, HistoricalAhLeagueSeasonCoverage> byYear = new LinkedHashMap<>();
		for (HistoricalAhLeagueSeasonCoverage row : coverageReport.leagueSeasons()) {
			if (row.competition() == competition) {
				byYear.put(row.season().startYear(), row);
			}
		}
		List<Integer> missingWarmup = missingYears(warmupYears, byYear);
		List<Integer> missingEval = missingYears(evalYears, byYear);
		List<LeagueSeasonCoverageRow> seasons = new ArrayList<>();
		int warmupMatches = 0;
		int evalMatches = 0;
		int evalQuoted = 0;
		List<Integer> allYears = new ArrayList<>(byYear.keySet());
		allYears.sort(Comparator.naturalOrder());
		for (Integer year : allYears) {
			HistoricalAhLeagueSeasonCoverage row = byYear.get(year);
			int quoted = quotedMatches(row, quoteSource);
			boolean warmup = warmupYears.contains(year);
			boolean evaluation = evalYears.contains(year);
			seasons.add(new LeagueSeasonCoverageRow(
					year,
					row.season().displayValue(),
					row.totalMatches(),
					quoted,
					coverageRate(quoted, row.totalMatches()),
					warmup,
					evaluation));
			if (warmup) {
				warmupMatches += row.totalMatches();
			}
			if (evaluation) {
				evalMatches += row.totalMatches();
				evalQuoted += quoted;
			}
		}
		return new LeagueCoverageSnapshot(
				competition,
				quoteSource,
				warmupYears,
				evalYears,
				missingWarmup,
				missingEval,
				seasons,
				warmupMatches,
				evalMatches,
				evalQuoted);
	}

	private static List<Integer> missingYears(
			List<Integer> expected, Map<Integer, HistoricalAhLeagueSeasonCoverage> byYear) {
		List<Integer> missing = new ArrayList<>();
		for (Integer year : expected) {
			if (!byYear.containsKey(year)) {
				missing.add(year);
			}
		}
		return List.copyOf(missing);
	}

	private static int quotedMatches(HistoricalAhLeagueSeasonCoverage row, HistoricalQuoteSource quoteSource) {
		for (HistoricalAhQuoteSourceCoverage source : row.sourceCoverages()) {
			if (source.quoteSource() == quoteSource) {
				return source.matchesWithQuote();
			}
		}
		return 0;
	}

	private static BigDecimal coverageRate(int quoted, int total) {
		if (total == 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(quoted).divide(BigDecimal.valueOf(total), DiagnosticMath.MATH);
	}
}
