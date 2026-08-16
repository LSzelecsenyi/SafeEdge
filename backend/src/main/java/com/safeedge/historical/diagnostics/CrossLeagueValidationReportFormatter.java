package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.probability.ProbabilityModelConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Cross-league validation markdown. Does not pick a better league to bet.
 */
public final class CrossLeagueValidationReportFormatter {

	private CrossLeagueValidationReportFormatter() {
	}

	public static String format(CrossLeagueComparison comparison) {
		if (comparison == null) {
			throw new IllegalArgumentException("comparison is required");
		}
		LeagueDiagnosticSnapshot pl = comparison.premierLeague();
		LeagueDiagnosticSnapshot bl = comparison.bundesliga();
		StringBuilder text = new StringBuilder();
		text.append("# Baseline 003 – Cross-League Structural Validation").append('\n').append('\n');
		appendConfig(text);
		appendPredictionQuality(text, pl, bl);
		appendEdgeQuality(text, pl, bl);
		appendHighEdge(text, pl, bl);
		appendFamilies(text, pl, bl);
		appendSides(text, pl, bl);
		appendSeasons(text, pl, bl);
		appendOverround(text, pl, bl);
		appendStrategies(text, pl, bl);
		appendIntervals(text, pl, bl);
		appendTop30(text, pl, bl);
		appendStructuralTest(text, comparison);
		appendClassification(text, comparison);
		appendNonConclusions(text, comparison);
		return text.toString();
	}

	private static void appendConfig(StringBuilder text) {
		ProbabilityModelConfig model = ProbabilityModelConfig.defaults();
		text.append("## Experiment configuration").append('\n').append('\n');
		text.append("- Leagues: PREMIER_LEAGUE (published Baseline 001/002) and BUNDESLIGA (this replication).")
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
		text.append("- Premier League numbers are the published Baseline 001/002 results, not a rerun.")
				.append('\n')
				.append('\n');
	}

