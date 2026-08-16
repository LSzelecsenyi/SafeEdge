package com.safeedge.historical.diagnostics;

import com.safeedge.event.domain.SelectionType;
import com.safeedge.settlement.SettlementResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

record AnalyzedCandidate(
		String opportunityId,
		String eventId,
		String seasonDisplay,
		LocalDate matchDate,
		Instant decisionAt,
		SelectionType side,
		BigDecimal selectedLine,
		BigDecimal odds,
		BigDecimal predictedEdge,
		SettlementResult settlement,
		BigDecimal realizedReturnRate) {
}
