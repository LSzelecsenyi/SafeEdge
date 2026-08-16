package com.safeedge.candidate;

import com.safeedge.strategy.BettingOpportunity;
import com.safeedge.strategy.SettlementProbabilityDistribution;
import java.math.BigDecimal;

public record CandidateEvaluation(
		BettingOpportunity opportunity,
		SettlementProbabilityDistribution settlementProbabilityDistribution,
		BigDecimal impliedProbabilityReference,
		BigDecimal expectedReturnRate,
		CandidateValueStatus status) {

	public CandidateEvaluation {
		if (opportunity == null) {
			throw new CandidateException("opportunity is required");
		}
		if (settlementProbabilityDistribution == null) {
			throw new CandidateException("settlementProbabilityDistribution is required");
		}
		if (!settlementProbabilityDistribution.equals(opportunity.settlementProbabilities())) {
			throw new CandidateException("settlementProbabilityDistribution must match the opportunity");
		}
		if (impliedProbabilityReference == null) {
			throw new CandidateException("impliedProbabilityReference is required");
		}
		if (expectedReturnRate == null) {
			throw new CandidateException("expectedReturnRate is required");
		}
		if (opportunity.edge().compareTo(expectedReturnRate) != 0) {
			throw new CandidateException("opportunity edge must equal expectedReturnRate");
		}
		if (status == null) {
			throw new CandidateException("status is required");
		}
		if (status != statusOf(expectedReturnRate)) {
			throw new CandidateException("status must match the sign of expectedReturnRate");
		}
		impliedProbabilityReference = impliedProbabilityReference.stripTrailingZeros();
		expectedReturnRate = expectedReturnRate.stripTrailingZeros();
	}

	static CandidateValueStatus statusOf(BigDecimal expectedReturnRate) {
		int sign = expectedReturnRate.compareTo(BigDecimal.ZERO);
		if (sign > 0) {
			return CandidateValueStatus.POSITIVE_EV;
		}
		if (sign < 0) {
			return CandidateValueStatus.NEGATIVE_EV;
		}
		return CandidateValueStatus.ZERO_EV;
	}

}
