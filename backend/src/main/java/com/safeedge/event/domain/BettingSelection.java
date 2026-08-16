package com.safeedge.event.domain;

import java.math.BigDecimal;

public record BettingSelection(
		String provider,
		Integer externalOutcomeNo,
		Integer externalOutcomeRealNo,
		String providerOutcomeName,
		SelectionType selectionType,
		BigDecimal line,
		BigDecimal odds) {
}
