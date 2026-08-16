package com.safeedge.historical.diagnostics;

public record ConsistencyChecks(
		boolean exhaustiveGroupCounts,
		boolean weightedRealizedMatchesGlobal,
		boolean weightedEdgeMatchesGlobal,
		boolean settlementProbabilitiesSumToOne,
		boolean actualSettlementIsExclusive,
		boolean expectedReturnMatchesCandidateEngine,
		boolean unitReturnMatchesPayoutCalculator,
		boolean inputNotMutated) {

	public boolean allPassed() {
		return exhaustiveGroupCounts
				&& weightedRealizedMatchesGlobal
				&& weightedEdgeMatchesGlobal
				&& settlementProbabilitiesSumToOne
				&& actualSettlementIsExclusive
				&& expectedReturnMatchesCandidateEngine
				&& unitReturnMatchesPayoutCalculator
				&& inputNotMutated;
	}
}
