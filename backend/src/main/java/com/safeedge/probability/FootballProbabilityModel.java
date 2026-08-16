package com.safeedge.probability;

import java.util.List;

/**
 * Estimates a full-time score probability distribution from point-in-time
 * historical matches. Implementations must not use the target result, future
 * matches, or bookmaker odds.
 */
public interface FootballProbabilityModel {

	ProbabilityPrediction predict(List<ProbabilityTrainingMatch> trainingData, MatchPredictionContext target);
}
