package com.safeedge.historical.evaluation;

import com.safeedge.backtest.BacktestCounts;
import com.safeedge.backtest.BacktestMetrics;
import com.safeedge.backtest.BacktestResult;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concise manual-run text. Raw metrics only — no profitability claims.
 */
public final class HistoricalBacktestReportFormatter {

	private HistoricalBacktestReportFormatter() {
	}

	public static String format(HistoricalStrategyComparisonResult comparison) {
		if (comparison == null) {
			throw new IllegalArgumentException("comparison is required");
		}
		StringBuilder text = new StringBuilder();
		appendDataset(text, comparison.dataset().stats());
		text.append('\n').append("Strategy comparison").append('\n');
		for (NamedBacktestResult named : comparison.strategyResults()) {
			appendStrategy(text, named);
		}
		return text.toString();
	}

	private static void appendDataset(StringBuilder text, WalkForwardBuildStats stats) {
		HistoricalQuoteSource quoteSource = stats.quoteSource();
		text.append("Historical walk-forward evaluation").append('\n');
		text.append("Competition: ").append(stats.competition()).append('\n');
		text.append("Training from season: ").append(stats.trainingFromSeason()).append('\n');
		text.append("Evaluation range: ")
				.append(stats.evaluationFromSeason())
				.append(" → ")
				.append(stats.evaluationToSeason())
				.append('\n');
		if (stats.trainingFromSeason() < stats.evaluationFromSeason()) {
			text.append("Warmup seasons (train only, no bets): ")
					.append(stats.trainingFromSeason())
					.append(" → ")
					.append(stats.evaluationFromSeason() - 1)
					.append('\n');
		}
		text.append("HISTORICAL QUOTE SOURCE = ").append(quoteSource).append('\n');
		text.append("These prices are football-data.co.uk historical quotes, not Tippmix odds.").append('\n');
		text.append("Matches loaded: ").append(stats.matchesLoaded()).append('\n');
		text.append("Matches evaluated: ").append(stats.matchesEvaluated()).append('\n');
		text.append("Predictions available: ").append(stats.predictionsAvailable()).append('\n');
		text.append("Predictions with selected AH quote: ")
				.append(stats.predictionsWithSelectedAhQuote())
				.append('\n');
		text.append("Skipped no-league-history: ").append(stats.matchesSkippedNoLeagueHistory()).append('\n');
		text.append("Skipped insufficient-history: ").append(stats.matchesSkippedInsufficientHistory()).append('\n');
		text.append("Skipped fitting-failed: ").append(stats.matchesSkippedFittingFailed()).append('\n');
		text.append("Skipped missing quote: ").append(stats.matchesSkippedMissingQuote()).append('\n');
		text.append("Candidates generated: ").append(stats.candidatesGenerated()).append('\n');
		text.append("  HOME: ").append(stats.homeCandidatesGenerated()).append('\n');
		text.append("  AWAY: ").append(stats.awayCandidatesGenerated()).append('\n');
		text.append("Positive / zero / negative EV: ")
				.append(stats.positiveEvCandidates())
				.append(" / ")
				.append(stats.zeroEvCandidates())
				.append(" / ")
				.append(stats.negativeEvCandidates())
				.append('\n');
		text.append("Average candidate edge: ").append(decimal(stats.averageCandidateEdge())).append('\n');
		text.append("Average actual-score log loss: ")
				.append(decimal(stats.averageActualScoreLogLoss()))
				.append(" (n=")
				.append(stats.logLossObservations())
				.append(", missing-from-grid=")
				.append(stats.logLossMissingFromGrid())
				.append(')')
				.append('\n');
		text.append("Synthetic ordering timestamps: decisionAt = matchDate 00:00 UTC; settlementAt = matchDate+1 00:00 UTC.")
				.append('\n');
		text.append("Those instants are chronological ordering only, not kickoff and not real odds observation times.")
				.append('\n');
	}

	private static void appendStrategy(StringBuilder text, NamedBacktestResult named) {
		BacktestResult result = named.result();
		BacktestCounts counts = result.counts();
		BacktestMetrics metrics = result.metrics();
		text.append('\n').append(named.name()).append('\n');
		text.append("  Bets: ").append(counts.betsAccepted()).append('\n');
		text.append("  WIN / HALF_WIN / PUSH / HALF_LOSS / LOSS: ")
				.append(counts.wins())
				.append(" / ")
				.append(counts.halfWins())
				.append(" / ")
				.append(counts.pushes())
				.append(" / ")
				.append(counts.halfLosses())
				.append(" / ")
				.append(counts.losses())
				.append('\n');
		text.append("  Total stake: ").append(decimal(metrics.totalStake())).append('\n');
		text.append("  Profit: ").append(decimal(metrics.totalProfit())).append('\n');
		text.append("  ROI: ").append(decimal(metrics.roi())).append('\n');
		text.append("  Final active: ").append(decimal(result.finalActiveBankroll())).append('\n');
		text.append("  Vault: ").append(decimal(result.finalVaultBalance())).append('\n');
		text.append("  Total equity: ").append(decimal(result.finalTotalEquity())).append('\n');
		text.append("  Max active DD: ").append(decimal(metrics.maxActiveDrawdownRate())).append('\n');
		text.append("  Max equity DD: ").append(decimal(metrics.maxTotalEquityDrawdownRate())).append('\n');
		text.append("  Longest losing streak: ").append(metrics.longestLosingStreak()).append('\n');
		text.append("  Paused: ").append(result.pausedByDrawdown()).append('\n');
	}

	private static String decimal(BigDecimal value) {
		if (value == null) {
			return "n/a";
		}
		return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}
}
