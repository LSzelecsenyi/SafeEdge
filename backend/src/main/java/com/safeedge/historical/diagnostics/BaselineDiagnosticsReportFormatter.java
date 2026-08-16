package com.safeedge.historical.diagnostics;

import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.evaluation.WalkForwardBuildStats;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Markdown autopsy report. Facts only. Does not recommend thresholds or presets.
 */
public final class BaselineDiagnosticsReportFormatter {

	private BaselineDiagnosticsReportFormatter() {
	}

	public static String format(BaselineDiagnosticsReport report) {
		if (report == null) {
			throw new IllegalArgumentException("report is required");
		}
		StringBuilder text = new StringBuilder();
		text.append("# Baseline 001 Diagnostics").append('\n').append('\n');
		appendExperiment(text, report.overview().datasetStats());
		appendOverview(text, report);
		appendEdgeCalibration(text, report);
		appendPositiveThresholds(text, report);
		appendOdds(text, report);
		appendAhLines(text, report);
		appendSides(text, report);
		appendSeasons(text, report);
		appendGoalsAndMargins(text, report);
		appendQuantiles(text, report);
		appendHypothesis(text, report);
		appendStrategies(text, report);
		appendObservations(text, report);
		appendNonConclusions(text);
		return text.toString();
	}

	private static void appendExperiment(StringBuilder text, WalkForwardBuildStats stats) {
		HistoricalQuoteSource quoteSource = stats.quoteSource();
		text.append("## Experiment configuration").append('\n').append('\n');
		text.append("- Competition: ").append(stats.competition()).append('\n');
		text.append("- Training from season: ").append(stats.trainingFromSeason()).append('\n');
		text.append("- Evaluation range: ")
				.append(stats.evaluationFromSeason())
				.append(" → ")
				.append(stats.evaluationToSeason())
				.append('\n');
		text.append("- HISTORICAL QUOTE SOURCE = ").append(quoteSource).append('\n');
		text.append("- These prices are football-data.co.uk historical quotes, not Tippmix odds.").append('\n');
		text.append("- Model: current independent time-decayed Poisson defaults (not retuned).").append('\n');
		text.append("- Strategies: unchanged DEFENSIVE / BALANCED / GROWTH / FLAT_STAKE.").append('\n');
		text.append("- Diagnostics used the already-prepared walk-forward candidates and one backtest per strategy.")
				.append('\n');
		text.append("- Unit-stake ROI is a diagnostic (stake = 1 on every candidate in the group). It is not a production strategy.")
				.append('\n')
				.append('\n');
	}

	private static void appendOverview(StringBuilder text, BaselineDiagnosticsReport report) {
		WalkForwardBuildStats stats = report.overview().datasetStats();
		UnitStakeSummary all = report.overview().allCandidates();
		text.append("## Candidate overview").append('\n').append('\n');
		text.append("- Matches loaded: ").append(stats.matchesLoaded()).append('\n');
		text.append("- Matches evaluated: ").append(stats.matchesEvaluated()).append('\n');
		text.append("- Predictions available: ").append(stats.predictionsAvailable()).append('\n');
		text.append("- Candidates generated (dataset): ").append(stats.candidatesGenerated()).append('\n');
		text.append("- Candidates analyzed: ").append(report.overview().analyzedCandidateCount()).append('\n');
		text.append("- Positive / zero / negative EV: ")
				.append(stats.positiveEvCandidates())
				.append(" / ")
				.append(stats.zeroEvCandidates())
				.append(" / ")
				.append(stats.negativeEvCandidates())
				.append('\n');
		text.append("- Average candidate edge (dataset): ").append(decimal(stats.averageCandidateEdge())).append('\n');
		text.append("- Average unit-stake realized return (all candidates): ")
				.append(decimal(all.averageRealizedReturnRate()))
				.append('\n');
		text.append("- Calibration gap (realized − predicted): ").append(decimal(all.calibrationGap())).append('\n');
		text.append("- Negative-edge unit-stake ROI: ")
				.append(decimal(report.edgeSign().negativeEdge().unitStakeRoi()))
				.append(" (n=")
				.append(report.edgeSign().negativeEdge().candidateCount())
				.append(')')
				.append('\n');
		text.append("- Positive-edge unit-stake ROI: ")
				.append(decimal(report.edgeSign().positiveEdge().unitStakeRoi()))
				.append(" (n=")
				.append(report.edgeSign().positiveEdge().candidateCount())
				.append(')')
				.append('\n')
				.append('\n');
		text.append("Positive-EV concentration (share of edge > 0 candidates):").append('\n').append('\n');
		appendConcentration(text, "HOME/AWAY", report.positiveEdgeConcentration().bySide());
		appendConcentration(text, "AH line", report.positiveEdgeConcentration().byAhLine());
		appendConcentration(text, "Odds bucket", report.positiveEdgeConcentration().byOddsBucket());
		appendConcentration(text, "Season", report.positiveEdgeConcentration().bySeason());
		text.append('\n');
	}

