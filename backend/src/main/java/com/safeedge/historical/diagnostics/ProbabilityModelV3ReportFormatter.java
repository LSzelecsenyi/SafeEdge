package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.probability.ProbabilityModelV3Config;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Probability Model v3 development markdown. Ranking and calibration first;
 * strategy ROI is secondary and is not a success criterion.
 */
public final class ProbabilityModelV3ReportFormatter {

	private ProbabilityModelV3ReportFormatter() {
	}

	public static String format(ProbabilityModelV3DevelopmentReport report) {
		if (report == null) {
			throw new IllegalArgumentException("report is required");
		}
		StringBuilder text = new StringBuilder();
		text.append("# Probability Model v3 Development Evaluation").append('\n').append('\n');
		appendModelDefinition(text, report.v3Config());
		appendGates(text);
		appendAntiLeakage(text);
		for (ProbabilityModelV3LeagueRun league : report.leagues()) {
			appendLeague(text, league);
		}
		appendComparisonTable(text, report);
		appendEdgeRanking(text, report);
		appendHighEdgeCalibration(text, report);
		appendStrategy(text, report);
		appendNonConclusions(text, report);
		return text.toString();
	}

	private static void appendModelDefinition(StringBuilder text, ProbabilityModelV3Config config) {
		text.append("## Model definition").append('\n').append('\n');
		text.append("Implemented model: `JointDixonColesFootballProbabilityModel`.").append('\n');
		text.append("Frozen baselines: `PoissonFootballProbabilityModel` (v1) and `RegularizedDixonColesFootballProbabilityModel` (v2).")
				.append('\n')
				.append('\n');
		text.append("Defence convention: **positive defence is stronger** (concedes fewer). Log-link:")
				.append('\n')
				.append('\n');
		text.append("```text").append('\n');
		text.append("log λ_home = intercept + homeAdvantage + attack(home) − defence(away)").append('\n');
		text.append("log λ_away = intercept + attack(away) − defence(home)").append('\n');
		text.append("ρ = rhoScale * tanh(z)").append('\n');
		text.append("```").append('\n').append('\n');
		text.append("Identifiability: `Σ attack = 0` and `Σ defence = 0` after every optimizer step.").append('\n');
		text.append("Regularization: L2 on centered attack/defence; intercept and home advantage unpenalized.")
				.append('\n')
				.append('\n');
		text.append("| Field | Frozen default |").append('\n');
		text.append("|---|---|").append('\n');
		text.append("| decayHalfLifeDays | ").append(config.decayHalfLifeDays()).append(" |").append('\n');
		text.append("| maxGoalsPerTeam | ").append(config.maxGoalsPerTeam()).append(" |").append('\n');
		text.append("| minimumTeamMatches | ").append(config.minimumTeamMatches()).append(" |").append('\n');
		text.append("| minimumLeagueMatches | ").append(config.minimumLeagueMatches()).append(" |").append('\n');
		text.append("| attackRegularization | ").append(decimal(BigDecimal.valueOf(config.attackRegularization()))).append(" |").append('\n');
		text.append("| defenceRegularization | ").append(decimal(BigDecimal.valueOf(config.defenceRegularization()))).append(" |").append('\n');
		text.append("| optimizerMaxIterations | ").append(config.optimizerMaxIterations()).append(" |").append('\n');
		text.append("| gradientTolerance | ").append(decimal(BigDecimal.valueOf(config.gradientTolerance()))).append(" |").append('\n');
		text.append("| rhoScale | ").append(decimal(BigDecimal.valueOf(config.rhoScale()))).append(" |").append('\n')
				.append('\n');
		text.append("These defaults were declared before evaluation. They are not ROI-fitted optima.")
				.append('\n')
				.append('\n');
	}

