package com.safeedge.historical.diagnostics;

import com.safeedge.event.domain.SelectionType;
import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.SettlementProbabilityDistribution;
import java.math.BigDecimal;
import java.time.LocalDate;

record EdgeQualityCandidate(
		String opportunityId,
		String eventId,
		String seasonDisplay,
		LocalDate matchDate,
		String homeTeam,
		String awayTeam,
		SelectionType side,
		BigDecimal selectedLine,
		BigDecimal marketHomeLine,
		BigDecimal odds,
		BigDecimal oppositeOdds,
		BigDecimal predictedEdge,
		BigDecimal recomputedExpectedReturn,
		SettlementProbabilityDistribution settlementProbabilities,
		SettlementResult settlement,
		BigDecimal realizedReturnRate) {
}
