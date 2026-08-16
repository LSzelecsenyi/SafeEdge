package com.safeedge.candidate;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.settlement.SettlementEngine;
import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.BettingOpportunity;
import com.safeedge.strategy.GeneralizedKellyCalculator;
import com.safeedge.strategy.SettlementProbabilityDistribution;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.EnumMap;
import java.util.Map;

/**
 * Maps a point-in-time score distribution through {@link SettlementEngine} and
 * prices the offered selection. It evaluates value only; it does not apply
 * {@code StrategyConfig} gates, Kelly, exposure, or drawdown.
 *
 * {@code impliedProbabilityReference} is {@code 1 / odds}. That is a simple
 * decimal-odds reference, not a complete Asian Handicap break-even model.
 */
public final class CandidateEngine {

	private static final MathContext MATH = MathContext.DECIMAL128;

	private final SettlementEngine settlementEngine;
	private final GeneralizedKellyCalculator expectedReturnCalculator;

	public CandidateEngine() {
		this(new SettlementEngine(), new GeneralizedKellyCalculator());
	}

	public CandidateEngine(SettlementEngine settlementEngine, GeneralizedKellyCalculator expectedReturnCalculator) {
		if (settlementEngine == null) {
			throw new CandidateException("settlementEngine is required");
		}
		if (expectedReturnCalculator == null) {
			throw new CandidateException("expectedReturnCalculator is required");
		}
		this.settlementEngine = settlementEngine;
		this.expectedReturnCalculator = expectedReturnCalculator;
	}

	public CandidateEvaluation evaluate(
			BettingMarket market,
			BettingSelection selection,
			BigDecimal observedOdds,
			ScoreProbabilityDistribution scoreDistribution,
			CandidateContext context) {
		if (market == null) {
			throw new CandidateException("market is required");
		}
		if (selection == null) {
			throw new CandidateException("selection is required");
		}
		if (market.selections() == null || !market.selections().contains(selection)) {
			throw new CandidateException("selection must belong to the market");
		}
		if (observedOdds == null || observedOdds.compareTo(BigDecimal.ONE) <= 0) {
			throw new CandidateException("observedOdds must be greater than 1");
		}
		if (scoreDistribution == null) {
			throw new CandidateException("scoreDistribution is required");
		}
		if (context == null) {
			throw new CandidateException("context is required");
		}
		SettlementProbabilityDistribution settlementProbabilities =
				deriveSettlementProbabilities(market, selection, scoreDistribution);
		BigDecimal expectedReturnRate = expectedReturnCalculator.expectedReturnRate(
				observedOdds, settlementProbabilities);
		BigDecimal impliedProbabilityReference = BigDecimal.ONE.divide(observedOdds, MATH);
		BettingOpportunity opportunity = new BettingOpportunity(
				context.opportunityId(),
				context.eventId(),
				context.leagueId(),
				context.bettingDate(),
				observedOdds,
				expectedReturnRate,
				settlementProbabilities);
		return new CandidateEvaluation(
				opportunity,
				settlementProbabilities,
				impliedProbabilityReference,
				expectedReturnRate,
				CandidateEvaluation.statusOf(expectedReturnRate));
	}

	private SettlementProbabilityDistribution deriveSettlementProbabilities(
			BettingMarket market,
			BettingSelection selection,
			ScoreProbabilityDistribution scoreDistribution) {
		Map<SettlementResult, BigDecimal> buckets = new EnumMap<>(SettlementResult.class);
		for (SettlementResult result : SettlementResult.values()) {
			buckets.put(result, BigDecimal.ZERO);
		}
		for (ScoreProbability entry : scoreDistribution.entries()) {
			SettlementResult result = settlementEngine.settle(market, selection, entry.score());
			buckets.merge(result, entry.probability(), BigDecimal::add);
		}
		return new SettlementProbabilityDistribution(
				buckets.get(SettlementResult.WIN),
				buckets.get(SettlementResult.HALF_WIN),
				buckets.get(SettlementResult.PUSH),
				buckets.get(SettlementResult.HALF_LOSS),
				buckets.get(SettlementResult.LOSS));
	}

}