	private static void appendGates(StringBuilder text) {
		text.append("## Predeclared classification gates").append('\n').append('\n');
		text.append("Compare v3 to the **better of v1/v2**. Positive ROI is ignored.").append('\n').append('\n');
		text.append("- `MODEL_V3_CLEAR_IMPROVEMENT`: Spearman +≥0.05 vs better baseline in ≥2/3 leagues; third not worse by ≥0.02; ≥10% WIN **and** LOSS abs-gap shrink ≥0.03 in ≥2/3 leagues; log loss not worse by >0.02 in any league.")
				.append('\n');
		text.append("- `MODEL_V3_PARTIAL_IMPROVEMENT`: log loss gate holds; Spearman +≥0.02 in ≥2 leagues **or** WIN+LOSS gap shrink in ≥2; Spearman not worse by ≥0.02 in more than one league.")
				.append('\n');
		text.append("- `MODEL_V3_REGRESSION`: Spearman worse by ≥0.02 in ≥2 leagues, **or** log loss worse by >0.02 in ≥2, **or** ≥10% WIN gaps worsen by ≥0.03 in ≥2, without CLEAR/PARTIAL offset.")
				.append('\n');
		text.append("- `MODEL_V3_NO_MEANINGFUL_IMPROVEMENT`: otherwise.").append('\n').append('\n');
	}

	private static void appendAntiLeakage(StringBuilder text) {
		text.append("## Anti-leakage").append('\n').append('\n');
		text.append("- Walk-forward: same competition, `matchDate < targetDate`, no same-day, no future.")
				.append('\n');
		text.append("- Score-only joint MLE. No bookmaker odds, AH line, edge, or ROI in fitting.")
				.append('\n');
		text.append("- Development leagues only: Premier League, Bundesliga, Serie A.")
				.append('\n');
		text.append("- La Liga and Ligue 1 were not run and were not inspected.")
				.append('\n');
		text.append("- CandidateEngine, StrategyEngine, BacktestEngine, and SettlementEngine were not changed.")
				.append('\n');
		text.append("- HISTORICAL QUOTE SOURCE = MARKET_AVERAGE (football-data.co.uk, not Tippmix).")
				.append('\n');
		text.append("- Window: trainingFromSeason=2014, evaluation 2019→2023, starting bankroll 100000.")
				.append('\n');
		text.append("- Warm-start uses only earlier cutoffs as initial values; each date still refits.")
				.append('\n')
				.append('\n');
	}

	private static void appendLeague(StringBuilder text, ProbabilityModelV3LeagueRun league) {
		text.append("## ").append(title(league.competition())).append('\n').append('\n');
		ProbabilityModelV3Comparison comparison = league.comparison();
		appendCounts(text, comparison);
		appendCoreMetrics(text, comparison);
		appendDeciles(text, comparison);
		appendHighEdgeFiveWay(text, comparison);
		appendSlices(text, comparison.v3Extra());
		appendOptimizer(text, comparison.v3Extra().optimizer());
		appendStrategyTable(text, comparison);
		text.append('\n');
	}

	private static void appendCounts(StringBuilder text, ProbabilityModelV3Comparison comparison) {
		text.append("### Counts").append('\n').append('\n');
		text.append("| Count | V1 | V2 | V3 |").append('\n');
		text.append("|---|---|---|---|").append('\n');
		count(text, "matches loaded", comparison.v1Extra().matchesLoaded(), comparison.v2Extra().matchesLoaded(), comparison.v3Extra().matchesLoaded());
		count(text, "matches evaluated", comparison.v1Extra().matchesEvaluated(), comparison.v2Extra().matchesEvaluated(), comparison.v3Extra().matchesEvaluated());
		count(text, "predictions available", comparison.v1Extra().predictionsAvailable(), comparison.v2Extra().predictionsAvailable(), comparison.v3Extra().predictionsAvailable());
		count(text, "skipped insufficient history", comparison.v1Extra().skippedInsufficientHistory(), comparison.v2Extra().skippedInsufficientHistory(), comparison.v3Extra().skippedInsufficientHistory());
		count(text, "skipped fitting failed", comparison.v1Extra().skippedFittingFailed(), comparison.v2Extra().skippedFittingFailed(), comparison.v3Extra().skippedFittingFailed());
		count(text, "candidates", comparison.v1Extra().candidates(), comparison.v2Extra().candidates(), comparison.v3Extra().candidates());
		count(text, "positive EV", comparison.v1Extra().positiveEv(), comparison.v2Extra().positiveEv(), comparison.v3Extra().positiveEv());
		count(text, "zero EV", comparison.v1Extra().zeroEv(), comparison.v2Extra().zeroEv(), comparison.v3Extra().zeroEv());
		count(text, "negative EV", comparison.v1Extra().negativeEv(), comparison.v2Extra().negativeEv(), comparison.v3Extra().negativeEv());
		text.append('\n');
	}

