package com.safeedge.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Already-evaluated betting opportunity. Edge and probabilities are upstream
 * inputs; this type does not compute them.
 *
 * {@code edge} is the modelled expected net return per unit stake
 * ({@code Σ p_i R_i}). CandidateEngine sets it from the score distribution and
 * observed odds. It may be negative. It is not {@code 1/odds} and not a
 * probability-point difference.
 */
public record BettingOpportunity(
		String opportunityId,
		String eventId,
		String leagueId,
		LocalDate bettingDate,
		BigDecimal odds,
		BigDecimal edge,
		SettlementProbabilityDistribution settlementProbabilities) {

	public BettingOpportunity {
		opportunityId = requireText(opportunityId, "opportunityId");
		eventId = requireText(eventId, "eventId");
		leagueId = requireText(leagueId, "leagueId");
		if (bettingDate == null) {
			throw new StrategyException("bettingDate is required");
		}
		if (odds == null || odds.compareTo(BigDecimal.ONE) <= 0) {
			throw new StrategyException("odds must be greater than 1");
		}
		if (edge == null) {
			throw new StrategyException("edge is required");
		}
		if (settlementProbabilities == null) {
			throw new StrategyException("settlementProbabilities is required");
		}
		odds = odds.stripTrailingZeros();
		edge = edge.stripTrailingZeros();
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new StrategyException(name + " is required");
		}
		return value;
	}

}
