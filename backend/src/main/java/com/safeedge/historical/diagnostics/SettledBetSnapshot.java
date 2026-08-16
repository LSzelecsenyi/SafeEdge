package com.safeedge.historical.diagnostics;

import com.safeedge.event.domain.SelectionType;
import com.safeedge.settlement.SettlementResult;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SettledBetSnapshot(
		LocalDate bettingDate,
		SelectionType side,
		BigDecimal selectedLine,
		BigDecimal odds,
		BigDecimal edge,
		SettlementResult settlement,
		BigDecimal profit) {

	public SettledBetSnapshot {
		if (bettingDate == null) {
			throw new IllegalArgumentException("bettingDate is required");
		}
		if (side == null) {
			throw new IllegalArgumentException("side is required");
		}
		if (selectedLine == null) {
			throw new IllegalArgumentException("selectedLine is required");
		}
		if (odds == null) {
			throw new IllegalArgumentException("odds are required");
		}
		if (edge == null) {
			throw new IllegalArgumentException("edge is required");
		}
		if (settlement == null) {
			throw new IllegalArgumentException("settlement is required");
		}
		if (profit == null) {
			throw new IllegalArgumentException("profit is required");
		}
		selectedLine = selectedLine.stripTrailingZeros();
		odds = odds.stripTrailingZeros();
		edge = edge.stripTrailingZeros();
		profit = profit.stripTrailingZeros();
	}
}
