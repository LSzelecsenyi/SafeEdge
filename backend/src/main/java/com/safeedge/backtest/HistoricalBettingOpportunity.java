package com.safeedge.backtest;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.strategy.BettingOpportunity;
import java.time.Instant;

/**
 * Point-in-time historical opportunity. Settlement scores and payouts are not
 * stored here; {@link BacktestEngine} obtains them from
 * {@link HistoricalEventResult} only at {@code settlementAt}.
 *
 * {@link BettingOpportunity#odds()} is the decision-time price used for payout.
 * {@link BettingSelection#odds()} is the same observed decimal price on the
 * normalized selection, so the two must match.
 */
public record HistoricalBettingOpportunity(
		BettingOpportunity opportunity,
		BettingMarket market,
		BettingSelection selection,
		Instant decisionAt) {

	public HistoricalBettingOpportunity {
		if (opportunity == null) {
			throw new BacktestException("opportunity is required");
		}
		if (market == null) {
			throw new BacktestException("market is required");
		}
		if (selection == null) {
			throw new BacktestException("selection is required");
		}
		if (decisionAt == null) {
			throw new BacktestException("decisionAt is required");
		}
		if (market.selections() == null || !market.selections().contains(selection)) {
			throw new BacktestException("selection must belong to the historical market");
		}
		if (selection.odds() == null) {
			throw new BacktestException("selection odds are required");
		}
		if (opportunity.odds().compareTo(selection.odds()) != 0) {
			throw new BacktestException("opportunity odds must match selection observation odds");
		}
	}

}