	private static void appendCoreMetrics(StringBuilder text, ProbabilityModelV3Comparison comparison) {
		text.append("### Score, ranking, and mean edge").append('\n').append('\n');
		text.append("| Metric | V1 | V2 | V3 |").append('\n');
		text.append("|---|---|---|---|").append('\n');
		row3(text, "score log loss", comparison.v1().scoreLogLoss(), comparison.v2().scoreLogLoss(), comparison.v3().scoreLogLoss());
		row3(text, "Spearman", comparison.v1().rankQuality().spearman(), comparison.v2().rankQuality().spearman(), comparison.v3().rankQuality().spearman());
		row3(text, "Pearson", comparison.v1().rankQuality().pearson(), comparison.v2().rankQuality().pearson(), comparison.v3().rankQuality().pearson());
		row3(text, "mean predicted edge", comparison.v1().meanPredictedEdge(), comparison.v2().meanPredictedEdge(), comparison.v3().meanPredictedEdge());
		row3(text, "realized all-candidate unit ROI", comparison.v1().realizedUnitRoi(), comparison.v2().realizedUnitRoi(), comparison.v3().realizedUnitRoi());
		text.append("| decile ROI inversions (n≥30) | ")
				.append(comparison.v1().decileRoiInversions())
				.append(" | ")
				.append(comparison.v2().decileRoiInversions())
				.append(" | ")
				.append(comparison.v3().decileRoiInversions())
				.append(" |")
				.append('\n')
				.append('\n');
		appendQuantileTriple(text, "λ_home", comparison.v1().lambdaHome(), comparison.v2().lambdaHome(), comparison.v3().lambdaHome());
		appendQuantileTriple(text, "λ_away", comparison.v1().lambdaAway(), comparison.v2().lambdaAway(), comparison.v3().lambdaAway());
		appendQuantileTriple(text, "P(WIN)", comparison.v1().predictedWin(), comparison.v2().predictedWin(), comparison.v3().predictedWin());
	}