	private static void appendConcentration(StringBuilder text, String title, List<ConcentrationShare> shares) {
		text.append("- ").append(title).append(':').append('\n');
		if (shares.isEmpty()) {
			text.append("  - none").append('\n');
			return;
		}
		for (ConcentrationShare share : shares) {
			text.append("  - ")
					.append(share.key())
					.append(": ")
					.append(share.count())
					.append(" (")
					.append(percent(share.shareOfPositiveEdge()))
					.append(')')
					.append('\n');
		}
	}

	private static void appendEdgeCalibration(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## Edge calibration").append('\n').append('\n');
		text.append("| Bucket | Count | Avg edge | Avg odds | WIN | HALF_WIN | PUSH | HALF_LOSS | LOSS | Avg realized | Unit-stake ROI | Gap |")
				.append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |").append('\n');
		for (EdgeBucketDiagnostics row : report.edgeBuckets()) {
			appendSummaryRow(text, row.bucket().label(), row.summary());
		}
		text.append('\n');
	}

	private static void appendPositiveThresholds(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## Positive-edge threshold diagnostics").append('\n').append('\n');
		text.append("Diagnostic thresholds only. Not StrategyConfig.").append('\n').append('\n');
		text.append("| Subset | Count | Avg edge | Unit-stake ROI |").append('\n');
		text.append("| --- | ---: | ---: | ---: |").append('\n');
		for (CandidateSubsetDiagnostics row : report.positiveEdgeThresholds()) {
			text.append("| ")
					.append(row.label())
					.append(" | ")
					.append(row.summary().candidateCount())
					.append(" | ")
					.append(decimal(row.summary().averagePredictedEdge()))
					.append(" | ")
					.append(decimal(row.summary().unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendOdds(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## Odds buckets").append('\n').append('\n');
		text.append("| Bucket | Count | +EV | Avg edge | Unit-stake ROI |").append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: |").append('\n');
		for (OddsBucketDiagnostics row : report.oddsBuckets()) {
			text.append("| ")
					.append(row.bucket().label())
					.append(" | ")
					.append(row.summary().candidateCount())
					.append(" | ")
					.append(row.summary().positiveEdgeCount())
					.append(" | ")
					.append(decimal(row.summary().averagePredictedEdge()))
					.append(" | ")
					.append(decimal(row.summary().unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendAhLines(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## AH lines").append('\n').append('\n');
		text.append("Exact selected-side handicap. Quarter-lines are not merged.").append('\n').append('\n');
		text.append("| Line | Count | +EV | Avg edge | Avg odds | Unit-stake ROI |").append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: | ---: |").append('\n');
		for (AhLineDiagnostics row : report.ahLines()) {
			text.append("| ")
					.append(row.selectedLine().toPlainString())
					.append(" | ")
					.append(row.summary().candidateCount())
					.append(" | ")
					.append(row.summary().positiveEdgeCount())
					.append(" | ")
					.append(decimal(row.summary().averagePredictedEdge()))
					.append(" | ")
					.append(decimal(row.summary().averageOdds()))
					.append(" | ")
					.append(decimal(row.summary().unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
		text.append("Line-family summary:").append('\n').append('\n');
		text.append("| Family | Count | +EV | Avg edge | Unit-stake ROI |").append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: |").append('\n');
		for (LineFamilyDiagnostics row : report.lineFamilies()) {
			text.append("| ")
					.append(row.family())
					.append(" | ")
					.append(row.summary().candidateCount())
					.append(" | ")
					.append(row.summary().positiveEdgeCount())
					.append(" | ")
					.append(decimal(row.summary().averagePredictedEdge()))
					.append(" | ")
					.append(decimal(row.summary().unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendSides(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## HOME vs AWAY").append('\n').append('\n');
		text.append("| Side | Count | +EV | Avg edge | Avg odds | WIN | HALF_WIN | PUSH | HALF_LOSS | LOSS | Unit-stake ROI |")
				.append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |").append('\n');
		for (SideDiagnostics row : report.sides()) {
			UnitStakeSummary summary = row.summary();
			SettlementCounts s = summary.settlements();
			text.append("| ")
					.append(row.side())
					.append(" | ")
					.append(summary.candidateCount())
					.append(" | ")
					.append(summary.positiveEdgeCount())
					.append(" | ")
					.append(decimal(summary.averagePredictedEdge()))
					.append(" | ")
					.append(decimal(summary.averageOdds()))
					.append(" | ")
					.append(s.win())
					.append(" | ")
					.append(s.halfWin())
					.append(" | ")
					.append(s.push())
					.append(" | ")
					.append(s.halfLoss())
					.append(" | ")
					.append(s.loss())
					.append(" | ")
					.append(decimal(summary.unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendSeasons(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## Seasons").append('\n').append('\n');
		text.append("| Season | Predictions | Candidates | +EV | Avg edge | Unit-stake ROI |").append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: | ---: |").append('\n');
		for (SeasonDiagnostics row : report.seasons()) {
			text.append("| ")
					.append(row.seasonDisplay())
					.append(" | ")
					.append(row.predictionCount())
					.append(" | ")
					.append(row.candidates().candidateCount())
					.append(" | ")
					.append(row.candidates().positiveEdgeCount())
					.append(" | ")
					.append(decimal(row.candidates().averagePredictedEdge()))
					.append(" | ")
					.append(decimal(row.candidates().unitStakeRoi()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	static void appendGoalsAndMargins(StringBuilder text, BaselineDiagnosticsReport report) {
		GoalCalibrationDiagnostics goals = report.goalCalibration();
		text.append("## Goal / margin calibration").append('\n').append('\n');
		text.append("No odds involved. Predicted values are sums of the captured score distribution.")
				.append('\n')
				.append('\n');
		text.append("- Predictions: ").append(goals.predictionCount()).append('\n');
		text.append("- Predicted vs actual home goals: ")
				.append(decimal(goals.averagePredictedHomeGoals()))
				.append(" vs ")
				.append(decimal(goals.averageActualHomeGoals()))
				.append('\n');
		text.append("- Predicted vs actual away goals: ")
				.append(decimal(goals.averagePredictedAwayGoals()))
				.append(" vs ")
				.append(decimal(goals.averageActualAwayGoals()))
				.append('\n');
		text.append("- Predicted vs actual total goals: ")
				.append(decimal(goals.averagePredictedTotalGoals()))
				.append(" vs ")
				.append(decimal(goals.averageActualTotalGoals()))
				.append('\n');
		text.append("- Predicted vs actual home-win: ")
				.append(decimal(goals.averagePredictedHomeWinProbability()))
				.append(" vs ")
				.append(decimal(goals.actualHomeWinFrequency()))
				.append('\n');
		text.append("- Predicted vs actual draw: ")
				.append(decimal(goals.averagePredictedDrawProbability()))
				.append(" vs ")
				.append(decimal(goals.actualDrawFrequency()))
				.append('\n');
		text.append("- Predicted vs actual away-win: ")
				.append(decimal(goals.averagePredictedAwayWinProbability()))
				.append(" vs ")
				.append(decimal(goals.actualAwayWinFrequency()))
				.append('\n')
				.append('\n');
		text.append("| Margin category | Predicted | Actual frequency | Actual count |").append('\n');
		text.append("| --- | ---: | ---: | ---: |").append('\n');
		for (MarginCategoryCalibration row : report.marginCalibration().categories()) {
			text.append("| ")
					.append(row.category())
					.append(" | ")
					.append(decimal(row.averagePredictedProbability()))
					.append(" | ")
					.append(decimal(row.actualFrequency()))
					.append(" | ")
					.append(row.actualCount())
					.append(" |")
					.append('\n');
		}
		text.append('\n');
		text.append("| Exact home margin | Predicted | Actual frequency | Actual count |").append('\n');
		text.append("| --- | ---: | ---: | ---: |").append('\n');
		for (ExactMarginCalibration row : report.marginCalibration().exactMargins()) {
			text.append("| ")
					.append(row.bucket().label())
					.append(" | ")
					.append(decimal(row.averagePredictedProbability()))
					.append(" | ")
					.append(decimal(row.actualFrequency()))
					.append(" | ")
					.append(row.actualCount())
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendQuantiles(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## Edge quantiles").append('\n').append('\n');
		text.append("Nearest-rank: index = round_half_up(p × (n − 1)).").append('\n').append('\n');
		text.append("| Group | min | p10 | p25 | median | p75 | p90 | p95 | p99 | max |").append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |").append('\n');
		appendQuantileRow(text, "All candidates", report.allCandidateEdgeQuantiles());
		appendQuantileRow(text, "Positive-EV only", report.positiveEdgeQuantiles());
		text.append('\n');
	}

	private static void appendQuantileRow(StringBuilder text, String label, EdgeQuantiles q) {
		text.append("| ")
				.append(label)
				.append(" | ")
				.append(decimal(q.min()))
				.append(" | ")
				.append(decimal(q.p10()))
				.append(" | ")
				.append(decimal(q.p25()))
				.append(" | ")
				.append(decimal(q.median()))
				.append(" | ")
				.append(decimal(q.p75()))
				.append(" | ")
				.append(decimal(q.p90()))
				.append(" | ")
				.append(decimal(q.p95()))
				.append(" | ")
				.append(decimal(q.p99()))
				.append(" | ")
				.append(decimal(q.max()))
				.append(" |")
				.append('\n');
	}

	private static void appendHypothesis(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## Original 1.15–1.35 hypothesis").append('\n').append('\n');
		text.append("Diagnostic subset only. Not a production filter.").append('\n').append('\n');
		text.append("| Subset | Count | Avg edge | Unit-stake ROI | WIN | HALF_WIN | PUSH | HALF_LOSS | LOSS |")
				.append('\n');
		text.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |").append('\n');
		for (CandidateSubsetDiagnostics row : report.originalOddsRangeSubsets()) {
			SettlementCounts s = row.summary().settlements();
			text.append("| ")
					.append(row.label())
					.append(" | ")
					.append(row.summary().candidateCount())
					.append(" | ")
					.append(decimal(row.summary().averagePredictedEdge()))
					.append(" | ")
					.append(decimal(row.summary().unitStakeRoi()))
					.append(" | ")
					.append(s.win())
					.append(" | ")
					.append(s.halfWin())
					.append(" | ")
					.append(s.push())
					.append(" | ")
					.append(s.halfLoss())
					.append(" | ")
					.append(s.loss())
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendStrategies(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## Strategy accepted-bet composition").append('\n').append('\n');
		text.append("Denominator is StrategyEngine accepted bets only. Unit-stake ROI is stake=1 on those same identities.")
				.append('\n')
				.append('\n');
		for (StrategyAcceptedBetDiagnostics strategy : report.strategyAcceptedBets()) {
			text.append("### ").append(strategy.strategyName()).append('\n').append('\n');
			text.append("- Accepted bets: ").append(strategy.acceptedCount()).append('\n');
			text.append("- Average edge of accepted identities: ").append(decimal(strategy.averageEdge())).append('\n');
			text.append("- Unit-stake ROI of accepted identities: ").append(decimal(strategy.unitStakeRoi())).append('\n');
			text.append("- HOME count: ").append(sideCount(strategy, SelectionType.HOME)).append('\n');
			text.append("- AWAY count: ").append(sideCount(strategy, SelectionType.AWAY)).append('\n');
			text.append('\n');
			text.append("Accepted by edge bucket:").append('\n').append('\n');
			text.append("| Bucket | Count | Avg edge | Unit-stake ROI |").append('\n');
			text.append("| --- | ---: | ---: | ---: |").append('\n');
			for (EdgeBucketDiagnostics row : strategy.byEdgeBucket()) {
				text.append("| ")
						.append(row.bucket().label())
						.append(" | ")
						.append(row.summary().candidateCount())
						.append(" | ")
						.append(decimal(row.summary().averagePredictedEdge()))
						.append(" | ")
						.append(decimal(row.summary().unitStakeRoi()))
						.append(" |")
						.append('\n');
			}
			text.append('\n');
			text.append("Accepted by odds bucket:").append('\n').append('\n');
			text.append("| Bucket | Count | Unit-stake ROI |").append('\n');
			text.append("| --- | ---: | ---: |").append('\n');
			for (OddsBucketDiagnostics row : strategy.byOddsBucket()) {
				if (row.summary().candidateCount() == 0) {
					continue;
				}
				text.append("| ")
						.append(row.bucket().label())
						.append(" | ")
						.append(row.summary().candidateCount())
						.append(" | ")
						.append(decimal(row.summary().unitStakeRoi()))
						.append(" |")
						.append('\n');
			}
			text.append('\n');
			text.append("Accepted by AH line:").append('\n').append('\n');
			text.append("| Line | Count | Unit-stake ROI |").append('\n');
			text.append("| --- | ---: | ---: |").append('\n');
			for (AhLineDiagnostics row : strategy.byAhLine()) {
				text.append("| ")
						.append(row.selectedLine().toPlainString())
						.append(" | ")
						.append(row.summary().candidateCount())
						.append(" | ")
						.append(decimal(row.summary().unitStakeRoi()))
						.append(" |")
						.append('\n');
			}
			text.append('\n');
		}
		appendDrawdownStops(text, report);
	}

	private static void appendDrawdownStops(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## Drawdown stop details").append('\n').append('\n');
		if (report.strategyAcceptedBets().isEmpty()) {
			text.append("No strategy results.").append('\n').append('\n');
			return;
		}
		for (StrategyAcceptedBetDiagnostics strategy : report.strategyAcceptedBets()) {
			text.append("### ").append(strategy.strategyName()).append('\n').append('\n');
			appendPauseBody(text, strategy.pause());
		}
	}

	private static void appendPauseBody(StringBuilder text, DrawdownPauseDiagnostics pause) {
		if (!pause.paused()) {
			text.append("Not paused by drawdown.").append('\n').append('\n');
			return;
		}
		text.append("- Paused: true").append('\n');
		text.append("- Opportunity index (0-based, first skipped-by-pause): ")
				.append(value(pause.opportunityIndex()))
				.append('\n');
		text.append("- Pause betting date: ").append(date(pause.pauseBettingDate())).append('\n');
		text.append("- Pause decisionAt: ").append(instant(pause.pauseDecisionAt())).append('\n');
		text.append("- Accepted bets before pause: ").append(pause.acceptedBetsBeforePause()).append('\n');
		text.append("- Active bankroll at pause: ").append(decimal(pause.activeBankrollAtPause())).append('\n');
		text.append("- Active drawdown at pause: ").append(decimal(pause.activeDrawdownAtPause())).append('\n');
		text.append("- Total equity at pause: ").append(decimal(pause.totalEquityAtPause())).append('\n');
		text.append('\n');
		text.append("Last settled bets before pause:").append('\n').append('\n');
		if (pause.lastSettledBetsBeforePause().isEmpty()) {
			text.append("None with settlementAt <= pause decisionAt.").append('\n').append('\n');
			return;
		}
		text.append("| Date | Side | Line | Odds | Edge | Settlement | Profit |").append('\n');
		text.append("| --- | --- | ---: | ---: | ---: | --- | ---: |").append('\n');
		for (SettledBetSnapshot bet : pause.lastSettledBetsBeforePause()) {
			text.append("| ")
					.append(bet.bettingDate())
					.append(" | ")
					.append(bet.side())
					.append(" | ")
					.append(bet.selectedLine().toPlainString())
					.append(" | ")
					.append(decimal(bet.odds()))
					.append(" | ")
					.append(decimal(bet.edge()))
					.append(" | ")
					.append(bet.settlement())
					.append(" | ")
					.append(decimal(bet.profit()))
					.append(" |")
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendObservations(StringBuilder text, BaselineDiagnosticsReport report) {
		text.append("## Observations").append('\n').append('\n');
		text.append("Facts only. No recommended threshold, preset, or proven edge.").append('\n').append('\n');
		UnitStakeSummary all = report.overview().allCandidates();
		text.append("- Analyzed candidate count equals dataset candidate count: ")
				.append(report.overview().analyzedCandidateCount())
				.append(" vs ")
				.append(report.overview().datasetStats().candidatesGenerated())
				.append('\n');
		text.append("- All-candidate average predicted edge: ")
				.append(decimal(all.averagePredictedEdge()))
				.append("; average realized unit return: ")
				.append(decimal(all.averageRealizedReturnRate()))
				.append("; gap: ")
				.append(decimal(all.calibrationGap()))
				.append('\n');
		text.append("- Negative-edge n=")
				.append(report.edgeSign().negativeEdge().candidateCount())
				.append(" unit ROI=")
				.append(decimal(report.edgeSign().negativeEdge().unitStakeRoi()))
				.append("; positive-edge n=")
				.append(report.edgeSign().positiveEdge().candidateCount())
				.append(" unit ROI=")
				.append(decimal(report.edgeSign().positiveEdge().unitStakeRoi()))
				.append('\n');
		for (EdgeBucketDiagnostics row : report.edgeBuckets()) {
			text.append("- Edge bucket ")
					.append(row.bucket().label())
					.append(": n=")
					.append(row.summary().candidateCount())
					.append(" avgEdge=")
					.append(decimal(row.summary().averagePredictedEdge()))
					.append(" realized=")
					.append(decimal(row.summary().averageRealizedReturnRate()))
					.append('\n');
		}
		for (SideDiagnostics row : report.sides()) {
			text.append("- ")
					.append(row.side())
					.append(": n=")
					.append(row.summary().candidateCount())
					.append(" +EV=")
					.append(row.summary().positiveEdgeCount())
					.append(" unit ROI=")
					.append(decimal(row.summary().unitStakeRoi()))
					.append('\n');
		}
		for (SeasonDiagnostics row : report.seasons()) {
			text.append("- Season ")
					.append(row.seasonDisplay())
					.append(": predictions=")
					.append(row.predictionCount())
					.append(" candidates=")
					.append(row.candidates().candidateCount())
					.append(" unit ROI=")
					.append(decimal(row.candidates().unitStakeRoi()))
					.append('\n');
		}
		for (StrategyAcceptedBetDiagnostics strategy : report.strategyAcceptedBets()) {
			text.append("- Strategy ")
					.append(strategy.strategyName())
					.append(": accepted=")
					.append(strategy.acceptedCount())
					.append(" paused=")
					.append(strategy.pause().paused())
					.append(" pauseDate=")
					.append(date(strategy.pause().pauseBettingDate()))
					.append('\n');
		}
		text.append('\n');
	}

	private static void appendNonConclusions(StringBuilder text) {
		text.append("## Explicit non-conclusions").append('\n').append('\n');
		text.append("- no parameter optimization performed").append('\n');
		text.append("- best-looking bucket is not validated strategy").append('\n');
		text.append("- historical pattern may be noise").append('\n');
		text.append("- MARKET_AVERAGE is not Tippmix").append('\n');
		text.append("- HISTORICAL QUOTE SOURCE prices are football-data.co.uk quotes, never Tippmix odds").append('\n');
	}

	private static void appendSummaryRow(StringBuilder text, String label, UnitStakeSummary summary) {
		SettlementCounts s = summary.settlements();
		text.append("| ")
				.append(label)
				.append(" | ")
				.append(summary.candidateCount())
				.append(" | ")
				.append(decimal(summary.averagePredictedEdge()))
				.append(" | ")
				.append(decimal(summary.averageOdds()))
				.append(" | ")
				.append(s.win())
				.append(" | ")
				.append(s.halfWin())
				.append(" | ")
				.append(s.push())
				.append(" | ")
				.append(s.halfLoss())
				.append(" | ")
				.append(s.loss())
				.append(" | ")
				.append(decimal(summary.averageRealizedReturnRate()))
				.append(" | ")
				.append(decimal(summary.unitStakeRoi()))
				.append(" | ")
				.append(decimal(summary.calibrationGap()))
				.append(" |")
				.append('\n');
	}

	private static int sideCount(StrategyAcceptedBetDiagnostics strategy, SelectionType side) {
		for (SideDiagnostics row : strategy.bySide()) {
			if (row.side() == side) {
				return row.summary().candidateCount();
			}
		}
		return 0;
	}

	private static String decimal(BigDecimal value) {
		if (value == null) {
			return "n/a";
		}
		return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	private static String percent(BigDecimal share) {
		if (share == null) {
			return "n/a";
		}
		return share.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
	}

	private static String value(Object object) {
		return object == null ? "n/a" : object.toString();
	}

	private static String date(LocalDate date) {
		return date == null ? "n/a" : date.toString();
	}

	private static String instant(Instant instant) {
		return instant == null ? "n/a" : instant.toString();
	}
}
