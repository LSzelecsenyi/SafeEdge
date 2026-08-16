package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.evaluation.WalkForwardBuildStats;
import com.safeedge.probability.ProbabilityModelV2Config;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Probability Model v2 development markdown. Ranking and calibration first;
 * strategy ROI is secondary and is not a success criterion.
 */
public final class ProbabilityModelV2ReportFormatter {

	private ProbabilityModelV2ReportFormatter() {
	}

	public static String format(ProbabilityModelV2DevelopmentReport report) {
		if (report == null) {
			throw new IllegalArgumentException("report is required");
		}
		StringBuilder text = new StringBuilder();
		text.append("# Probability Model v2 Development Evaluation").append('\n').append('\n');
		appendModelDefinition(text, report.v2Config());
		appendAntiLeakage(text);
		for (ProbabilityModelV2LeagueRun league : report.leagues()) {
			appendLeague(text, league);
		}
		appendCrossLeague(text, report);
		appendConfidence(text, report);
		appendEdgeRanking(text, report);
		appendSettlement(text, report);
		appendScoreLikelihood(text, report);
		appendStrategy(text, report);
		appendHypotheses(text);
		appendNonConclusions(text, report);
		return text.toString();
	}

	private static void appendModelDefinition(StringBuilder text, ProbabilityModelV2Config config) {
		text.append("## Model definition").append('\n').append('\n');
		text.append("Implemented model: `RegularizedDixonColesFootballProbabilityModel`.").append('\n');
		text.append("Frozen baseline: `PoissonFootballProbabilityModel` (v1, unchanged).").append('\n').append('\n');
		text.append("### Shrinkage").append('\n').append('\n');
		text.append("```text").append('\n');
		text.append("shrunkRate = (weightedTeamGoals + prior * leagueRate)").append('\n');
		text.append("           / (weightedTeamExposure + prior)").append('\n');
		text.append("shrunkStrength = shrunkRate / leagueRate").append('\n');
		text.append("```").append('\n').append('\n');
		text.append("- `attackDefenceShrinkageStrength` default = **")
				.append(config.attackDefenceShrinkageStrength().toPlainString())
				.append("** weighted league-average pseudo-matches (`Σ timeWeight`, not raw match count).")
				.append('\n');
		text.append("- Chosen before evaluation as the same order of magnitude as `minimumTeamMatches`. Not fitted to ROI.")
				.append('\n');
		text.append("- `prior = 0` reproduces the unregularized v1 ratio (Dixon-Coles still differs when enabled).")
				.append('\n')
				.append('\n');
		text.append("### Dixon-Coles").append('\n').append('\n');
		text.append("```text").append('\n');
		text.append("τ(0,0) = 1 − λμρ").append('\n');
		text.append("τ(0,1) = 1 + λρ").append('\n');
		text.append("τ(1,0) = 1 + μρ").append('\n');
		text.append("τ(1,1) = 1 − ρ").append('\n');
		text.append("τ(x,y) = 1 otherwise").append('\n');
		text.append("```").append('\n').append('\n');
		text.append("- ρ is fitted walk-forward from **weighted score log-likelihood** on matches with `matchDate < targetDate`.")
				.append('\n');
		text.append("- Market odds, candidate edge, and betting ROI never enter ρ fitting.")
				.append('\n');
		text.append("- Search is deterministic golden-section maximization on a τ-valid interval; ρ = 0 is always a candidate.")
				.append('\n');
		text.append("- Shared Poisson defaults remain decayHalfLifeDays=")
				.append(config.decayHalfLifeDays())
				.append(", maxGoalsPerTeam=")
				.append(config.maxGoalsPerTeam())
				.append(", minimumTeamMatches=")
				.append(config.minimumTeamMatches())
				.append('.')
				.append('\n')
				.append('\n');
	}