	private static void appendPredictionQuality(
			StringBuilder text, LeagueDiagnosticSnapshot pl, LeagueDiagnosticSnapshot bl) {
		text.append("## Prediction quality").append('\n').append('\n');
		text.append("| Metric | PREMIER_LEAGUE | BUNDESLIGA |").append('\n');
		text.append("| --- | ---: | ---: |").append('\n');
		PredictionQualitySnapshot p = pl.predictionQuality();
		PredictionQualitySnapshot b = bl.predictionQuality();
		row(text, "Predictions available", p.predictionsAvailable(), b.predictionsAvailable());
		row(text, "Average actual-score log loss", p.averageActualScoreLogLoss(), b.averageActualScoreLogLoss());
		row(text, "Predicted home goals", p.predictedHomeGoals(), b.predictedHomeGoals());
		row(text, "Actual home goals", p.actualHomeGoals(), b.actualHomeGoals());
		row(text, "Predicted away goals", p.predictedAwayGoals(), b.predictedAwayGoals());
		row(text, "Actual away goals", p.actualAwayGoals(), b.actualAwayGoals());
		row(text, "Predicted home win", p.predictedHomeWin(), b.predictedHomeWin());
		row(text, "Actual home win", p.actualHomeWin(), b.actualHomeWin());
		row(text, "Predicted draw", p.predictedDraw(), b.predictedDraw());
		row(text, "Actual draw", p.actualDraw(), b.actualDraw());
		row(text, "Predicted away win", p.predictedAwayWin(), b.predictedAwayWin());
		row(text, "Actual away win", p.actualAwayWin(), b.actualAwayWin());
		text.append('\n');
		text.append("Margin categories use the same Baseline 001 buckets.").append('\n').append('\n');
		text.append("| Margin | PL predicted | PL actual | BL predicted | BL actual |").append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: |").append('\n');
		for (DiagnosticMarginCategory category : DiagnosticMarginCategory.values()) {
			MarginCategoryCalibration plRow = margin(p, category);
			MarginCategoryCalibration blRow = margin(b, category);
			text.append("| ")
					.append(category.name())
					.append(" | ")
					.append(decimal(plRow == null ? null : plRow.averagePredictedProbability()))
					.append(" | ")
					.append(decimal(plRow == null ? null : plRow.actualFrequency()))
					.append(" | ")
					.append(decimal(blRow == null ? null : blRow.averagePredictedProbability()))
					.append(" | ")
					.append(decimal(blRow == null ? null : blRow.actualFrequency()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendEdgeQuality(
			StringBuilder text, LeagueDiagnosticSnapshot pl, LeagueDiagnosticSnapshot bl) {
		text.append("## Edge quality").append('\n').append('\n');
		text.append("| Metric | PREMIER_LEAGUE | BUNDESLIGA |").append('\n');
		text.append("| --- | ---: | ---: |").append('\n');
		row(text, "Candidate count", pl.candidateCount(), bl.candidateCount());
		row(text, "+EV count", pl.positiveEvCount(), bl.positiveEvCount());
		row(text, "−EV count", pl.negativeEvCount(), bl.negativeEvCount());
		row(text, "Average predicted edge", pl.averagePredictedEdge(), bl.averagePredictedEdge());
		row(text, "Average realized unit return", pl.averageRealizedReturn(), bl.averageRealizedReturn());
		row(text, "Calibration gap", pl.calibrationGap(), bl.calibrationGap());
		row(text, "Spearman(edge, realized return)", pl.spearman(), bl.spearman());
		row(text, "Pearson(edge, realized return)", pl.pearson(), bl.pearson());
		text.append('\n');
		text.append("Single-bet realized return is noisy. Correlation is diagnostic, not proof.")
				.append('\n')
				.append('\n');
		text.append("### Edge deciles").append('\n').append('\n');
		text.append("| Decile | PL n | PL avg edge | PL ROI | BL n | BL avg edge | BL ROI |").append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: |").append('\n');
		int decileCount = Math.max(pl.edgeDeciles().size(), bl.edgeDeciles().size());
		for (int i = 0; i < decileCount; i++) {
			BucketTrendRow left = i < pl.edgeDeciles().size() ? pl.edgeDeciles().get(i) : null;
			BucketTrendRow right = i < bl.edgeDeciles().size() ? bl.edgeDeciles().get(i) : null;
			text.append("| ")
					.append(i + 1)
					.append(" | ")
					.append(n(left))
					.append(" | ")
					.append(decimal(left == null ? null : left.averageEdge()))
					.append(" | ")
					.append(decimal(left == null ? null : left.unitStakeRoi()))
					.append(" | ")
					.append(n(right))
					.append(" | ")
					.append(decimal(right == null ? null : right.averageEdge()))
					.append(" | ")
					.append(decimal(right == null ? null : right.unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendHighEdge(
			StringBuilder text, LeagueDiagnosticSnapshot pl, LeagueDiagnosticSnapshot bl) {
		text.append("## High-edge calibration").append('\n').append('\n');
		text.append("| League | Threshold | n | Avg edge | ROI | P(WIN) pred | WIN actual | P(LOSS) pred | LOSS actual |")
				.append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |").append('\n');
		appendHighEdgeLeague(text, "PREMIER_LEAGUE", pl.highEdgeSlices());
		appendHighEdgeLeague(text, "BUNDESLIGA", bl.highEdgeSlices());
		text.append('\n');
	}

	private static void appendHighEdgeLeague(StringBuilder text, String league, List<HighEdgeCalibrationSlice> slices) {
		for (HighEdgeCalibrationSlice slice : slices) {
			if (slice.threshold().compareTo(new BigDecimal("0.40")) >= 0) {
				continue;
			}
			text.append("| ")
					.append(league)
					.append(" | ")
					.append(decimal(slice.threshold()))
					.append(" | ")
					.append(slice.n())
					.append(" | ")
					.append(decimal(slice.averageEdge()))
					.append(" | ")
					.append(decimal(slice.unitStakeRoi()))
					.append(" | ")
					.append(decimal(slice.predictedWinProbability()))
					.append(" | ")
					.append(decimal(slice.actualWinFrequency()))
					.append(" | ")
					.append(decimal(slice.predictedLossProbability()))
					.append(" | ")
					.append(decimal(slice.actualLossFrequency()))
					.append(" |")
					.append('\n');
		}
	}

	private static void appendFamilies(
			StringBuilder text, LeagueDiagnosticSnapshot pl, LeagueDiagnosticSnapshot bl) {
		text.append("## AH family").append('\n').append('\n');
		text.append("| Family | PL n | PL avg edge | PL ROI | BL n | BL avg edge | BL ROI |").append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: |").append('\n');
		for (DiagnosticLineFamily family : DiagnosticLineFamily.values()) {
			AhFamilySnapshot left = family(pl, family);
			AhFamilySnapshot right = family(bl, family);
			text.append("| ")
					.append(family.name())
					.append(" | ")
					.append(left == null ? "n/a" : left.n())
					.append(" | ")
					.append(decimal(left == null ? null : left.averageEdge()))
					.append(" | ")
					.append(decimal(left == null ? null : left.unitStakeRoi()))
					.append(" | ")
					.append(right == null ? "n/a" : right.n())
					.append(" | ")
					.append(decimal(right == null ? null : right.averageEdge()))
					.append(" | ")
					.append(decimal(right == null ? null : right.unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendSides(StringBuilder text, LeagueDiagnosticSnapshot pl, LeagueDiagnosticSnapshot bl) {
		text.append("## HOME / AWAY").append('\n').append('\n');
		text.append("| Side | PL n | PL ROI | BL n | BL ROI |").append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: |").append('\n');
		for (String side : List.of("HOME", "AWAY")) {
			SideSnapshot left = side(pl, side);
			SideSnapshot right = side(bl, side);
			text.append("| ")
					.append(side)
					.append(" | ")
					.append(left == null ? "n/a" : left.n())
					.append(" | ")
					.append(decimal(left == null ? null : left.unitStakeRoi()))
					.append(" | ")
					.append(right == null ? "n/a" : right.n())
					.append(" | ")
					.append(decimal(right == null ? null : right.unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendSeasons(StringBuilder text, LeagueDiagnosticSnapshot pl, LeagueDiagnosticSnapshot bl) {
		text.append("## Season stability").append('\n').append('\n');
		text.append("- Premier League missing evaluation seasons: ")
				.append(missing(pl.missingEvaluationStartYears()))
				.append('\n');
		text.append("- Bundesliga missing evaluation seasons: ")
				.append(missing(bl.missingEvaluationStartYears()))
				.append('\n')
				.append('\n');
		if (!pl.missingEvaluationStartYears().isEmpty() && bl.missingEvaluationStartYears().isEmpty()) {
			text.append("Bundesliga includes 2021/22; Premier League does not. Do not pool first and infer stability.")
					.append('\n')
					.append('\n');
		}
		text.append("| League | Season | n | +EV n | Avg edge | ROI | >=3% ROI | >=10% ROI |").append('\n');
		text.append("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |").append('\n');
		appendSeasonRows(text, "PREMIER_LEAGUE", pl.seasons());
		appendSeasonRows(text, "BUNDESLIGA", bl.seasons());
		text.append('\n');
	}

	private static void appendSeasonRows(StringBuilder text, String league, List<SeasonStabilityRow> rows) {
		for (SeasonStabilityRow row : rows) {
			text.append("| ")
					.append(league)
					.append(" | ")
					.append(row.seasonDisplay())
					.append(" | ")
					.append(row.candidateCount())
					.append(" | ")
					.append(row.positiveEdgeCount())
					.append(" | ")
					.append(decimal(row.averageEdge()))
					.append(" | ")
					.append(decimal(row.unitStakeRoi()))
					.append(" | ")
					.append(decimal(row.edgeAtLeast03Roi()))
					.append(" | ")
					.append(decimal(row.edgeAtLeast10Roi()))
					.append(" |")
					.append('\n');
		}
	}

	private static void appendOverround(
			StringBuilder text, LeagueDiagnosticSnapshot pl, LeagueDiagnosticSnapshot bl) {
		text.append("## Market overround").append('\n').append('\n');
		text.append("MARKET-IMPLIED REFERENCE. Not ground-truth probability and not the SafeEdge model.")
				.append('\n')
				.append('\n');
		text.append("| League | Mean overround | Median overround |").append('\n');
		text.append("| --- | ---: | ---: |").append('\n');
		text.append("| PREMIER_LEAGUE | ")
				.append(decimal(pl.meanOverround()))
				.append(" | ")
				.append(decimal(pl.medianOverround()))
				.append(" |")
				.append('\n');
		text.append("| BUNDESLIGA | ")
				.append(decimal(bl.meanOverround()))
				.append(" | ")
				.append(decimal(bl.medianOverround()))
				.append(" |")
				.append('\n')
				.append('\n');
		if (pl.medianOverround() == null) {
			text.append("Premier League median overround was not published in Baseline 002; mean and season means are shown.")
					.append('\n')
					.append('\n');
		}
		text.append("| League | Season | Events | Avg overround |").append('\n');
		text.append("| --- | --- | ---: | ---: |").append('\n');
		for (OverroundSeasonRow row : pl.overroundBySeason()) {
			text.append("| PREMIER_LEAGUE | ")
					.append(row.seasonDisplay())
					.append(" | ")
					.append(row.eventCount())
					.append(" | ")
					.append(decimal(row.averageOverround()))
					.append(" |")
					.append('\n');
		}
		for (OverroundSeasonRow row : bl.overroundBySeason()) {
			text.append("| BUNDESLIGA | ")
					.append(row.seasonDisplay())
					.append(" | ")
					.append(row.eventCount())
					.append(" | ")
					.append(decimal(row.averageOverround()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendStrategies(
			StringBuilder text, LeagueDiagnosticSnapshot pl, LeagueDiagnosticSnapshot bl) {
		text.append("## Strategy results").append('\n').append('\n');
		text.append("Unchanged StrategyPresetFactory configs. Not a league-selection exercise.").append('\n').append('\n');
		text.append("| Strategy | PL bets | PL ROI | PL paused | BL bets | BL ROI | BL paused |").append('\n');
		text.append("| --- | ---: | ---: | --- | ---: | ---: | --- |").append('\n');
		for (String name : List.of("DEFENSIVE", "BALANCED", "GROWTH", "FLAT_STAKE")) {
			StrategyRegressionSnapshot left = strategy(pl, name);
			StrategyRegressionSnapshot right = strategy(bl, name);
			text.append("| ")
					.append(name)
					.append(" | ")
					.append(left == null ? "n/a" : left.betsAccepted())
					.append(" | ")
					.append(decimal(left == null ? null : left.roi()))
					.append(" | ")
					.append(left == null ? "n/a" : left.pausedByDrawdown())
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
			StringBuilder text, LeagueDiagnosticSnapshot pl, LeagueDiagnosticSnapshot bl) {
		text.append("## Statistical uncertainty").append('\n').append('\n');
		text.append("Deterministic bootstrap seed=20260816, replicates=2000. CI excluding 0 is not proof of future profitability.")
				.append('\n')
				.append('\n');
		text.append("| Group | PL mean | PL 95% | BL mean | BL 95% |").append('\n');
		text.append("| --- | ---: | --- | ---: | --- |").append('\n');
		for (String label : List.of(
				"all candidates", "positive-edge", "edge >= 0.10", "NEGATIVE_HANDICAP", "POSITIVE_HANDICAP")) {
			NamedMeanInterval left = interval(pl, label);
			NamedMeanInterval right = interval(bl, label);
			text.append("| ")
					.append(label)
					.append(" | ")
					.append(decimal(left == null ? null : left.interval().mean()))
					.append(" | ")
					.append(ci(left))
					.append(" | ")
					.append(decimal(right == null ? null : right.interval().mean()))
					.append(" | ")
					.append(ci(right))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendTop30(StringBuilder text, LeagueDiagnosticSnapshot pl, LeagueDiagnosticSnapshot bl) {
		text.append("## High-edge forensics (qualitative)").append('\n').append('\n');
		text.append("Inspection only. Outcomes must not define a filter.").append('\n').append('\n');
		text.append("Premier League Baseline 002 top-30 included several 50–90% predicted WIN candidates that settled LOSS or HALF_LOSS (for example Norwich vs Chelsea HOME +0.75, Southampton vs Nott'm Forest HOME −0.5).")
				.append('\n')
				.append('\n');
		if (bl.topPredictedEdges().isEmpty()) {
			text.append("Bundesliga top-30 were not attached to this comparison snapshot.")
					.append('\n')
					.append('\n');
			return;
		}
		text.append("Bundesliga top predicted-edge rows:").append('\n').append('\n');
		text.append("| Date | Event | Side | Line | Odds | P(WIN) | Edge | Settlement | Unit return |").append('\n');
		text.append("| --- | --- | --- | ---: | ---: | ---: | ---: | --- | ---: |").append('\n');
		int limit = Math.min(30, bl.topPredictedEdges().size());
		for (int i = 0; i < limit; i++) {
			ForensicCandidateRow row = bl.topPredictedEdges().get(i);
			text.append("| ")
					.append(row.date())
					.append(" | ")
					.append(row.homeTeam() == null ? row.eventId() : row.homeTeam() + " vs " + row.awayTeam())
					.append(" | ")
					.append(row.side())
					.append(" | ")
					.append(decimal(row.selectedLine()))
					.append(" | ")
					.append(decimal(row.odds()))
					.append(" | ")
					.append(decimal(row.predictedSettlement().winProbability()))
					.append(" | ")
					.append(decimal(row.predictedEdge()))
					.append(" | ")
					.append(row.actualSettlement())
					.append(" | ")
					.append(decimal(row.actualUnitReturn()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
		int extremeWinThenLoss = 0;
		for (ForensicCandidateRow row : bl.topPredictedEdges()) {
			if (row.predictedSettlement().winProbability().compareTo(new BigDecimal("0.50")) >= 0
					&& "LOSS".equals(row.actualSettlement().name())) {
				extremeWinThenLoss++;
			}
		}
		text.append("- Bundesliga top-30 rows with P(WIN) ≥ 0.50 that settled LOSS: ")
				.append(extremeWinThenLoss)
				.append('\n')
				.append('\n');
	}

	private static void appendStructuralTest(StringBuilder text, CrossLeagueComparison comparison) {
		text.append("## Structural-error test").append('\n').append('\n');
		text.append("Diagnostic cutoffs (not production filters): |1X2 gap| ≤ 0.03; |goal gap| ≤ 0.15; |edge−return gap| ≤ 0.01; |Spearman| < 0.10; high-edge ≥10% and ≥20% have predicted WIN above actual and predicted LOSS below actual; edge buckets with n≥30 are not monotone in ROI.")
				.append('\n')
				.append('\n');
		text.append("| Pattern | PREMIER_LEAGUE | BUNDESLIGA |").append('\n');
		text.append("| --- | --- | --- |").append('\n');
		StructuralPatternFlags pl = comparison.premierLeagueFlags();
		StructuralPatternFlags bl = comparison.bundesligaFlags();
		flagRow(text, "A) aggregate goals / 1X2 reasonably calibrated", pl.aggregateGoalsAndMatchResultCalibrated(), bl.aggregateGoalsAndMatchResultCalibrated());
		flagRow(text, "B) aggregate predicted edge near realized return", pl.aggregateEdgeNearRealizedReturn(), bl.aggregateEdgeNearRealizedReturn());
		flagRow(text, "C) edge ranking near zero / weak", pl.edgeRankingWeak(), bl.edgeRankingWeak());
		flagRow(text, "D) high predicted edge: P(WIN) too high, P(LOSS) too low", pl.highEdgeWinOverconfidentAndLossUnderconfident(), bl.highEdgeWinOverconfidentAndLossUnderconfident());
		flagRow(text, "E) higher edge does not monotonically improve ROI", pl.higherEdgeDoesNotMonotonicallyImproveRoi(), bl.higherEdgeDoesNotMonotonicallyImproveRoi());
		flagRow(text, "F) failure appears across multiple seasons", pl.failureStableAcrossSeasons(), bl.failureStableAcrossSeasons());
		text.append('\n');
		text.append("Two leagues are not a proof. This is a replication check.").append('\n').append('\n');
	}

	private static void appendClassification(StringBuilder text, CrossLeagueComparison comparison) {
		text.append("## Classification").append('\n').append('\n');
		text.append("**").append(comparison.classification().name().replace('_', ' ')).append("**").append('\n').append('\n');
		switch (comparison.classification()) {
			case FAILURE_STRONGLY_REPLICATES -> text.append(
					"Both leagues show weak edge ranking, high-edge settlement overconfidence, and the same qualitative pattern.")
					.append('\n');
			case FAILURE_PARTIALLY_REPLICATES -> text.append(
					"Some edge-quality problems appear in Bundesliga, but not the full Premier League pattern.")
					.append('\n');
			case FAILURE_DOES_NOT_REPLICATE -> text.append(
					"Bundesliga materially differs; the Premier League issue may be sample/league specific.")
					.append('\n');
			case FAILURE_STRONGLY_REPLICATES_AGAIN -> throw new IllegalStateException(
					"FAILURE_STRONGLY_REPLICATES_AGAIN is a three-league classification");
		}
		text.append('\n');
		if (comparison.classification() == StructuralReplicationClassification.FAILURE_STRONGLY_REPLICATES) {
			text.append("### NEXT HYPOTHESIS").append('\n').append('\n');
			text.append("Independent-Poisson score tails / dependence assumptions may be too confident for AH value ranking.")
					.append('\n')
					.append('\n');
			text.append("Possible later experiments (not implemented): Dixon-Coles; shrinkage / regularization of team strength; richer team-strength model; Elo/xG.")
					.append('\n')
					.append('\n');
		}
	}

	private static void appendNonConclusions(StringBuilder text, CrossLeagueComparison comparison) {
		text.append("## Explicit non-conclusions").append('\n').append('\n');
		text.append("- no parameter optimization performed").append('\n');
		text.append("- no production filter selected").append('\n');
		text.append("- Bundesliga is not selected as a betting venue because of this comparison").append('\n');
		text.append("- best-looking cell is not a validated strategy").append('\n');
		text.append("- two leagues do not prove a theorem").append('\n');
		text.append("- CI excluding 0 is not proof of future profitability").append('\n');
		text.append("- MARKET_AVERAGE is not Tippmix").append('\n');
		text.append("- 1/odds is not true AH probability").append('\n');
		if (comparison.premierLeague().quoteSource() == HistoricalQuoteSource.MARKET_AVERAGE
				&& comparison.bundesliga().competition() == CanonicalCompetition.BUNDESLIGA) {
			text.append("- football-data.co.uk historical quotes only").append('\n');
		}
		text.append('\n');
	}

	private static void flagRow(StringBuilder text, String label, boolean left, boolean right) {
		text.append("| ")
				.append(label)
				.append(" | ")
				.append(left ? "yes" : "no")
				.append(" | ")
				.append(right ? "yes" : "no")
				.append(" |")
				.append('\n');
	}

	private static void row(StringBuilder text, String metric, int left, int right) {
		text.append("| ").append(metric).append(" | ").append(left).append(" | ").append(right).append(" |").append('\n');
	}

	private static void row(StringBuilder text, String metric, BigDecimal left, BigDecimal right) {
		text.append("| ")
				.append(metric)
				.append(" | ")
				.append(decimal(left))
				.append(" | ")
				.append(decimal(right))
				.append(" |")
				.append('\n');
	}

	private static MarginCategoryCalibration margin(
			PredictionQualitySnapshot quality, DiagnosticMarginCategory category) {
		for (MarginCategoryCalibration row : quality.marginCategories()) {
			if (row.category() == category) {
				return row;
			}
		}
		return null;
	}

	private static AhFamilySnapshot family(LeagueDiagnosticSnapshot league, DiagnosticLineFamily family) {
		for (AhFamilySnapshot row : league.ahFamilies()) {
			if (row.family() == family) {
				return row;
			}
		}
		return null;
	}

	private static SideSnapshot side(LeagueDiagnosticSnapshot league, String side) {
		for (SideSnapshot row : league.sides()) {
			if (side.equals(row.side())) {
				return row;
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

	private static String n(BucketTrendRow row) {
		return row == null ? "n/a" : String.valueOf(row.n());
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
