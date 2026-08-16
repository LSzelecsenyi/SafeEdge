package com.safeedge.strategy;

import java.math.BigDecimal;
import java.util.List;

public record StrategyDecision(
		StrategyDecisionStatus status,
		BigDecimal stake,
		BigDecimal fullKellyFraction,
		BigDecimal appliedStakeRate,
		BigDecimal expectedReturnRate,
		List<StrategyDecisionReason> reasons) {

	public StrategyDecision {
		if (status == null) {
			throw new StrategyException("status is required");
		}
		if (stake == null || stake.compareTo(BigDecimal.ZERO) < 0) {
			throw new StrategyException("stake must be >= 0");
		}
		if (appliedStakeRate == null || appliedStakeRate.compareTo(BigDecimal.ZERO) < 0) {
			throw new StrategyException("appliedStakeRate must be >= 0");
		}
		if (expectedReturnRate == null) {
			throw new StrategyException("expectedReturnRate is required");
		}
		if (reasons == null) {
			throw new StrategyException("reasons are required");
		}
		stake = stake.stripTrailingZeros();
		appliedStakeRate = appliedStakeRate.stripTrailingZeros();
		expectedReturnRate = expectedReturnRate.stripTrailingZeros();
		if (fullKellyFraction != null) {
			fullKellyFraction = fullKellyFraction.stripTrailingZeros();
		}
		reasons = List.copyOf(reasons);
	}

	public boolean accepted() {
		return status == StrategyDecisionStatus.ACCEPTED;
	}

}
