package com.safeedge.historical.evaluation;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.probability.ProbabilityModelConfig;

/**
 * Walk-forward evaluation window. Training history may start before the
 * evaluation seasons; only evaluation-season matches generate candidates.
 */
public record WalkForwardEvaluationRequest(
		CanonicalCompetition competition,
		int trainingFromSeason,
		int evaluationFromSeason,
		int evaluationToSeason,
		HistoricalQuoteSource quoteSource,
		ProbabilityModelConfig modelConfig) {

	public WalkForwardEvaluationRequest {
		if (competition == null) {
			throw new HistoricalDataException("competition is required");
		}
		if (quoteSource == null) {
			throw new HistoricalDataException("quoteSource is required");
		}
		if (modelConfig == null) {
			throw new HistoricalDataException("modelConfig is required");
		}
		if (trainingFromSeason <= 0
				|| evaluationFromSeason < trainingFromSeason
				|| evaluationToSeason < evaluationFromSeason) {
			throw new HistoricalDataException(
					"Season range is invalid: trainingFromSeason="
							+ trainingFromSeason
							+ " evaluationFromSeason="
							+ evaluationFromSeason
							+ " evaluationToSeason="
							+ evaluationToSeason);
		}
	}
}
