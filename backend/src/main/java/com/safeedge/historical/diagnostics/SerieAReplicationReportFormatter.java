package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.evaluation.HistoricalBacktestReportFormatter;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.WalkForwardBuildStats;
import com.safeedge.probability.ProbabilityModelConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Baseline 004 Serie A replication markdown. Zero-tuning. Not a betting
 * recommendation.
 */
public final class SerieAReplicationReportFormatter {

	private SerieAReplicationReportFormatter() {
	}

	public static String format(
			LeagueCoverageSnapshot coverage,
			BaselineDiagnosticsReport baseline,
			EdgeQualityReport edgeQuality) {
		return format(coverage, baseline, edgeQuality, null);
	}

	public static String format(
			LeagueCoverageSnapshot coverage,
			BaselineDiagnosticsReport baseline,
			EdgeQualityReport edgeQuality,
			HistoricalStrategyComparisonResult comparison) {
		if (coverage == null || baseline == null || edgeQuality == null) {
			throw new IllegalArgumentException("coverage, baseline, and edgeQuality are required");
		}
		StringBuilder text = new StringBuilder();
		text.append("# Baseline 004 – Serie A Replication").append('\n').append('\n');
		appendConfig(text, edgeQuality.datasetStats());
		appendCoverage(text, coverage, edgeQuality.datasetStats());
		BaselineDiagnosticsReportFormatter.appendGoalsAndMargins(text, baseline);
		text.append('\n');
		text.append("The following sections reuse Baseline 002 edge-quality definitions on this Serie A dataset.")
				.append('\n')
				.append('\n');
		EdgeQualityReportFormatter.appendAfterConfig(text, edgeQuality);
		if (comparison != null) {
			text.append('\n');
			text.append("## Strategy comparison (unchanged presets)").append('\n').append('\n');
			text.append("Same StrategyPresetFactory configs and starting bankroll 100000. Not a recommended stake plan.")
					.append('\n')
					.append('\n');
			text.append("```").append('\n');
			text.append(HistoricalBacktestReportFormatter.format(comparison).trim()).append('\n');
			text.append("```").append('\n').append('\n');
		}
		return text.toString();
	}

	private static void appendConfig(StringBuilder text, WalkForwardBuildStats stats) {
		ProbabilityModelConfig model = ProbabilityModelConfig.defaults();
		text.append("## Experiment configuration").append('\n').append('\n');
		text.append("- Competition: ").append(stats.competition()).append('\n');
		text.append("- Training from season: ").append(stats.trainingFromSeason()).append('\n');
		text.append("- Evaluation range: ")
				.append(stats.evaluationFromSeason())
				.append(" → ")
				.append(stats.evaluationToSeason())
				.append('\n');
		text.append("- HISTORICAL QUOTE SOURCE = ").append(stats.quoteSource()).append('\n');
		text.append("- These prices are football-data.co.uk historical quotes, not Tippmix odds.").append('\n');
		text.append("- Model: current independent time-decayed Poisson defaults (not retuned).").append('\n');
		text.append("- decayHalfLifeDays = ").append(model.decayHalfLifeDays()).append('\n');
		text.append("- maxGoalsPerTeam = ").append(model.maxGoalsPerTeam()).append('\n');
		text.append("- minimumTeamMatches = ").append(model.minimumTeamMatches()).append('\n');
		text.append("- No Serie-A-specific home-advantage parameter.").append('\n');
		text.append("- Zero-tuning replication of Premier League Baseline 001/002 and Bundesliga Baseline 003.")
				.append('\n');
		text.append("- Strategies: unchanged DEFENSIVE / BALANCED / GROWTH / FLAT_STAKE.").append('\n');
		text.append("- Starting bankroll: 100000 (simulation capital only).").append('\n');
		if (stats.quoteSource() == HistoricalQuoteSource.MARKET_AVERAGE) {
			text.append("- MARKET_AVERAGE is not Tippmix.").append('\n');
		}
		text.append('\n');
	}

