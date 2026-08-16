package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.probability.ProbabilityModelConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Three-league structural validation markdown. Does not pick a league to bet.
 */
public final class ThreeLeagueValidationReportFormatter {

	private ThreeLeagueValidationReportFormatter() {
	}

	public static String format(ThreeLeagueComparison comparison) {
		if (comparison == null) {
			throw new IllegalArgumentException("comparison is required");
		}
		LeagueDiagnosticSnapshot pl = comparison.premierLeague();
		LeagueDiagnosticSnapshot bl = comparison.bundesliga();
		LeagueDiagnosticSnapshot sa = comparison.serieA();
		StringBuilder text = new StringBuilder();
		text.append("# Baseline 004 – Three-League Structural Validation").append('\n').append('\n');
		appendConfig(text);
		appendDataset(text, pl, bl, sa);
		appendAggregateCalibration(text, pl, bl, sa);
		appendRanking(text, pl, bl, sa);
		appendHighEdge(text, pl, bl, sa);
		appendGoalCalibration(text, pl, bl, sa);
		appendFamilies(text, pl, bl, sa);
		appendStrategies(text, pl, bl, sa);
		appendIntervals(text, pl, bl, sa);
		appendStructuralTest(text, comparison);
		appendClassification(text, comparison);
		appendNonConclusions(text, comparison);
		return text.toString();
	}

	private static void appendConfig(StringBuilder text) {
		ProbabilityModelConfig model = ProbabilityModelConfig.defaults();
		text.append("## Experiment configuration").append('\n').append('\n');
		text.append("- Leagues: PREMIER_LEAGUE (published Baseline 001/002), BUNDESLIGA (published Baseline 003), SERIE_A (this replication).")
				.append('\n');
		text.append("- Training from season: 2014").append('\n');
		text.append("- Evaluation range: 2019 → 2023").append('\n');
		text.append("- HISTORICAL QUOTE SOURCE = MARKET_AVERAGE").append('\n');
		text.append("- These prices are football-data.co.uk historical quotes, not Tippmix odds.").append('\n');
		text.append("- Model: independent time-decayed Poisson defaults (not retuned).").append('\n');
		text.append("- decayHalfLifeDays = ").append(model.decayHalfLifeDays()).append('\n');
		text.append("- maxGoalsPerTeam = ").append(model.maxGoalsPerTeam()).append('\n');
		text.append("- minimumTeamMatches = ").append(model.minimumTeamMatches()).append('\n');
		text.append("- No league-specific tuning. No production filter. Zero-tuning replication.").append('\n');
		text.append("- Premier League and Bundesliga numbers are published baselines, not reruns.")
				.append('\n')
				.append('\n');
	}

	private static void appendDataset(
			StringBuilder text,
			LeagueDiagnosticSnapshot pl,
			LeagueDiagnosticSnapshot bl,
			LeagueDiagnosticSnapshot sa) {
		text.append("## Dataset").append('\n').append('\n');
		text.append("| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |").append('\n');
		text.append("| --- | ---: | ---: | ---: |").append('\n');
		row(text, "Predictions", pl.predictionQuality().predictionsAvailable(), bl.predictionQuality().predictionsAvailable(), sa.predictionQuality().predictionsAvailable());
		row(text, "Candidates", pl.candidateCount(), bl.candidateCount(), sa.candidateCount());
		row(text, "Matches evaluated", pl.matchesEvaluated(), bl.matchesEvaluated(), sa.matchesEvaluated());
		row(text, "Matches skipped missing quote", pl.matchesSkippedMissingQuote(), bl.matchesSkippedMissingQuote(), sa.matchesSkippedMissingQuote());
		text.append("| Evaluation seasons present | ")
				.append(presentSeasons(pl))
				.append(" | ")
				.append(presentSeasons(bl))
				.append(" | ")
				.append(presentSeasons(sa))
				.append(" |")
				.append('\n');
		text.append("| Missing evaluation seasons | ")
				.append(missing(pl.missingEvaluationStartYears()))
				.append(" | ")
				.append(missing(bl.missingEvaluationStartYears()))
				.append(" | ")
				.append(missing(sa.missingEvaluationStartYears()))
				.append(" |")
				.append('\n')
				.append('\n');
	}

