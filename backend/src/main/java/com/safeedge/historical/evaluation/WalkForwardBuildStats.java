package com.safeedge.historical.evaluation;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import java.math.BigDecimal;

public record WalkForwardBuildStats(
		CanonicalCompetition competition,
		int trainingFromSeason,
		int evaluationFromSeason,
		int evaluationToSeason,
		HistoricalQuoteSource quoteSource,
		int matchesLoaded,
		int matchesEvaluated,
		int matchesSkippedNoLeagueHistory,
		int matchesSkippedInsufficientHistory,
		int matchesSkippedFittingFailed,
		int matchesSkippedMissingQuote,
		int predictionsAvailable,
		int predictionsWithSelectedAhQuote,
		int candidatesGenerated,
		int homeCandidatesGenerated,
		int awayCandidatesGenerated,
		int positiveEvCandidates,
		int zeroEvCandidates,
		int negativeEvCandidates,
		int logLossObservations,
		int logLossMissingFromGrid,
		BigDecimal averageActualScoreLogLoss,
		BigDecimal averageCandidateEdge) {
}
