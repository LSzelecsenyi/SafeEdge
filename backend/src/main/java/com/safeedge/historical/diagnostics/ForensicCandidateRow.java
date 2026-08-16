package com.safeedge.historical.diagnostics;

import com.safeedge.event.domain.SelectionType;
import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.SettlementProbabilityDistribution;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ForensicCandidateRow(
		LocalDate date,
		String eventId,
		String homeTeam,
		String awayTeam,
		SelectionType side,
		BigDecimal selectedLine,
		BigDecimal odds,
		SettlementProbabilityDistribution predictedSettlement,
		BigDecimal predictedEdge,
		SettlementResult actualSettlement,
		BigDecimal actualUnitReturn) {

	public ForensicCandidateRow {
		if (date == null
				|| eventId == null
				|| side == null
				|| selectedLine == null
				|| odds == null
				|| predictedSettlement == null
				|| predictedEdge == null
				|| actualSettlement == null
				|| actualUnitReturn == null) {
			throw new IllegalArgumentException("forensic row fields are required");
		}
		selectedLine = selectedLine.stripTrailingZeros();
		odds = odds.stripTrailingZeros();
		predictedEdge = predictedEdge.stripTrailingZeros();
		actualUnitReturn = actualUnitReturn.stripTrailingZeros();
	}
}