	private static void appendAggregateCalibration(
			StringBuilder text,
			LeagueDiagnosticSnapshot pl,
			LeagueDiagnosticSnapshot bl,
			LeagueDiagnosticSnapshot sa) {
		text.append("## Aggregate calibration").append('\n').append('\n');
		text.append("| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |").append('\n');
		text.append("| --- | ---: | ---: | ---: |").append('\n');
		row(text, "Average predicted edge", pl.averagePredictedEdge(), bl.averagePredictedEdge(), sa.averagePredictedEdge());
		row(text, "Realized unit ROI", pl.averageRealizedReturn(), bl.averageRealizedReturn(), sa.averageRealizedReturn());
		row(text, "Calibration gap", pl.calibrationGap(), bl.calibrationGap(), sa.calibrationGap());
		text.append('\n');
	}

	private static void appendRanking(
			StringBuilder text,
			LeagueDiagnosticSnapshot pl,
			LeagueDiagnosticSnapshot bl,
			LeagueDiagnosticSnapshot sa) {
		text.append("## Ranking").append('\n').append('\n');
		text.append("| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |").append('\n');
		text.append("| --- | ---: | ---: | ---: |").append('\n');
		row(text, "Spearman", pl.spearman(), bl.spearman(), sa.spearman());
		row(text, "Pearson", pl.pearson(), bl.pearson(), sa.pearson());
		text.append('\n');
		text.append("Single-bet realized return is noisy. Correlation is diagnostic, not proof.")
				.append('\n')
				.append('\n');
	}

	private static void appendHighEdge(
			StringBuilder text,
			LeagueDiagnosticSnapshot pl,
			LeagueDiagnosticSnapshot bl,
			LeagueDiagnosticSnapshot sa) {
		appendHighEdgeThreshold(text, "## High edge >=10%", new BigDecimal("0.10"), pl, bl, sa);
		appendHighEdgeThreshold(text, "## High edge >=20%", new BigDecimal("0.20"), pl, bl, sa);
	}

	private static void appendHighEdgeThreshold(
			StringBuilder text,
			String title,
			BigDecimal threshold,
			LeagueDiagnosticSnapshot pl,
			LeagueDiagnosticSnapshot bl,
			LeagueDiagnosticSnapshot sa) {
		text.append(title).append('\n').append('\n');
		text.append("| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |").append('\n');
		text.append("| --- | ---: | ---: | ---: |").append('\n');
		HighEdgeCalibrationSlice p = slice(pl, threshold);
		HighEdgeCalibrationSlice b = slice(bl, threshold);
		HighEdgeCalibrationSlice s = slice(sa, threshold);
		row(text, "n", n(p), n(b), n(s));
		row(text, "Avg edge", edge(p), edge(b), edge(s));
		row(text, "ROI", roi(p), roi(b), roi(s));
		row(text, "Predicted P(WIN)", winPred(p), winPred(b), winPred(s));
		row(text, "Actual P(WIN)", winAct(p), winAct(b), winAct(s));
		row(text, "Predicted P(LOSS)", lossPred(p), lossPred(b), lossPred(s));
		row(text, "Actual P(LOSS)", lossAct(p), lossAct(b), lossAct(s));
		text.append('\n');
	}

	private static void appendGoalCalibration(
			StringBuilder text,
			LeagueDiagnosticSnapshot pl,
			LeagueDiagnosticSnapshot bl,
			LeagueDiagnosticSnapshot sa) {
		text.append("## Goal calibration").append('\n').append('\n');
		text.append("| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |").append('\n');
		text.append("| --- | ---: | ---: | ---: |").append('\n');
		PredictionQualitySnapshot p = pl.predictionQuality();
		PredictionQualitySnapshot b = bl.predictionQuality();
		PredictionQualitySnapshot s = sa.predictionQuality();
		row(text, "Home goals predicted", p.predictedHomeGoals(), b.predictedHomeGoals(), s.predictedHomeGoals());
		row(text, "Home goals actual", p.actualHomeGoals(), b.actualHomeGoals(), s.actualHomeGoals());
		row(text, "Away goals predicted", p.predictedAwayGoals(), b.predictedAwayGoals(), s.predictedAwayGoals());
		row(text, "Away goals actual", p.actualAwayGoals(), b.actualAwayGoals(), s.actualAwayGoals());
		row(text, "1X2 HOME predicted", p.predictedHomeWin(), b.predictedHomeWin(), s.predictedHomeWin());
		row(text, "1X2 HOME actual", p.actualHomeWin(), b.actualHomeWin(), s.actualHomeWin());
		row(text, "1X2 DRAW predicted", p.predictedDraw(), b.predictedDraw(), s.predictedDraw());
		row(text, "1X2 DRAW actual", p.actualDraw(), b.actualDraw(), s.actualDraw());
		row(text, "1X2 AWAY predicted", p.predictedAwayWin(), b.predictedAwayWin(), s.predictedAwayWin());
		row(text, "1X2 AWAY actual", p.actualAwayWin(), b.actualAwayWin(), s.actualAwayWin());
		text.append('\n');
	}

