package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record TwoSidedCoherence(
		int twoSidedEvents,
		int bothSidesPositiveEdge,
		int exactlyOneSidePositiveEdge,
		int neitherSidePositiveEdge,
		BigDecimal averageHomePlusAwayEdge,
		BigDecimal averageOverround,
		BigDecimal medianOverround) {

	public TwoSidedCoherence {
		if (twoSidedEvents < 0
				|| bothSidesPositiveEdge < 0
				|| exactlyOneSidePositiveEdge < 0
				|| neitherSidePositiveEdge < 0) {
			throw new IllegalArgumentException("counts must be >= 0");
		}
		averageHomePlusAwayEdge = averageHomePlusAwayEdge == null ? null : averageHomePlusAwayEdge.stripTrailingZeros();
		averageOverround = averageOverround == null ? null : averageOverround.stripTrailingZeros();
		medianOverround = medianOverround == null ? null : medianOverround.stripTrailingZeros();
	}
}
