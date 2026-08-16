package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.evaluation.WalkForwardBuildStats;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Baseline 002 markdown. Facts and next hypotheses are separated. No strategy
 * recommendation.
 */
public final class EdgeQualityReportFormatter {

	private EdgeQualityReportFormatter() {
	}

	public static String format(EdgeQualityReport report) {
		return format(report, "# Baseline 002 – Edge Quality / Market Calibration");
	}

	public static String format(EdgeQualityReport report, String title) {
		if (report == null) {
			throw new IllegalArgumentException("report is required");
		}
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("title is required");
		}
		StringBuilder text = new StringBuilder();
		text.append(title).append('\n').append('\n');
		appendConfig(text, report);
		appendAfterConfig(text, report);
		return text.toString();
	}

	static void appendAfterConfig(StringBuilder text, EdgeQualityReport report) {
		appendExecutive(text, report);
		appendSanity(text, report);
		appendRanking(text, report);
		appendEdgeCalibration(text, report);
		appendSettlement(text, report);
		appendCross(text, "6. Edge × odds", report.edgeByOdds(), true);
		appendCross(text, "7. Edge × AH line", report.edgeByAhLine(), true);
		appendGroupTable(text, "7b. Edge × line family", report.edgeByLineFamily());
		appendGroupTable(text, "8. HOME/AWAY × line family", report.sideByLineFamily());
		appendCross(text, "8b. HOME/AWAY × exact AH line", report.sideByAhLine(), true);
		appendSeasons(text, report);
		appendHighEdge(text, report);
		appendOverround(text, report);
		appendDisagreement(text, report);
		appendPositive(text, report);
		appendIntervals(text, report);
		appendRegression(text, report);
		appendObservations(text, report);
		appendNonConclusions(text);
	}

	private static void appendConfig(StringBuilder text, EdgeQualityReport report) {
		WalkForwardBuildStats stats = report.datasetStats();
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
		text.append("- Diagnostics used the already-prepared walk-forward candidates. Poisson was not rebuilt per bucket.")
				.append('\n');
		text.append("- Unit-stake ROI is diagnostic (stake = 1). It is not a production strategy.").append('\n');
		text.append("- Low-sample cells are marked when n < ")
				.append(EdgeQualityGroupSummary.LOW_SAMPLE_THRESHOLD)
				.append('.')
				.append('\n')
				.append('\n');
	}

	private static void appendExecutive(StringBuilder text, EdgeQualityReport report) {
		text.append("## 1. Executive diagnostic summary").append('\n').append('\n');
		text.append("Numbers only. This section does not recommend a betting strategy.").append('\n').append('\n');
		EdgeQualityGroupSummary all = report.allCandidates();
		text.append("- Candidates analyzed: ").append(report.analyzedCandidateCount()).append('\n');
		text.append("- Average predicted edge: ").append(decimal(all.averageEdge())).append('\n');
		text.append("- Average realized unit return: ").append(decimal(all.unitStakeRoi())).append('\n');
		text.append("- Aggregate calibration gap: ").append(decimal(all.calibrationGap())).append('\n');
		text.append("- Spearman(predicted edge, realized unit return): ")
				.append(decimal(report.rankQuality().spearman()))
				.append(" (n=")
				.append(report.rankQuality().n())
				.append("; noisy single-bet outcomes; diagnostic only)")
				.append('\n');
		text.append("- Pearson(predicted edge, realized unit return): ")
				.append(decimal(report.rankQuality().pearson()))
				.append('\n');
		text.append("- Consistency checks passed: ").append(report.consistency().allPassed()).append('\n');
		if (report.datasetStats().quoteSource() == HistoricalQuoteSource.MARKET_AVERAGE) {
			text.append("- MARKET_AVERAGE is not Tippmix.").append('\n');
		}
		text.append('\n');
	}

	private static void appendSanity(StringBuilder text, EdgeQualityReport report) {
		WalkForwardBuildStats stats = report.datasetStats();
		text.append("## 2. Dataset sanity").append('\n').append('\n');
		text.append("- Matches loaded: ").append(stats.matchesLoaded()).append('\n');
		text.append("- Matches evaluated: ").append(stats.matchesEvaluated()).append('\n');
		text.append("- Predictions available: ").append(stats.predictionsAvailable()).append('\n');
		text.append("- Dataset candidates: ").append(stats.candidatesGenerated()).append('\n');
		text.append("- Analyzed candidates: ").append(report.analyzedCandidateCount()).append('\n');
		text.append("- Positive / zero / negative EV: ")
				.append(stats.positiveEvCandidates())
				.append(" / ")
				.append(stats.zeroEvCandidates())
				.append(" / ")
				.append(stats.negativeEvCandidates())
				.append('\n');
		text.append("- Consistency.exhaustiveGroupCounts: ")
				.append(report.consistency().exhaustiveGroupCounts())
				.append('\n');
		text.append("- Consistency.weightedRealizedMatchesGlobal: ")
				.append(report.consistency().weightedRealizedMatchesGlobal())
				.append('\n');
		text.append("- Consistency.expectedReturnMatchesCandidateEngine: ")
				.append(report.consistency().expectedReturnMatchesCandidateEngine())
				.append('\n');
		text.append("- Consistency.unitReturnMatchesPayoutCalculator: ")
				.append(report.consistency().unitReturnMatchesPayoutCalculator())
				.append('\n');
		text.append("- Consistency.inputNotMutated: ").append(report.consistency().inputNotMutated()).append('\n');
		text.append('\n');
	}

	private static void appendRanking(StringBuilder text, EdgeQualityReport report) {
		text.append("## 3. Edge ranking").append('\n').append('\n');
		text.append("Single-bet realized return is noisy. Correlation is diagnostic, not proof of predictive quality.")
				.append('\n')
				.append('\n');
		text.append("- Spearman: ").append(decimal(report.rankQuality().spearman())).append('\n');
		text.append("- Pearson: ").append(decimal(report.rankQuality().pearson())).append('\n').append('\n');
		text.append("Edge deciles (1 = lowest predicted edge, 10 = highest):").append('\n').append('\n');
		appendSummaryTable(text, report.edgeDeciles(), true);
	}

	private static void appendEdgeCalibration(StringBuilder text, EdgeQualityReport report) {
		text.append("## 4. Edge calibration").append('\n').append('\n');
		appendSummaryTable(text, report.edgeBuckets(), true);
	}

	private static void appendSettlement(StringBuilder text, EdgeQualityReport report) {
		text.append("## 5. Settlement probability calibration").append('\n').append('\n');
		text.append("HALF_WIN / HALF_LOSS are not merged into WIN / LOSS.").append('\n').append('\n');
		text.append("### By edge bucket").append('\n').append('\n');
		appendSettlementTable(text, report.settlementByEdgeBucket());
		text.append("### By selected-side AH line").append('\n').append('\n');
		appendSettlementTable(text, report.settlementByAhLine());
		text.append("### By line family").append('\n').append('\n');
		appendSettlementTable(text, report.settlementByLineFamily());
		text.append("### By HOME/AWAY").append('\n').append('\n');
		appendSettlementTable(text, report.settlementBySide());
		text.append("### By odds bucket").append('\n').append('\n');
		appendSettlementTable(text, report.settlementByOddsBucket());
	}

	private static void appendCross(StringBuilder text, String title, java.util.List<CrossCellDiagnostics> cells, boolean markLow) {
		text.append("## ").append(title).append('\n').append('\n');
		if (markLow) {
			text.append("Low-sample cells (n < ")
					.append(EdgeQualityGroupSummary.LOW_SAMPLE_THRESHOLD)
					.append(") are labelled. Positive historical ROI is not a usable filter.")
					.append('\n')
					.append('\n');
		}
		text.append("| Row | Column | n | Low-n | Avg edge | Avg odds | Unit ROI | Pred. E[return] | Gap |")
				.append('\n');
		text.append("| --- | --- | ---: | --- | ---: | ---: | ---: | ---: | ---: |").append('\n');
		for (CrossCellDiagnostics cell : cells) {
			EdgeQualityGroupSummary s = cell.summary();
			text.append("| ")
					.append(cell.rowKey())
					.append(" | ")
					.append(cell.columnKey())
					.append(" | ")
					.append(s.n())
					.append(" | ")
					.append(s.lowSample() ? "yes" : "")
					.append(" | ")
					.append(decimal(s.averageEdge()))
					.append(" | ")
					.append(decimal(s.averageOdds()))
					.append(" | ")
					.append(decimal(s.unitStakeRoi()))
					.append(" | ")
					.append(decimal(s.averageEdge()))
					.append(" | ")
					.append(decimal(s.calibrationGap()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendGroupTable(StringBuilder text, String title, java.util.List<CrossCellDiagnostics> cells) {
		appendCross(text, title, cells, true);
	}

	private static void appendSeasons(StringBuilder text, EdgeQualityReport report) {
		text.append("## 9. Season stability").append('\n').append('\n');
		text.append("| Season | n | Avg edge | Unit ROI | +EV n | +EV ROI | >=3% n | >=3% ROI | >=10% n | >=10% ROI |")
				.append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |").append('\n');
		for (SeasonStabilityRow row : report.seasonStability()) {
			text.append("| ")
					.append(row.seasonDisplay())
					.append(" | ")
					.append(row.candidateCount())
					.append(" | ")
					.append(decimal(row.averageEdge()))
					.append(" | ")
					.append(decimal(row.unitStakeRoi()))
					.append(" | ")
					.append(row.positiveEdgeCount())
					.append(" | ")
					.append(decimal(row.positiveEdgeRoi()))
					.append(" | ")
					.append(row.edgeAtLeast03Count())
					.append(" | ")
					.append(decimal(row.edgeAtLeast03Roi()))
					.append(" | ")
					.append(row.edgeAtLeast10Count())
					.append(" | ")
					.append(decimal(row.edgeAtLeast10Roi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
		appendGroupTable(text, "9b. Season × line family", report.seasonByLineFamily());
		appendGroupTable(text, "9c. Season × edge bucket", report.seasonByEdgeBucket());
	}

	private static void appendHighEdge(StringBuilder text, EdgeQualityReport report) {
		text.append("## 10. High-edge forensics").append('\n').append('\n');
		text.append("Inspection only. Outcomes here must not define a filter.").append('\n').append('\n');
		for (HighEdgeThresholdDiagnostics row : report.highEdgeThresholds()) {
			text.append("### edge >= ").append(row.threshold().toPlainString()).append('\n').append('\n');
			appendOneSummary(text, row.summary());
			text.append("- HOME: ").append(row.homeCount()).append("; AWAY: ").append(row.awayCount()).append('\n');
			text.append("- Lines: ");
			appendSharesInline(text, row.byAhLine());
			text.append("- Seasons: ");
			appendSharesInline(text, row.bySeason());
			text.append('\n');
			appendSettlementTable(text, java.util.List.of(row.summary()));
		}
		text.append("### Top 30 by predicted edge").append('\n').append('\n');
		text.append("| Date | Event | Side | Line | Odds | P(WIN) | P(HW) | P(P) | P(HL) | P(L) | Edge | Settlement | Unit return |")
				.append('\n');
		text.append("| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | ---: |").append('\n');
		for (ForensicCandidateRow row : report.topPredictedEdges()) {
			var p = row.predictedSettlement();
			text.append("| ")
					.append(row.date())
					.append(" | ")
					.append(eventLabel(row))
					.append(" | ")
					.append(row.side())
					.append(" | ")
					.append(row.selectedLine().toPlainString())
					.append(" | ")
					.append(decimal(row.odds()))
					.append(" | ")
					.append(decimal(p.winProbability()))
					.append(" | ")
					.append(decimal(p.halfWinProbability()))
					.append(" | ")
					.append(decimal(p.pushProbability()))
					.append(" | ")
					.append(decimal(p.halfLossProbability()))
					.append(" | ")
					.append(decimal(p.lossProbability()))
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
	}

	private static void appendOverround(StringBuilder text, EdgeQualityReport report) {
		text.append("## 11. Market overround").append('\n').append('\n');
		text.append("Raw inverse-price mass: qHomeRaw = 1/homeOdds, qAwayRaw = 1/awayOdds, overround = qHome + qAway − 1.")
				.append('\n');
		text.append("This is a MARKET-IMPLIED REFERENCE / vig diagnostic, not ground-truth probability and not the SafeEdge model.")
				.append('\n');
		text.append("Each two-sided event is counted once. Odds-bucket overround uses the HOME quote.")
				.append('\n')
				.append('\n');
		TwoSidedCoherence coherence = report.twoSidedCoherence();
		text.append("- Two-sided events: ").append(coherence.twoSidedEvents()).append('\n');
		text.append("- Both sides +EV: ").append(coherence.bothSidesPositiveEdge()).append('\n');
		text.append("- Exactly one side +EV: ").append(coherence.exactlyOneSidePositiveEdge()).append('\n');
		text.append("- Neither side +EV: ").append(coherence.neitherSidePositiveEdge()).append('\n');
		text.append("- Average (HOME edge + AWAY edge): ").append(decimal(coherence.averageHomePlusAwayEdge())).append('\n');
		text.append("- Average overround: ").append(decimal(coherence.averageOverround())).append('\n');
		text.append("- Median overround: ").append(decimal(coherence.medianOverround())).append('\n').append('\n');
		appendOverroundTable(text, "By season", report.overroundBySeason());
		appendOverroundTable(text, "By AH market home line", report.overroundByAhLine());
		appendOverroundTable(text, "By HOME odds bucket", report.overroundByOddsBucket());
	}

	private static void appendOverroundTable(StringBuilder text, String title, java.util.List<OverroundGroup> rows) {
		text.append("### ").append(title).append('\n').append('\n');
		text.append("| Key | Events | Avg overround | Median overround | Low-n |").append('\n');
		text.append("| --- | ---: | ---: | ---: | --- |").append('\n');
		for (OverroundGroup row : rows) {
			text.append("| ")
					.append(row.key())
					.append(" | ")
					.append(row.eventCount())
					.append(" | ")
					.append(decimal(row.averageOverround()))
					.append(" | ")
					.append(decimal(row.medianOverround()))
					.append(" | ")
					.append(row.lowSample() ? "yes" : "")
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendDisagreement(StringBuilder text, EdgeQualityReport report) {
		text.append("## 12. Model-vs-market disagreement").append('\n').append('\n');
		text.append("Disagreement magnitude is |predicted edge|. Edge is CandidateEngine expected return at the quoted price.")
				.append('\n');
		text.append("This does not replace CandidateEngine probabilities with 1/odds.").append('\n').append('\n');
		appendSummaryTable(text, report.disagreementGroups().stream().map(DisagreementGroup::summary).toList(), true);
	}

	private static void appendPositive(StringBuilder text, EdgeQualityReport report) {
		text.append("## 13. Positive-handicap investigation").append('\n').append('\n');
		text.append("No production filter. No best-line selection.").append('\n').append('\n');
		text.append("| Line | n | Low-n | +EV n | Avg edge | Avg odds | All ROI | +EV ROI | >=3% ROI | >=10% ROI |")
				.append('\n');
		text.append("| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |").append('\n');
		for (PositiveLineForensics row : report.positiveHandicapLines()) {
			text.append("| ")
					.append(row.selectedLine().toPlainString())
					.append(" | ")
					.append(row.all().n())
					.append(" | ")
					.append(row.all().lowSample() ? "yes" : "")
					.append(" | ")
					.append(row.positiveEdgeOnly().n())
					.append(" | ")
					.append(decimal(row.all().averageEdge()))
					.append(" | ")
					.append(decimal(row.all().averageOdds()))
					.append(" | ")
					.append(decimal(row.all().unitStakeRoi()))
					.append(" | ")
					.append(decimal(row.positiveEdgeOnly().unitStakeRoi()))
					.append(" | ")
					.append(decimal(row.edgeAtLeast03().unitStakeRoi()))
					.append(" | ")
					.append(decimal(row.edgeAtLeast10().unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
		text.append("Settlement calibration by positive line:").append('\n').append('\n');
		appendSettlementTable(
				text, report.positiveHandicapLines().stream().map(PositiveLineForensics::all).toList());
	}

	private static void appendIntervals(StringBuilder text, EdgeQualityReport report) {
		text.append("## 14. Statistical uncertainty").append('\n').append('\n');
		text.append("Deterministic percentile bootstrap of the unit-stake mean. Seed=")
				.append(DeterministicBootstrap.SEED)
				.append(", replicates=")
				.append(DeterministicBootstrap.REPLICATES)
				.append(". 95% interval from 2.5th and 97.5th percentiles of bootstrap means.")
				.append('\n');
		text.append("CI excluding 0 is not proof of future profitability.").append('\n').append('\n');
		text.append("| Group | n | Mean | 95% low | 95% high |").append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: |").append('\n');
		for (NamedMeanInterval row : report.confidenceIntervals()) {
			MeanConfidenceInterval interval = row.interval();
			text.append("| ")
					.append(row.label())
					.append(" | ")
					.append(interval.n())
					.append(" | ")
					.append(decimal(interval.mean()))
					.append(" | ")
					.append(decimal(interval.lower95()))
					.append(" | ")
					.append(decimal(interval.upper95()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendRegression(StringBuilder text, EdgeQualityReport report) {
		text.append("## 15. Baseline 001 regression check").append('\n').append('\n');
		text.append("Same dataset and StrategyPresetFactory configs. Diagnostics must not change strategy results.")
				.append('\n')
				.append('\n');
		if (report.strategyRegression().isEmpty()) {
			text.append("No strategy backtests were supplied to this diagnostics pass.").append('\n').append('\n');
			return;
		}
		text.append("| Strategy | Accepted | ROI | Paused |").append('\n');
		text.append("| --- | ---: | ---: | --- |").append('\n');
		for (StrategyRegressionSnapshot row : report.strategyRegression()) {
			text.append("| ")
					.append(row.name())
					.append(" | ")
					.append(row.betsAccepted())
					.append(" | ")
					.append(decimal(row.roi()))
					.append(" | ")
					.append(row.pausedByDrawdown())
					.append(" |")
					.append('\n');
		}
		text.append('\n');
		if (report.datasetStats().competition() == CanonicalCompetition.PREMIER_LEAGUE) {
			text.append("Baseline 001 reference (unchanged parameters): DEFENSIVE ≈ −3.03%, BALANCED ≈ −2.11%, GROWTH ≈ −2.16%, FLAT ≈ −3.82%; all paused.")
					.append('\n')
					.append('\n');
		}
	}

	private static void appendObservations(StringBuilder text, EdgeQualityReport report) {
		text.append("## 16. Observations").append('\n').append('\n');
		text.append("### FACTS").append('\n').append('\n');
		EdgeQualityGroupSummary all = report.allCandidates();
		text.append("- n=").append(all.n())
				.append(" avgEdge=").append(decimal(all.averageEdge()))
				.append(" realized=").append(decimal(all.unitStakeRoi()))
				.append(" gap=").append(decimal(all.calibrationGap()))
				.append('\n');
		text.append("- Spearman=").append(decimal(report.rankQuality().spearman()))
				.append(" Pearson=").append(decimal(report.rankQuality().pearson()))
				.append('\n');
		for (EdgeQualityGroupSummary bucket : report.edgeBuckets()) {
			text.append("- Edge bucket ").append(bucket.key())
					.append(": n=").append(bucket.n())
					.append(" avgEdge=").append(decimal(bucket.averageEdge()))
					.append(" realized=").append(decimal(bucket.unitStakeRoi()))
					.append(" P(WIN) pred/act=")
					.append(decimal(bucket.settlementCalibration().win().averagePredictedProbability()))
					.append("/")
					.append(decimal(bucket.settlementCalibration().win().actualFrequency()))
					.append('\n');
		}
		text.append("- Two-sided both-+EV events: ")
				.append(report.twoSidedCoherence().bothSidesPositiveEdge())
				.append(" / ")
				.append(report.twoSidedCoherence().twoSidedEvents())
				.append('\n');
		text.append('\n');
		text.append("### HYPOTHESES TO TEST NEXT").append('\n').append('\n');
		text.append("- Independent Poisson may be mean-calibrated on 1X2/goals while overconfident on AH settlement tails.")
				.append('\n');
		text.append("- Large |edge| may mark model-vs-market disagreement rather than exploitable value.")
				.append('\n');
		text.append("- Apparent line-family differences may be sampling noise; intervals in section 14 bound that claim.")
				.append('\n');
		text.append("- These hypotheses are not implemented as filters in this task.").append('\n').append('\n');
	}

	private static void appendNonConclusions(StringBuilder text) {
		text.append("## 17. Explicit non-conclusions").append('\n').append('\n');
		text.append("- no parameter optimization performed").append('\n');
		text.append("- no production filter selected").append('\n');
		text.append("- best-looking cell is not a validated strategy").append('\n');
		text.append("- historical pattern may be noise").append('\n');
		text.append("- CI excluding 0 is not proof of future profitability").append('\n');
		text.append("- MARKET_AVERAGE is not Tippmix").append('\n');
		text.append("- 1/odds is not true AH probability").append('\n');
		text.append("- de-vig / overround figures are market references, not SafeEdge model output").append('\n');
	}

	private static void appendSummaryTable(StringBuilder text, java.util.List<EdgeQualityGroupSummary> rows, boolean includeMedian) {
		text.append("| Group | n | Low-n | Avg edge |");
		if (includeMedian) {
			text.append(" Median edge |");
		}
		text.append(" Avg odds | Unit ROI | Pred. profit | Realized profit | Gap |").append('\n');
		text.append("| --- | ---: | --- | ---: |");
		if (includeMedian) {
			text.append(" ---: |");
		}
		text.append(" ---: | ---: | ---: | ---: | ---: |").append('\n');
		for (EdgeQualityGroupSummary row : rows) {
			text.append("| ")
					.append(row.key())
					.append(" | ")
					.append(row.n())
					.append(" | ")
					.append(row.lowSample() ? "yes" : "")
					.append(" | ")
					.append(decimal(row.averageEdge()));
			if (includeMedian) {
				text.append(" | ").append(decimal(row.medianEdge()));
			}
			text.append(" | ")
					.append(decimal(row.averageOdds()))
					.append(" | ")
					.append(decimal(row.unitStakeRoi()))
					.append(" | ")
					.append(decimal(row.predictedExpectedProfit()))
					.append(" | ")
					.append(decimal(row.realizedProfit()))
					.append(" | ")
					.append(decimal(row.calibrationGap()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendOneSummary(StringBuilder text, EdgeQualityGroupSummary row) {
		text.append("- n=").append(row.n())
				.append(row.lowSample() ? " (low-sample)" : "")
				.append(" avgEdge=").append(decimal(row.averageEdge()))
				.append(" medianEdge=").append(decimal(row.medianEdge()))
				.append(" avgOdds=").append(decimal(row.averageOdds()))
				.append(" unitROI=").append(decimal(row.unitStakeRoi()))
				.append(" gap=").append(decimal(row.calibrationGap()))
				.append('\n');
	}

	private static void appendSettlementTable(StringBuilder text, java.util.List<EdgeQualityGroupSummary> rows) {
		text.append("| Group | n | P(WIN) pred/act/gap | P(HW) pred/act/gap | P(PUSH) pred/act/gap | P(HL) pred/act/gap | P(LOSS) pred/act/gap | Pred E[r] | Realized | Gap |")
				.append('\n');
		text.append("| --- | ---: | --- | --- | --- | --- | --- | ---: | ---: | ---: |").append('\n');
		for (EdgeQualityGroupSummary row : rows) {
			SettlementCalibration c = row.settlementCalibration();
			text.append("| ")
					.append(row.key())
					.append(" | ")
					.append(row.n())
					.append(" | ")
					.append(triple(c.win()))
					.append(" | ")
					.append(triple(c.halfWin()))
					.append(" | ")
					.append(triple(c.push()))
					.append(" | ")
					.append(triple(c.halfLoss()))
					.append(" | ")
					.append(triple(c.loss()))
					.append(" | ")
					.append(decimal(row.averageEdge()))
					.append(" | ")
					.append(decimal(row.unitStakeRoi()))
					.append(" | ")
					.append(decimal(row.calibrationGap()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendSharesInline(StringBuilder text, java.util.List<ConcentrationShare> shares) {
		if (shares.isEmpty()) {
			text.append("none").append('\n');
			return;
		}
		boolean first = true;
		for (ConcentrationShare share : shares) {
			if (!first) {
				text.append("; ");
			}
			first = false;
			text.append(share.key()).append("=").append(share.count());
		}
		text.append('\n');
	}

	private static String triple(OutcomeCalibration outcome) {
		return decimal(outcome.averagePredictedProbability())
				+ "/"
				+ decimal(outcome.actualFrequency())
				+ "/"
				+ decimal(outcome.gap());
	}

	private static String eventLabel(ForensicCandidateRow row) {
		if (row.homeTeam() != null && row.awayTeam() != null) {
			return row.homeTeam() + " vs " + row.awayTeam();
		}
		return row.eventId();
	}

	private static String decimal(BigDecimal value) {
		if (value == null) {
			return "n/a";
		}
		return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
