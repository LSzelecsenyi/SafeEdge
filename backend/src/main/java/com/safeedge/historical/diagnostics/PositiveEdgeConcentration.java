package com.safeedge.historical.diagnostics;

import java.util.List;

public record PositiveEdgeConcentration(
		List<ConcentrationShare> bySide,
		List<ConcentrationShare> byAhLine,
		List<ConcentrationShare> byOddsBucket,
		List<ConcentrationShare> bySeason) {

	public PositiveEdgeConcentration {
		bySide = List.copyOf(bySide == null ? List.of() : bySide);
		byAhLine = List.copyOf(byAhLine == null ? List.of() : byAhLine);
		byOddsBucket = List.copyOf(byOddsBucket == null ? List.of() : byOddsBucket);
		bySeason = List.copyOf(bySeason == null ? List.of() : bySeason);
	}
}