	private static void appendCoverage(
			StringBuilder text, LeagueCoverageSnapshot coverage, WalkForwardBuildStats stats) {
		text.append("## Historical coverage").append('\n').append('\n');
		text.append("Persisted football-data.co.uk rows. Not re-imported. Evaluation years were not altered.")
				.append('\n')
				.append('\n');
		text.append("- Warmup seasons expected: ");
		appendYears(text, coverage.expectedWarmupStartYears());
		text.append('\n');
		text.append("- Evaluation seasons expected: ");
		appendYears(text, coverage.expectedEvaluationStartYears());
		text.append('\n');
		text.append("- Missing warmup seasons: ")
				.append(formatMissing(coverage.missingWarmupStartYears()))
				.append('\n');
		text.append("- Missing evaluation seasons: ")
				.append(formatMissing(coverage.missingEvaluationStartYears()))
				.append('\n');
		text.append("- Missing evaluation season count: ")
				.append(coverage.missingEvaluationStartYears().size())
				.append('\n');
		text.append("- Warmup match count: ").append(coverage.warmupMatchCount()).append('\n');
		text.append("- Evaluation match count: ").append(coverage.evaluationMatchCount()).append('\n');
		text.append("- Evaluation matches with ")
				.append(coverage.quoteSource())
				.append(": ")
				.append(coverage.evaluationMatchesWithSelectedQuote())
				.append('\n');
		text.append("- Evaluation matches missing ")
				.append(coverage.quoteSource())
				.append(": ")
				.append(coverage.evaluationMatchesMissingSelectedQuote())
				.append('\n');
		text.append("- Walk-forward matches skipped missing quote: ")
				.append(stats.matchesSkippedMissingQuote())
				.append('\n');
		if (coverage.evaluationSeasonsComplete() && coverage.warmupSeasonsComplete()) {
			text.append("- All expected Serie A seasons are present, including 2021/22.")
					.append('\n');
		}
		text.append('\n');
		text.append("| Season | Role | Matches | ")
				.append(coverage.quoteSource())
				.append(" quotes | Coverage |")
				.append('\n');
		text.append("| --- | --- | ---: | ---: | ---: |").append('\n');
		for (LeagueSeasonCoverageRow row : coverage.seasons()) {
			text.append("| ")
					.append(row.seasonDisplay())
					.append(" | ")
					.append(role(row))
					.append(" | ")
					.append(row.totalMatches())
					.append(" | ")
					.append(row.matchesWithSelectedQuote())
					.append(" | ")
					.append(decimal(row.selectedQuoteCoverageRate()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
		text.append("Warmup seasons may have scores without AH quotes. Poisson training uses match results, not quotes.")
				.append('\n')
				.append('\n');
	}

	private static String role(LeagueSeasonCoverageRow row) {
		if (row.warmupHistory() && row.evaluationWindow()) {
			return "warmup+eval";
		}
		if (row.warmupHistory()) {
			return "warmup";
		}
		if (row.evaluationWindow()) {
			return "evaluation";
		}
		return "other";
	}

	private static void appendYears(StringBuilder text, List<Integer> years) {
		for (int i = 0; i < years.size(); i++) {
			if (i > 0) {
				text.append(", ");
			}
			text.append(years.get(i)).append('/').append(String.valueOf(years.get(i) + 1).substring(2));
		}
	}

	private static String formatMissing(List<Integer> years) {
		if (years.isEmpty()) {
			return "none";
		}
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < years.size(); i++) {
			if (i > 0) {
				text.append(", ");
			}
			text.append(years.get(i)).append('/').append(String.valueOf(years.get(i) + 1).substring(2));
		}
		return text.toString();
	}

	private static String decimal(BigDecimal value) {
		if (value == null) {
			return "n/a";
		}
		return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