	private static void appendFamilies(
			StringBuilder text,
			LeagueDiagnosticSnapshot pl,
			LeagueDiagnosticSnapshot bl,
			LeagueDiagnosticSnapshot sa) {
		text.append("## AH families").append('\n').append('\n');
		text.append("| Family | PL ROI | BL ROI | SA ROI |").append('\n');
		text.append("| --- | ---: | ---: | ---: |").append('\n');
		for (DiagnosticLineFamily family : DiagnosticLineFamily.values()) {
			text.append("| ")
					.append(family.name())
					.append(" | ")
					.append(decimal(familyRoi(pl, family)))
					.append(" | ")
					.append(decimal(familyRoi(bl, family)))
					.append(" | ")
					.append(decimal(familyRoi(sa, family)))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
		text.append("Repeated family ordering is not a production filter.").append('\n').append('\n');
	}

	private static void appendStrategies(
			StringBuilder text,
			LeagueDiagnosticSnapshot pl,
			LeagueDiagnosticSnapshot bl,
			LeagueDiagnosticSnapshot sa) {
		text.append("## Strategy results").append('\n').append('\n');
		text.append("Unchanged StrategyPresetFactory configs. Not a league-selection exercise.").append('\n').append('\n');
		text.append("| Strategy | PL bets | PL ROI | PL paused | BL bets | BL ROI | BL paused | SA bets | SA ROI | SA paused |")
				.append('\n');
		text.append("| --- | ---: | ---: | --- | ---: | ---: | --- | ---: | ---: | --- |").append('\n');
		for (String name : List.of("DEFENSIVE", "BALANCED", "GROWTH", "FLAT_STAKE")) {
			StrategyRegressionSnapshot left = strategy(pl, name);
			StrategyRegressionSnapshot mid = strategy(bl, name);
			StrategyRegressionSnapshot right = strategy(sa, name);
			text.append("| ")
					.append(name)
					.append(" | ")
					.append(left == null ? "n/a" : left.betsAccepted())
					.append(" | ")
					.append(decimal(left == null ? null : left.roi()))
					.append(" | ")
					.append(left == null ? "n/a" : left.pausedByDrawdown())
					.append(" | ")
					.append(mid == null ? "n/a" : mid.betsAccepted())
					.append(" | ")
					.append(decimal(mid == null ? null : mid.roi()))
					.append(" | ")
					.append(mid == null ? "n/a" : mid.pausedByDrawdown())
					.append(" | ")
					.append(right == null ? "n/a" : right.betsAccepted())
					.append(" | ")
					.append(decimal(right == null ? null : right.roi()))
					.append(" | ")
					.append(right == null ? "n/a" : right.pausedByDrawdown())
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendIntervals(
			StringBuilder text,
			LeagueDiagnosticSnapshot pl,
			LeagueDiagnosticSnapshot bl,
			LeagueDiagnosticSnapshot sa) {
		text.append("## Statistical uncertainty").append('\n').append('\n');
		text.append("Deterministic bootstrap seed=20260816, replicates=2000. CI excluding 0 is not proof of future profitability.")
				.append('\n')
				.append('\n');
		text.append("| Group | PL mean | PL 95% | BL mean | BL 95% | SA mean | SA 95% |").append('\n');
		text.append("| --- | ---: | --- | ---: | --- | ---: | --- |").append('\n');
		for (String label : List.of(
				"all candidates", "positive-edge", "edge >= 0.10", "NEGATIVE_HANDICAP", "POSITIVE_HANDICAP")) {
			NamedMeanInterval left = interval(pl, label);
			NamedMeanInterval mid = interval(bl, label);
			NamedMeanInterval right = interval(sa, label);
			text.append("| ")
					.append(label)
					.append(" | ")
					.append(decimal(left == null ? null : left.interval().mean()))
					.append(" | ")
					.append(ci(left))
					.append(" | ")
					.append(decimal(mid == null ? null : mid.interval().mean()))
					.append(" | ")
					.append(ci(mid))
					.append(" | ")
					.append(decimal(right == null ? null : right.interval().mean()))
					.append(" | ")
					.append(ci(right))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendStructuralTest(StringBuilder text, ThreeLeagueComparison comparison) {
		text.append("## Structural-error test").append('\n').append('\n');
		text.append("Diagnostic cutoffs (not production filters): |1X2 gap| ≤ 0.03; |goal gap| ≤ 0.15; |edge−return gap| ≤ 0.01; |Spearman| < 0.10; high-edge ≥10% and ≥20% have predicted WIN above actual and predicted LOSS below actual; edge buckets with n≥30 are not monotone in ROI; ≥3 populated seasons have negative unit ROI and ≥2 populated ≥10%-edge seasons have negative ≥10% ROI.")
				.append('\n')
				.append('\n');
		text.append("| Pattern | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |").append('\n');
		text.append("| --- | --- | --- | --- |").append('\n');
		StructuralPatternFlags pl = comparison.premierLeagueFlags();
		StructuralPatternFlags bl = comparison.bundesligaFlags();
		StructuralPatternFlags sa = comparison.serieAFlags();
		flagRow(text, "A) aggregate goals / 1X2 reasonably calibrated", pl.aggregateGoalsAndMatchResultCalibrated(), bl.aggregateGoalsAndMatchResultCalibrated(), sa.aggregateGoalsAndMatchResultCalibrated());
		flagRow(text, "B) aggregate predicted edge near realized return", pl.aggregateEdgeNearRealizedReturn(), bl.aggregateEdgeNearRealizedReturn(), sa.aggregateEdgeNearRealizedReturn());
		flagRow(text, "C) edge ranking near zero / weak", pl.edgeRankingWeak(), bl.edgeRankingWeak(), sa.edgeRankingWeak());
		flagRow(text, "D) high predicted edge: P(WIN) too high, P(LOSS) too low", pl.highEdgeWinOverconfidentAndLossUnderconfident(), bl.highEdgeWinOverconfidentAndLossUnderconfident(), sa.highEdgeWinOverconfidentAndLossUnderconfident());
		flagRow(text, "E) higher edge does not monotonically improve ROI", pl.higherEdgeDoesNotMonotonicallyImproveRoi(), bl.higherEdgeDoesNotMonotonicallyImproveRoi(), sa.higherEdgeDoesNotMonotonicallyImproveRoi());
		flagRow(text, "F) failure appears across multiple seasons", pl.failureStableAcrossSeasons(), bl.failureStableAcrossSeasons(), sa.failureStableAcrossSeasons());
		text.append('\n');
		text.append("Three leagues are strong evidence, not mathematical proof.").append('\n').append('\n');
	}

	private static void appendClassification(StringBuilder text, ThreeLeagueComparison comparison) {
		text.append("## Classification").append('\n').append('\n');
		text.append("**").append(comparison.classification().name().replace('_', ' ')).append("**").append('\n').append('\n');
		switch (comparison.classification()) {
			case FAILURE_STRONGLY_REPLICATES_AGAIN -> text.append(
					"Serie A reproduces the same AH edge-ranking limitation seen in Premier League and Bundesliga.")
					.append('\n');
			case FAILURE_STRONGLY_REPLICATES -> text.append(
					"Two-league strong replication wording is retained for the published Baseline 003 check; this three-league report uses FAILURE STRONGLY REPLICATES AGAIN when the third league matches.")
					.append('\n');
			case FAILURE_PARTIALLY_REPLICATES -> text.append(
					"Some edge-quality problems appear in Serie A, but not the full three-league pattern.")
					.append('\n');
			case FAILURE_DOES_NOT_REPLICATE -> text.append(
					"Serie A materially differs; the earlier issue may be sample/league specific.")
					.append('\n');
		}
		text.append('\n');
		if (comparison.classification() == StructuralReplicationClassification.FAILURE_STRONGLY_REPLICATES_AGAIN) {
			text.append("The independent Poisson baseline has now shown the same AH edge-ranking limitation across three major leagues. Further same-model league replications are low-value; the next justified experiment is a better probability model.")
					.append('\n')
					.append('\n');
			text.append("### NEXT HYPOTHESIS").append('\n').append('\n');
			text.append("Possible later experiments (not implemented, not chosen here): Dixon-Coles dependence correction; attack/defence strength shrinkage; richer strength model; Elo; xG.")
					.append('\n')
					.append('\n');
		}
	}

	private static void appendNonConclusions(StringBuilder text, ThreeLeagueComparison comparison) {
		text.append("## Explicit non-conclusions").append('\n').append('\n');
		text.append("- no parameter optimization performed").append('\n');
		text.append("- no production filter selected").append('\n');
		text.append("- Serie A is not selected as a betting venue because of this comparison").append('\n');
		text.append("- best-looking cell is not a validated strategy").append('\n');
		text.append("- three leagues do not prove a theorem").append('\n');
		text.append("- CI excluding 0 is not proof of future profitability").append('\n');
		text.append("- MARKET_AVERAGE is not Tippmix").append('\n');
		text.append("- 1/odds is not true AH probability").append('\n');
		if (comparison.premierLeague().quoteSource() == HistoricalQuoteSource.MARKET_AVERAGE
				&& comparison.serieA().competition() == CanonicalCompetition.SERIE_A) {
			text.append("- football-data.co.uk historical quotes only").append('\n');
		}
		text.append('\n');
	}

	private static void flagRow(StringBuilder text, String label, boolean left, boolean mid, boolean right) {
		text.append("| ")
				.append(label)
				.append(" | ")
				.append(left ? "yes" : "no")
				.append(" | ")
				.append(mid ? "yes" : "no")
				.append(" | ")
				.append(right ? "yes" : "no")
				.append(" |")
				.append('\n');
	}

	private static void row(StringBuilder text, String metric, int left, int mid, int right) {
		text.append("| ")
				.append(metric)
				.append(" | ")
				.append(left)
				.append(" | ")
				.append(mid)
				.append(" | ")
				.append(right)
				.append(" |")
				.append('\n');
	}

	private static void row(StringBuilder text, String metric, BigDecimal left, BigDecimal mid, BigDecimal right) {
		text.append("| ")
				.append(metric)
				.append(" | ")
				.append(decimal(left))
				.append(" | ")
				.append(decimal(mid))
				.append(" | ")
				.append(decimal(right))
				.append(" |")
				.append('\n');
	}

	private static String presentSeasons(LeagueDiagnosticSnapshot league) {
		if (league.seasons().isEmpty()) {
			return "n/a";
		}
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < league.seasons().size(); i++) {
			if (i > 0) {
				text.append(", ");
			}
			text.append(league.seasons().get(i).seasonDisplay());
		}
		return text.toString();
	}

	private static HighEdgeCalibrationSlice slice(LeagueDiagnosticSnapshot league, BigDecimal threshold) {
		for (HighEdgeCalibrationSlice row : league.highEdgeSlices()) {
			if (row.threshold().compareTo(threshold) == 0) {
				return row;
			}
		}
		return null;
	}

	private static BigDecimal familyRoi(LeagueDiagnosticSnapshot league, DiagnosticLineFamily family) {
		for (AhFamilySnapshot row : league.ahFamilies()) {
			if (row.family() == family) {
				return row.unitStakeRoi();
			}
		}
		return null;
	}

	private static StrategyRegressionSnapshot strategy(LeagueDiagnosticSnapshot league, String name) {
		for (StrategyRegressionSnapshot row : league.strategies()) {
			if (name.equals(row.name())) {
				return row;
			}
		}
		return null;
	}

	private static NamedMeanInterval interval(LeagueDiagnosticSnapshot league, String label) {
		for (NamedMeanInterval row : league.confidenceIntervals()) {
			if (label.equals(row.label())) {
				return row;
			}
		}
		return null;
	}

	private static String missing(List<Integer> years) {
		if (years == null || years.isEmpty()) {
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

	private static int n(HighEdgeCalibrationSlice slice) {
		return slice == null ? 0 : slice.n();
	}

	private static BigDecimal edge(HighEdgeCalibrationSlice slice) {
		return slice == null ? null : slice.averageEdge();
	}

	private static BigDecimal roi(HighEdgeCalibrationSlice slice) {
		return slice == null ? null : slice.unitStakeRoi();
	}

	private static BigDecimal winPred(HighEdgeCalibrationSlice slice) {
		return slice == null ? null : slice.predictedWinProbability();
	}

	private static BigDecimal winAct(HighEdgeCalibrationSlice slice) {
		return slice == null ? null : slice.actualWinFrequency();
	}

	private static BigDecimal lossPred(HighEdgeCalibrationSlice slice) {
		return slice == null ? null : slice.predictedLossProbability();
	}

	private static BigDecimal lossAct(HighEdgeCalibrationSlice slice) {
		return slice == null ? null : slice.actualLossFrequency();
	}

	private static String ci(NamedMeanInterval interval) {
		if (interval == null) {
			return "n/a";
		}
		return "[" + decimal(interval.interval().lower95()) + ", " + decimal(interval.interval().upper95()) + "]";
	}

	private static String decimal(BigDecimal value) {
		if (value == null) {
			return "n/a";
		}
		return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