	private static void appendAntiLeakage(StringBuilder text) {
		text.append("## Anti-leakage").append('\n').append('\n');
		text.append("- Walk-forward: same competition, `matchDate < targetDate`, no same-day, no future.")
				.append('\n');
		text.append("- Score-only ρ fitting. No bookmaker odds as model features.")
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
				.append('\n')
				.append('\n');
	}

	private static void appendLeague(StringBuilder text, ProbabilityModelV2LeagueRun league) {
		text.append("## ").append(title(league.competition())).append('\n').append('\n');
		WalkForwardBuildStats v1Stats = league.v1Output().dataset().stats();
		text.append("- Predictions available: v1=")
				.append(v1Stats.predictionsAvailable())
				.append(" v2=")
				.append(league.v2Output().dataset().stats().predictionsAvailable())
				.append('\n');
		text.append("- Candidates: v1=")
				.append(league.comparison().v1().candidateCount())
				.append(" v2=")
				.append(league.comparison().v2().candidateCount())
				.append('\n')
				.append('\n');
		appendScoreTable(text, league.comparison());
		appendRankingTable(text, league.comparison());
		appendHighEdgeTable(text, league.comparison());
		appendLowScoreTable(text, league.comparison());
		appendRho(text, league.comparison().v2().rho());
		appendStrengths(text, league.comparison().v2().strengths());
		appendStrategyTable(text, league.comparison());
		text.append('\n');
	}

