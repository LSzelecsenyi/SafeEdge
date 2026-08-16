package com.safeedge.probability;

import com.safeedge.candidate.ScoreProbabilityDistribution;
import java.math.BigDecimal;

/**
 * Point-in-time score prediction. Unavailable statuses have null lambdas and
 * null distribution — never a fake zero grid.
 */
public record ProbabilityPrediction(
		ProbabilityPredictionStatus status,
		BigDecimal homeExpectedGoals,
		BigDecimal awayExpectedGoals,
		ScoreProbabilityDistribution scoreDistribution,
		int trainingMatchCount,
		int homeHistoryCount,
		int awayHistoryCount,
		BigDecimal capturedProbabilityMassBeforeNormalization) {

	public ProbabilityPrediction {
		if (status == null) {
			throw new ProbabilityModelException("status is required");
		}
		if (trainingMatchCount < 0 || homeHistoryCount < 0 || awayHistoryCount < 0) {
			throw new ProbabilityModelException("history counts must be >= 0");
		}
		if (status == ProbabilityPredictionStatus.AVAILABLE) {
			if (homeExpectedGoals == null || awayExpectedGoals == null) {
				throw new ProbabilityModelException("expected goals are required when available");
			}
			if (scoreDistribution == null) {
				throw new ProbabilityModelException("scoreDistribution is required when available");
			}
			if (capturedProbabilityMassBeforeNormalization == null) {
				throw new ProbabilityModelException("captured mass is required when available");
			}
		}
		else {
			if (homeExpectedGoals != null
					|| awayExpectedGoals != null
					|| scoreDistribution != null
					|| capturedProbabilityMassBeforeNormalization != null) {
				throw new ProbabilityModelException("unavailable predictions must not include a distribution");
			}
		}
	}

	public boolean available() {
		return status == ProbabilityPredictionStatus.AVAILABLE;
	}

	static ProbabilityPrediction available(
			BigDecimal homeExpectedGoals,
			BigDecimal awayExpectedGoals,
			ScoreProbabilityDistribution scoreDistribution,
			int trainingMatchCount,
			int homeHistoryCount,
			int awayHistoryCount,
			BigDecimal capturedMass) {
		return new ProbabilityPrediction(
				ProbabilityPredictionStatus.AVAILABLE,
				homeExpectedGoals,
				awayExpectedGoals,
				scoreDistribution,
				trainingMatchCount,
				homeHistoryCount,
				awayHistoryCount,
				capturedMass);
	}

	public static ProbabilityPrediction unavailable(
			ProbabilityPredictionStatus status, int trainingMatchCount, int homeHistoryCount, int awayHistoryCount) {
		if (status == ProbabilityPredictionStatus.AVAILABLE) {
			throw new ProbabilityModelException("AVAILABLE is not an unavailable status");
		}
		return new ProbabilityPrediction(
				status, null, null, null, trainingMatchCount, homeHistoryCount, awayHistoryCount, null);
	}
}
