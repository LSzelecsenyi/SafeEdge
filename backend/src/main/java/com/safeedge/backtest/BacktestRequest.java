package com.safeedge.backtest;

import com.safeedge.bankroll.OwnerId;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One deterministic backtest run. Vault starts at 0. {@code maxAcceptedBets}
 * is a chronological accept cap (first N StrategyEngine accepts), not a
 * hindsight selection of the best N outcomes.
 */
public record BacktestRequest(
		OwnerId ownerId,
		BigDecimal startingBankroll,
		StrategyConfig strategyConfig,
		List<HistoricalBettingOpportunity> opportunities,
		List<HistoricalEventResult> eventResults,
		Integer maxAcceptedBets) {

	public BacktestRequest {
		if (ownerId == null) {
			throw new BacktestException("ownerId is required");
		}
		if (startingBankroll == null || startingBankroll.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BacktestException("startingBankroll must be greater than 0");
		}
		if (strategyConfig == null) {
			throw new BacktestException("strategyConfig is required");
		}
		if (opportunities == null) {
			throw new BacktestException("opportunities are required");
		}
		if (eventResults == null) {
			throw new BacktestException("eventResults are required");
		}
		if (maxAcceptedBets != null && maxAcceptedBets <= 0) {
			throw new BacktestException("maxAcceptedBets must be greater than 0 when provided");
		}
		for (HistoricalBettingOpportunity opportunity : opportunities) {
			if (opportunity == null) {
				throw new BacktestException("opportunities must not contain null elements");
			}
		}
		for (HistoricalEventResult result : eventResults) {
			if (result == null) {
				throw new BacktestException("eventResults must not contain null elements");
			}
		}
		startingBankroll = startingBankroll.stripTrailingZeros();
		opportunities = List.copyOf(opportunities);
		eventResults = List.copyOf(eventResults);
		validateDataset(opportunities, eventResults);
	}

	private static void validateDataset(
			List<HistoricalBettingOpportunity> opportunities,
			List<HistoricalEventResult> eventResults) {
		Map<String, HistoricalEventResult> resultsByEventId = new HashMap<>();
		for (HistoricalEventResult result : eventResults) {
			if (resultsByEventId.put(result.eventId(), result) != null) {
				throw new BacktestException("Duplicate historical event result for eventId " + result.eventId());
			}
		}
		Set<String> opportunityIds = new HashSet<>();
		Instant previousDecisionAt = null;
		for (HistoricalBettingOpportunity historical : opportunities) {
			String opportunityId = historical.opportunity().opportunityId();
			if (!opportunityIds.add(opportunityId)) {
				throw new BacktestException("Duplicate opportunityId " + opportunityId);
			}
			if (previousDecisionAt != null && historical.decisionAt().isBefore(previousDecisionAt)) {
				throw new BacktestException(
						"Opportunities must have non-decreasing decisionAt; the engine does not reorder datasets");
			}
			previousDecisionAt = historical.decisionAt();
			HistoricalEventResult result = resultsByEventId.get(historical.opportunity().eventId());
			if (result == null) {
				throw new BacktestException(
						"Historical opportunity " + opportunityId + " has no event result for eventId "
								+ historical.opportunity().eventId());
			}
			if (!historical.decisionAt().isBefore(result.settlementAt())) {
				throw new BacktestException(
						"decisionAt must be before settlementAt for opportunity " + opportunityId
								+ " (look-ahead guardrail)");
			}
		}
	}

}