	private static void appendScoreTable(StringBuilder text, ProbabilityModelComparison comparison) {
		ProbabilityModelV2LeagueMetrics v1 = comparison.v1();
		ProbabilityModelV2LeagueMetrics v2 = comparison.v2();
		GoalCalibrationDiagnostics g1 = v1.goalCalibration();
		GoalCalibrationDiagnostics g2 = v2.goalCalibration();
		text.append("### Score prediction").append('\n').append('\n');
		text.append("| Metric | V1 | V2 |").append('\n');
		text.append("|---|---|---|").append('\n');
		row(text, "score log loss", v1.scoreLogLoss(), v2.scoreLogLoss());
		row(text, "predicted home goals", g1.averagePredictedHomeGoals(), g2.averagePredictedHomeGoals());
		row(text, "actual home goals", g1.averageActualHomeGoals(), g2.averageActualHomeGoals());
		row(text, "predicted away goals", g1.averagePredictedAwayGoals(), g2.averagePredictedAwayGoals());
		row(text, "actual away goals", g1.averageActualAwayGoals(), g2.averageActualAwayGoals());
		row(text, "1X2 HOME predicted", g1.averagePredictedHomeWinProbability(), g2.averagePredictedHomeWinProbability());
		row(text, "1X2 HOME actual", g1.actualHomeWinFrequency(), g2.actualHomeWinFrequency());
		row(text, "1X2 DRAW predicted", g1.averagePredictedDrawProbability(), g2.averagePredictedDrawProbability());
		row(text, "1X2 DRAW actual", g1.actualDrawFrequency(), g2.actualDrawFrequency());
		row(text, "1X2 AWAY predicted", g1.averagePredictedAwayWinProbability(), g2.averagePredictedAwayWinProbability());
		row(text, "1X2 AWAY actual", g1.actualAwayWinFrequency(), g2.actualAwayWinFrequency());
		text.append('\n');
		text.append("Margin categories:").append('\n').append('\n');
		text.append("| Category | V1 pred | V1 actual | V2 pred | V2 actual |").append('\n');
		text.append("|---|---|---|---|---|").append('\n');
		int n = Math.min(v1.marginCategories().size(), v2.marginCategories().size());
		for (int i = 0; i < n; i++) {
			MarginCategoryCalibration m1 = v1.marginCategories().get(i);
			MarginCategoryCalibration m2 = v2.marginCategories().get(i);
			text.append("| ")
					.append(m1.category())
					.append(" | ")
					.append(decimal(m1.averagePredictedProbability()))
					.append(" | ")
					.append(decimal(m1.actualFrequency()))
					.append(" | ")
					.append(decimal(m2.averagePredictedProbability()))
					.append(" | ")
					.append(decimal(m2.actualFrequency()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendRankingTable(StringBuilder text, ProbabilityModelComparison comparison) {
		text.append("### Edge ranking").append('\n').append('\n');
		text.append("| Metric | V1 | V2 |").append('\n');
		text.append("|---|---|---|").append('\n');
		row(text, "Spearman", comparison.v1().rankQuality().spearman(), comparison.v2().rankQuality().spearman());
		row(text, "Pearson", comparison.v1().rankQuality().pearson(), comparison.v2().rankQuality().pearson());
		row(text, "mean predicted edge", comparison.v1().meanPredictedEdge(), comparison.v2().meanPredictedEdge());
		row(text, "realized unit ROI", comparison.v1().realizedUnitRoi(), comparison.v2().realizedUnitRoi());
		text.append("| decile ROI inversions (n≥30) | ")
				.append(comparison.v1().decileRoiInversions())
				.append(" | ")
				.append(comparison.v2().decileRoiInversions())
				.append(" |")
				.append('\n')
				.append('\n');
		text.append("| Decile | V1 n | V1 avg edge | V1 ROI | V2 n | V2 avg edge | V2 ROI |").append('\n');
		text.append("|---|---|---|---|---|---|---|").append('\n');
		int n = Math.min(comparison.v1().edgeDeciles().size(), comparison.v2().edgeDeciles().size());
		for (int i = 0; i < n; i++) {
			DecileSnapshot d1 = comparison.v1().edgeDeciles().get(i);
			DecileSnapshot d2 = comparison.v2().edgeDeciles().get(i);
			text.append("| ")
					.append(d1.key())
					.append(" | ")
					.append(d1.n())
					.append(" | ")
					.append(decimal(d1.averageEdge()))
					.append(" | ")
					.append(decimal(d1.unitStakeRoi()))
					.append(" | ")
					.append(d2.n())
					.append(" | ")
					.append(decimal(d2.averageEdge()))
					.append(" | ")
					.append(decimal(d2.unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendHighEdgeTable(StringBuilder text, ProbabilityModelComparison comparison) {
		text.append("### High edge").append('\n').append('\n');
		appendHighEdge(text, comparison.v1().highEdge10(), comparison.v2().highEdge10());
		appendHighEdge(text, comparison.v1().highEdge20(), comparison.v2().highEdge20());
		appendHighEdge(text, comparison.v1().highEdge30(), comparison.v2().highEdge30());
	}

	private static void appendHighEdge(
			StringBuilder text, HighEdgeCalibrationSnapshot v1, HighEdgeCalibrationSnapshot v2) {
		text.append("#### ≥ ").append(percent(v1.threshold())).append('\n').append('\n');
		text.append("| Metric | V1 | V2 |").append('\n');
		text.append("|---|---|---|").append('\n');
		text.append("| n | ").append(v1.n()).append(" | ").append(v2.n()).append(" |").append('\n');
		row(text, "avg edge", v1.averageEdge(), v2.averageEdge());
		row(text, "unit ROI", v1.unitStakeRoi(), v2.unitStakeRoi());
		row(text, "predicted P(WIN)", v1.predictedWin(), v2.predictedWin());
		row(text, "actual WIN", v1.actualWin(), v2.actualWin());
		row(text, "predicted P(LOSS)", v1.predictedLoss(), v2.predictedLoss());
		row(text, "actual LOSS", v1.actualLoss(), v2.actualLoss());
		text.append('\n');
	}

	private static void appendLowScoreTable(StringBuilder text, ProbabilityModelComparison comparison) {
		text.append("### Low-score calibration (0-0 / 1-0 / 0-1 / 1-1)").append('\n').append('\n');
		text.append("| Score | V1 pred | V1 actual | V2 pred | V2 actual |").append('\n');
		text.append("|---|---|---|---|---|").append('\n');
		appendLowScoreRow(text, comparison.v1().lowScores().score00(), comparison.v2().lowScores().score00());
		appendLowScoreRow(text, comparison.v1().lowScores().score10(), comparison.v2().lowScores().score10());
		appendLowScoreRow(text, comparison.v1().lowScores().score01(), comparison.v2().lowScores().score01());
		appendLowScoreRow(text, comparison.v1().lowScores().score11(), comparison.v2().lowScores().score11());
		text.append('\n');
	}

	private static void appendLowScoreRow(
			StringBuilder text, LowScoreCellCalibration v1, LowScoreCellCalibration v2) {
		text.append("| ")
				.append(v1.scoreline())
				.append(" | ")
				.append(decimal(v1.averagePredicted()))
				.append(" | ")
				.append(decimal(v1.actualFrequency()))
				.append(" | ")
				.append(decimal(v2.averagePredicted()))
				.append(" | ")
				.append(decimal(v2.actualFrequency()))
				.append(" |")
				.append('\n');
	}

	private static void appendRho(StringBuilder text, RhoSummary rho) {
		text.append("### Fitted ρ (score likelihood only)").append('\n').append('\n');
		text.append("- n=").append(rho.n()).append('\n');
		text.append("- min=").append(decimal(rho.min())).append('\n');
		text.append("- median=").append(decimal(rho.median())).append('\n');
		text.append("- max=").append(decimal(rho.max())).append('\n').append('\n');
	}

	private static void appendStrengths(StringBuilder text, List<StrengthQuantileReport> strengths) {
		text.append("### Strength shrinkage (raw v1-style ratio vs shrunk v2)").append('\n').append('\n');
		text.append("| Strength | raw MAD from 1 | shrunk MAD from 1 | raw p50 | shrunk p50 | raw p95 | shrunk p95 | raw max | shrunk max |")
				.append('\n');
		text.append("|---|---|---|---|---|---|---|---|---|").append('\n');
		for (StrengthQuantileReport row : strengths) {
			text.append("| ")
					.append(row.name())
					.append(" | ")
					.append(decimal(row.rawMadFromOne()))
					.append(" | ")
					.append(decimal(row.shrunkMadFromOne()))
					.append(" | ")
					.append(q(row.raw(), q -> q.median()))
					.append(" | ")
					.append(q(row.shrunk(), q -> q.median()))
					.append(" | ")
					.append(q(row.raw(), EdgeQuantiles::p95))
					.append(" | ")
					.append(q(row.shrunk(), EdgeQuantiles::p95))
					.append(" | ")
					.append(q(row.raw(), EdgeQuantiles::max))
					.append(" | ")
					.append(q(row.shrunk(), EdgeQuantiles::max))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendStrategyTable(StringBuilder text, ProbabilityModelComparison comparison) {
		text.append("### Strategy secondary metrics").append('\n').append('\n');
		text.append("| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI |").append('\n');
		text.append("|---|---|---|---|---|").append('\n');
		int n = Math.min(comparison.v1().strategies().size(), comparison.v2().strategies().size());
		for (int i = 0; i < n; i++) {
			StrategySecondarySnapshot s1 = comparison.v1().strategies().get(i);
			StrategySecondarySnapshot s2 = comparison.v2().strategies().get(i);
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
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendCrossLeague(StringBuilder text, ProbabilityModelV2DevelopmentReport report) {
		text.append("## Cross-league summary").append('\n').append('\n');
		text.append("| League | Spearman V1 | Spearman V2 | Pearson V1 | Pearson V2 | log loss V1 | log loss V2 |").append('\n');
		text.append("|---|---|---|---|---|---|---|").append('\n');
		for (ProbabilityModelV2LeagueRun league : report.leagues()) {
			ProbabilityModelComparison comparison = league.comparison();
			text.append("| ")
					.append(title(league.competition()))
					.append(" | ")
					.append(decimal(comparison.v1().rankQuality().spearman()))
					.append(" | ")
					.append(decimal(comparison.v2().rankQuality().spearman()))
					.append(" | ")
					.append(decimal(comparison.v1().rankQuality().pearson()))
					.append(" | ")
					.append(decimal(comparison.v2().rankQuality().pearson()))
					.append(" | ")
					.append(decimal(comparison.v1().scoreLogLoss()))
					.append(" | ")
					.append(decimal(comparison.v2().scoreLogLoss()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendConfidence(StringBuilder text, ProbabilityModelV2DevelopmentReport report) {
		text.append("## Confidence compression").append('\n').append('\n');
		text.append("Question: does shrinkage reduce pathological extreme confidence?").append('\n').append('\n');
		for (ProbabilityModelV2LeagueRun league : report.leagues()) {
			text.append("### ").append(title(league.competition())).append('\n').append('\n');
			appendQuantilePair(text, "predicted edge", league.comparison().v1().predictedEdge(), league.comparison().v2().predictedEdge());
			appendQuantilePair(text, "P(WIN)", league.comparison().v1().predictedWin(), league.comparison().v2().predictedWin());
			appendQuantilePair(text, "P(LOSS)", league.comparison().v1().predictedLoss(), league.comparison().v2().predictedLoss());
			appendQuantilePair(text, "lambdaHome", league.comparison().v1().lambdaHome(), league.comparison().v2().lambdaHome());
			appendQuantilePair(text, "lambdaAway", league.comparison().v1().lambdaAway(), league.comparison().v2().lambdaAway());
		}
	}

	private static void appendQuantilePair(StringBuilder text, String name, EdgeQuantiles v1, EdgeQuantiles v2) {
		text.append("| ").append(name).append(" | p50 | p75 | p90 | p95 | p99 | max |").append('\n');
		text.append("|---|---|---|---|---|---|---|").append('\n');
		appendQuantileRow(text, "V1", v1);
		appendQuantileRow(text, "V2", v2);
		text.append('\n');
	}

	private static void appendQuantileRow(StringBuilder text, String label, EdgeQuantiles quantiles) {
		text.append("| ")
				.append(label)
				.append(" | ")
				.append(decimal(quantiles.median()))
				.append(" | ")
				.append(decimal(quantiles.p75()))
				.append(" | ")
				.append(decimal(quantiles.p90()))
				.append(" | ")
				.append(decimal(quantiles.p95()))
				.append(" | ")
				.append(decimal(quantiles.p99()))
				.append(" | ")
				.append(decimal(quantiles.max()))
				.append(" |")
				.append('\n');
	}

	private static void appendEdgeRanking(StringBuilder text, ProbabilityModelV2DevelopmentReport report) {
		text.append("## Edge ranking").append('\n').append('\n');
		for (ProbabilityModelV2LeagueRun league : report.leagues()) {
			text.append("- ")
					.append(title(league.competition()))
					.append(" Spearman delta=")
					.append(decimal(league.comparison().spearmanDelta()))
					.append(" Pearson delta=")
					.append(decimal(league.comparison().pearsonDelta()))
					.append(" decile inversions ")
					.append(league.comparison().v1().decileRoiInversions())
					.append("→")
					.append(league.comparison().v2().decileRoiInversions())
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendSettlement(StringBuilder text, ProbabilityModelV2DevelopmentReport report) {
		text.append("## Settlement calibration").append('\n').append('\n');
		text.append("| League | ≥10% V1 P(WIN)/actual | ≥10% V2 P(WIN)/actual | ≥10% V1 P(LOSS)/actual | ≥10% V2 P(LOSS)/actual |")
				.append('\n');
		text.append("|---|---|---|---|---|").append('\n');
		for (ProbabilityModelV2LeagueRun league : report.leagues()) {
			HighEdgeCalibrationSnapshot v1 = league.comparison().v1().highEdge10();
			HighEdgeCalibrationSnapshot v2 = league.comparison().v2().highEdge10();
			text.append("| ")
					.append(title(league.competition()))
					.append(" | ")
					.append(decimal(v1.predictedWin()))
					.append("/")
					.append(decimal(v1.actualWin()))
					.append(" | ")
					.append(decimal(v2.predictedWin()))
					.append("/")
					.append(decimal(v2.actualWin()))
					.append(" | ")
					.append(decimal(v1.predictedLoss()))
					.append("/")
					.append(decimal(v1.actualLoss()))
					.append(" | ")
					.append(decimal(v2.predictedLoss()))
					.append("/")
					.append(decimal(v2.actualLoss()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendScoreLikelihood(StringBuilder text, ProbabilityModelV2DevelopmentReport report) {
		text.append("## Score likelihood").append('\n').append('\n');
		for (ProbabilityModelV2LeagueRun league : report.leagues()) {
			text.append("- ")
					.append(title(league.competition()))
					.append(" log loss delta=")
					.append(decimal(league.comparison().logLossDelta()))
					.append(" (positive means v2 is worse)")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendStrategy(StringBuilder text, ProbabilityModelV2DevelopmentReport report) {
		text.append("## Strategy secondary metrics").append('\n').append('\n');
		text.append("Unchanged presets: DEFENSIVE, BALANCED, GROWTH, FLAT_STAKE. ROI is **not** a v2 success gate.")
				.append('\n')
				.append('\n');
		for (ProbabilityModelV2LeagueRun league : report.leagues()) {
			appendStrategyTable(text, league.comparison());
		}
	}

	private static void appendHypotheses(StringBuilder text) {
		text.append("## Hypotheses").append('\n').append('\n');
		text.append("- Independent Poisson plus noisy venue ratios can produce extreme AH settlement tails.")
				.append('\n');
		text.append("- Shrinkage toward league-average strength should compress those tails.")
				.append('\n');
		text.append("- Dixon-Coles should mainly move 0-0 / 1-0 / 0-1 / 1-1 mass, not 1X2 means.")
				.append('\n');
		text.append("- Edge ranking can remain weak even if score means are well calibrated.")
				.append('\n')
				.append('\n');
	}

	private static void appendNonConclusions(StringBuilder text, ProbabilityModelV2DevelopmentReport report) {
		text.append("## Explicit non-conclusions").append('\n').append('\n');
		text.append("- Classification: **").append(report.classification()).append("**").append('\n');
		for (String reason : report.classificationReasons()) {
			text.append("- ").append(reason).append('\n');
		}
		text.append("- This is not a claim that SafeEdge is profitable.").append('\n');
		text.append("- Parameters were not changed after seeing ROI.").append('\n');
		text.append("- La Liga and Ligue 1 remain untouched validation leagues.").append('\n');
		text.append('\n');
	}

	private static String title(CanonicalCompetition competition) {
		return switch (competition) {
			case PREMIER_LEAGUE -> "Premier League";
			case BUNDESLIGA -> "Bundesliga";
			case SERIE_A -> "Serie A";
			default -> competition.name();
		};
	}

	private static void row(StringBuilder text, String name, BigDecimal v1, BigDecimal v2) {
		text.append("| ")
				.append(name)
				.append(" | ")
				.append(decimal(v1))
				.append(" | ")
				.append(decimal(v2))
				.append(" |")
				.append('\n');
	}

	private static String q(EdgeQuantiles quantiles, java.util.function.Function<EdgeQuantiles, BigDecimal> getter) {
		return quantiles == null ? "n/a" : decimal(getter.apply(quantiles));
	}

	private static String percent(BigDecimal value) {
		if (value == null) {
			return "n/a";
		}
		return value.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
	}

	private static String decimal(BigDecimal value) {
		if (value == null) {
			return "n/a";
		}
		return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