	private static void appendDeciles(StringBuilder text, ProbabilityModelV3Comparison comparison) {
		text.append("### Edge deciles").append('\n').append('\n');
		text.append("| Decile | V1 n | V1 avg edge | V1 ROI | V1 gap | V2 n | V2 avg edge | V2 ROI | V2 gap | V3 n | V3 avg edge | V3 ROI | V3 gap |")
				.append('\n');
		text.append("|---|---|---|---|---|---|---|---|---|---|---|---|---|").append('\n');
		int n = min(
				comparison.v1Extra().edgeDeciles().size(),
				comparison.v2Extra().edgeDeciles().size(),
				comparison.v3Extra().edgeDeciles().size());
		for (int i = 0; i < n; i++) {
			EdgeQualityGroupSummary d1 = comparison.v1Extra().edgeDeciles().get(i);
			EdgeQualityGroupSummary d2 = comparison.v2Extra().edgeDeciles().get(i);
			EdgeQualityGroupSummary d3 = comparison.v3Extra().edgeDeciles().get(i);
			text.append("| ")
					.append(d1.key())
					.append(" | ")
					.append(d1.n())
					.append(" | ")
					.append(decimal(d1.averageEdge()))
					.append(" | ")
					.append(decimal(d1.unitStakeRoi()))
					.append(" | ")
					.append(decimal(d1.calibrationGap()))
					.append(" | ")
					.append(d2.n())
					.append(" | ")
					.append(decimal(d2.averageEdge()))
					.append(" | ")
					.append(decimal(d2.unitStakeRoi()))
					.append(" | ")
					.append(decimal(d2.calibrationGap()))
					.append(" | ")
					.append(d3.n())
					.append(" | ")
					.append(decimal(d3.averageEdge()))
					.append(" | ")
					.append(decimal(d3.unitStakeRoi()))
					.append(" | ")
					.append(decimal(d3.calibrationGap()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendHighEdgeFiveWay(StringBuilder text, ProbabilityModelV3Comparison comparison) {
		text.append("### High-edge five-way settlement").append('\n').append('\n');
		appendFiveWayBlock(text, "≥ 3%", comparison.v1Extra().fiveWay3(), comparison.v2Extra().fiveWay3(), comparison.v3Extra().fiveWay3());
		appendFiveWayBlock(text, "≥ 5%", comparison.v1Extra().fiveWay5(), comparison.v2Extra().fiveWay5(), comparison.v3Extra().fiveWay5());
		appendFiveWayBlock(text, "≥ 10%", comparison.v1Extra().fiveWay10(), comparison.v2Extra().fiveWay10(), comparison.v3Extra().fiveWay10());
		appendFiveWayBlock(text, "≥ 20%", comparison.v1Extra().fiveWay20(), comparison.v2Extra().fiveWay20(), comparison.v3Extra().fiveWay20());
		appendFiveWayBlock(text, "≥ 30%", comparison.v1Extra().fiveWay30(), comparison.v2Extra().fiveWay30(), comparison.v3Extra().fiveWay30());
	}

	private static void appendFiveWayBlock(
			StringBuilder text,
			String title,
			HighEdgeFiveWaySnapshot v1,
			HighEdgeFiveWaySnapshot v2,
			HighEdgeFiveWaySnapshot v3) {
		text.append("#### ").append(title).append('\n').append('\n');
		text.append("| Metric | V1 | V2 | V3 |").append('\n');
		text.append("|---|---|---|---|").append('\n');
		text.append("| n | ").append(v1.n()).append(" | ").append(v2.n()).append(" | ").append(v3.n()).append(" |").append('\n');
		row3(text, "avg edge", v1.averageEdge(), v2.averageEdge(), v3.averageEdge());
		row3(text, "unit ROI", v1.unitStakeRoi(), v2.unitStakeRoi(), v3.unitStakeRoi());
		outcome(text, "WIN", v1.win(), v2.win(), v3.win());
		outcome(text, "HALF_WIN", v1.halfWin(), v2.halfWin(), v3.halfWin());
		outcome(text, "PUSH", v1.push(), v2.push(), v3.push());
		outcome(text, "HALF_LOSS", v1.halfLoss(), v2.halfLoss(), v3.halfLoss());
		outcome(text, "LOSS", v1.loss(), v2.loss(), v3.loss());
		text.append('\n');
	}

	private static void outcome(
			StringBuilder text, String name, OutcomeCalibration v1, OutcomeCalibration v2, OutcomeCalibration v3) {
		row3(text, name + " predicted", v1.averagePredictedProbability(), v2.averagePredictedProbability(), v3.averagePredictedProbability());
		row3(text, name + " actual", v1.actualFrequency(), v2.actualFrequency(), v3.actualFrequency());
	}

	private static void appendSlices(StringBuilder text, ProbabilityModelV3Extras extra) {
		text.append("### HOME / AWAY (v3)").append('\n').append('\n');
		appendGroupTable(text, extra.bySide());
		text.append("### AH family (v3)").append('\n').append('\n');
		appendGroupTable(text, extra.byFamily());
		text.append("### Season (v3)").append('\n').append('\n');
		text.append("| Season | n | avg edge | unit ROI | ≥10% n | ≥10% ROI |").append('\n');
		text.append("|---|---|---|---|---|---|").append('\n');
		for (SeasonStabilityRow row : extra.bySeason()) {
			text.append("| ")
					.append(row.seasonDisplay())
					.append(" | ")
					.append(row.candidateCount())
					.append(" | ")
					.append(decimal(row.averageEdge()))
					.append(" | ")
					.append(decimal(row.unitStakeRoi()))
					.append(" | ")
					.append(row.edgeAtLeast10Count())
					.append(" | ")
					.append(decimal(row.edgeAtLeast10Roi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendGroupTable(StringBuilder text, List<EdgeQualityGroupSummary> rows) {
		text.append("| Group | n | avg edge | unit ROI |").append('\n');
		text.append("|---|---|---|---|").append('\n');
		for (EdgeQualityGroupSummary row : rows) {
			text.append("| ")
					.append(row.key())
					.append(" | ")
					.append(row.n())
					.append(" | ")
					.append(decimal(row.averageEdge()))
					.append(" | ")
					.append(decimal(row.unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendOptimizer(StringBuilder text, JointDixonColesOptimizerSummary optimizer) {
		text.append("### Fitted parameters and optimizer (v3)").append('\n').append('\n');
		text.append("- snapshots=").append(optimizer.snapshotCount()).append('\n');
		text.append("- fitting failures=").append(optimizer.fittingFailures()).append('\n');
		text.append("- converged=").append(optimizer.convergedCount()).append('\n');
		text.append("- mean iterations=").append(decimal(optimizer.meanIterations())).append('\n');
		text.append("- median iterations=").append(decimal(optimizer.medianIterations())).append('\n');
		text.append("- max iterations=").append(optimizer.maxIterationsObserved()).append('\n');
		text.append("- parameters finite=").append(optimizer.parametersFinite()).append('\n');
		text.append("- median home advantage positive=").append(optimizer.medianHomeAdvantagePositive()).append('\n');
		text.append("- ρ min/median/max=")
				.append(decimal(optimizer.rho().min()))
				.append("/")
				.append(decimal(optimizer.rho().median()))
				.append("/")
				.append(decimal(optimizer.rho().max()))
				.append('\n')
				.append('\n');
		appendQuantileOne(text, "attack", optimizer.attack());
		appendQuantileOne(text, "defence", optimizer.defence());
		appendQuantileOne(text, "homeAdvantage", optimizer.homeAdvantage());
	}

	private static void appendStrategyTable(StringBuilder text, ProbabilityModelV3Comparison comparison) {
		text.append("### Strategy secondary metrics").append('\n').append('\n');
		text.append("| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI | V3 accepted | V3 ROI |").append('\n');
		text.append("|---|---|---|---|---|---|---|").append('\n');
		int n = min(comparison.v1().strategies().size(), comparison.v2().strategies().size(), comparison.v3().strategies().size());
		for (int i = 0; i < n; i++) {
			StrategySecondarySnapshot s1 = comparison.v1().strategies().get(i);
			StrategySecondarySnapshot s2 = comparison.v2().strategies().get(i);
			StrategySecondarySnapshot s3 = comparison.v3().strategies().get(i);
			text.append("| ")
					.append(s1.name())
					.append(" | ")
					.append(s1.betsAccepted())
					.append(" | ")
					.append(decimal(s1.roi()))
					.append(" | ")
					.append(s2.betsAccepted())
					.append(" | ")
					.append(decimal(s2.roi()))
					.append(" | ")
					.append(s3.betsAccepted())
					.append(" | ")
					.append(decimal(s3.roi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendComparisonTable(StringBuilder text, ProbabilityModelV3DevelopmentReport report) {
		text.append("## V1 / V2 / V3 comparison").append('\n').append('\n');
		text.append("| Metric | V1 | V2 | V3 |").append('\n');
		text.append("|---|---|---|---|").append('\n');
		for (ProbabilityModelV3LeagueRun league : report.leagues()) {
			String prefix = shortName(league.competition());
			ProbabilityModelV3Comparison c = league.comparison();
			row3(text, prefix + " score log loss", c.v1().scoreLogLoss(), c.v2().scoreLogLoss(), c.v3().scoreLogLoss());
			row3(text, prefix + " Spearman", c.v1().rankQuality().spearman(), c.v2().rankQuality().spearman(), c.v3().rankQuality().spearman());
			row3(text, prefix + " ≥10% edge ROI", c.v1().highEdge10().unitStakeRoi(), c.v2().highEdge10().unitStakeRoi(), c.v3().highEdge10().unitStakeRoi());
			text.append("| ")
					.append(prefix)
					.append(" ≥10% WIN pred/act | ")
					.append(pair(c.v1().highEdge10().predictedWin(), c.v1().highEdge10().actualWin()))
					.append(" | ")
					.append(pair(c.v2().highEdge10().predictedWin(), c.v2().highEdge10().actualWin()))
					.append(" | ")
					.append(pair(c.v3().highEdge10().predictedWin(), c.v3().highEdge10().actualWin()))
					.append(" |")
					.append('\n');
			text.append("| ")
					.append(prefix)
					.append(" ≥10% LOSS pred/act | ")
					.append(pair(c.v1().highEdge10().predictedLoss(), c.v1().highEdge10().actualLoss()))
					.append(" | ")
					.append(pair(c.v2().highEdge10().predictedLoss(), c.v2().highEdge10().actualLoss()))
					.append(" | ")
					.append(pair(c.v3().highEdge10().predictedLoss(), c.v3().highEdge10().actualLoss()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
		text.append("Averages across the three development leagues:").append('\n').append('\n');
		text.append("| Metric | V1 | V2 | V3 |").append('\n');
		text.append("|---|---|---|---|").append('\n');
		row3(text, "mean Spearman", mean(report, m -> m.rankQuality().spearman(), 1), mean(report, m -> m.rankQuality().spearman(), 2), mean(report, m -> m.rankQuality().spearman(), 3));
		row3(text, "mean log loss", mean(report, ProbabilityModelV2LeagueMetrics::scoreLogLoss, 1), mean(report, ProbabilityModelV2LeagueMetrics::scoreLogLoss, 2), mean(report, ProbabilityModelV2LeagueMetrics::scoreLogLoss, 3));
		text.append('\n');
	}

	private static void appendEdgeRanking(StringBuilder text, ProbabilityModelV3DevelopmentReport report) {
		text.append("## Edge ranking vs better of v1/v2").append('\n').append('\n');
		for (ProbabilityModelV3LeagueRun league : report.leagues()) {
			text.append("- ")
					.append(title(league.competition()))
					.append(" Spearman delta=")
					.append(decimal(league.comparison().spearmanDeltaVsBetterBaseline()))
					.append(" log-loss delta=")
					.append(decimal(league.comparison().logLossDeltaVsBetterBaseline()))
					.append(" ≥10% WIN-gap shrink=")
					.append(decimal(league.comparison().winGap10ShrinkVsBetterBaseline()))
					.append(" ≥10% LOSS-gap shrink=")
					.append(decimal(league.comparison().lossGap10ShrinkVsBetterBaseline()))
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendHighEdgeCalibration(StringBuilder text, ProbabilityModelV3DevelopmentReport report) {
		text.append("## ≥10% and ≥20% calibration").append('\n').append('\n');
		text.append("| League | ≥10% V3 P(WIN)/act | ≥10% V3 P(LOSS)/act | ≥20% V3 P(WIN)/act | ≥20% V3 P(LOSS)/act |")
				.append('\n');
		text.append("|---|---|---|---|---|").append('\n');
		for (ProbabilityModelV3LeagueRun league : report.leagues()) {
			HighEdgeCalibrationSnapshot t10 = league.comparison().v3().highEdge10();
			HighEdgeCalibrationSnapshot t20 = league.comparison().v3().highEdge20();
			text.append("| ")
					.append(title(league.competition()))
					.append(" | ")
					.append(pair(t10.predictedWin(), t10.actualWin()))
					.append(" | ")
					.append(pair(t10.predictedLoss(), t10.actualLoss()))
					.append(" | ")
					.append(pair(t20.predictedWin(), t20.actualWin()))
					.append(" | ")
					.append(pair(t20.predictedLoss(), t20.actualLoss()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendStrategy(StringBuilder text, ProbabilityModelV3DevelopmentReport report) {
		text.append("## Strategy secondary metrics").append('\n').append('\n');
		text.append("Unchanged presets: DEFENSIVE, BALANCED, GROWTH, FLAT_STAKE. ROI is **not** a v3 success gate.")
				.append('\n')
				.append('\n');
		for (ProbabilityModelV3LeagueRun league : report.leagues()) {
			text.append("### ").append(title(league.competition())).append('\n').append('\n');
			appendStrategyTable(text, league.comparison());
		}
	}

	private static void appendNonConclusions(StringBuilder text, ProbabilityModelV3DevelopmentReport report) {
		text.append("## Explicit non-conclusions").append('\n').append('\n');
		text.append("- Classification: **").append(report.classification()).append("**").append('\n');
		for (String reason : report.classificationReasons()) {
			text.append("- ").append(reason).append('\n');
		}
		text.append("- This is not a claim that SafeEdge is profitable.").append('\n');
		text.append("- Parameters were not changed after seeing ROI or after Premier League.").append('\n');
		text.append("- La Liga and Ligue 1 remain untouched validation leagues.").append('\n');
		text.append('\n');
	}

	private static void appendQuantileTriple(
			StringBuilder text, String name, EdgeQuantiles v1, EdgeQuantiles v2, EdgeQuantiles v3) {
		text.append("| ").append(name).append(" | p50 | p90 | p99 | max |").append('\n');
		text.append("|---|---|---|---|---|").append('\n');
		appendQuantileRow(text, "V1", v1);
		appendQuantileRow(text, "V2", v2);
		appendQuantileRow(text, "V3", v3);
		text.append('\n');
	}

	private static void appendQuantileOne(StringBuilder text, String name, EdgeQuantiles quantiles) {
		text.append("| ").append(name).append(" | min | p10 | p50 | p90 | p99 | max |").append('\n');
		text.append("|---|---|---|---|---|---|---|").append('\n');
		text.append("| V3 | ")
				.append(decimal(quantiles.min()))
				.append(" | ")
				.append(decimal(quantiles.p10()))
				.append(" | ")
				.append(decimal(quantiles.median()))
				.append(" | ")
				.append(decimal(quantiles.p90()))
				.append(" | ")
				.append(decimal(quantiles.p99()))
				.append(" | ")
				.append(decimal(quantiles.max()))
				.append(" |")
				.append('\n')
				.append('\n');
	}

	private static void appendQuantileRow(StringBuilder text, String label, EdgeQuantiles quantiles) {
		text.append("| ")
				.append(label)
				.append(" | ")
				.append(decimal(quantiles.median()))
				.append(" | ")
				.append(decimal(quantiles.p90()))
				.append(" | ")
				.append(decimal(quantiles.p99()))
				.append(" | ")
				.append(decimal(quantiles.max()))
				.append(" |")
				.append('\n');
	}

	private static BigDecimal mean(
			ProbabilityModelV3DevelopmentReport report,
			java.util.function.Function<ProbabilityModelV2LeagueMetrics, BigDecimal> getter,
			int version) {
		BigDecimal sum = BigDecimal.ZERO;
		int n = 0;
		for (ProbabilityModelV3LeagueRun league : report.leagues()) {
			ProbabilityModelV2LeagueMetrics metrics = switch (version) {
				case 1 -> league.comparison().v1();
				case 2 -> league.comparison().v2();
				default -> league.comparison().v3();
			};
			BigDecimal value = getter.apply(metrics);
			if (value != null) {
				sum = sum.add(value);
				n++;
			}
		}
		return n == 0 ? null : sum.divide(BigDecimal.valueOf(n), java.math.MathContext.DECIMAL128);
	}

	private static void count(StringBuilder text, String name, int v1, int v2, int v3) {
		text.append("| ").append(name).append(" | ").append(v1).append(" | ").append(v2).append(" | ").append(v3).append(" |").append('\n');
	}

	private static void row3(StringBuilder text, String name, BigDecimal v1, BigDecimal v2, BigDecimal v3) {
		text.append("| ")
				.append(name)
				.append(" | ")
				.append(decimal(v1))
				.append(" | ")
				.append(decimal(v2))
				.append(" | ")
				.append(decimal(v3))
				.append(" |")
				.append('\n');
	}

	private static String pair(BigDecimal predicted, BigDecimal actual) {
		return decimal(predicted) + "/" + decimal(actual);
	}

	private static String title(CanonicalCompetition competition) {
		return switch (competition) {
			case PREMIER_LEAGUE -> "Premier League";
			case BUNDESLIGA -> "Bundesliga";
			case SERIE_A -> "Serie A";
			default -> competition.name();
		};
	}

	private static String shortName(CanonicalCompetition competition) {
		return switch (competition) {
			case PREMIER_LEAGUE -> "PL";
			case BUNDESLIGA -> "BL";
			case SERIE_A -> "SA";
			default -> competition.name();
		};
	}

	private static int min(int a, int b, int c) {
		return Math.min(a, Math.min(b, c));
	}

	private static String decimal(BigDecimal value) {
		if (value == null) {
			return "n/a";
		}
		return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
