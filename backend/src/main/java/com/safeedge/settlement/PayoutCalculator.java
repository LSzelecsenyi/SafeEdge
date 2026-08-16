package com.safeedge.settlement;

import java.math.BigDecimal;

public final class PayoutCalculator {

	private static final BigDecimal TWO = new BigDecimal("2");

	public PayoutResult calculate(SettlementResult settlementResult, BigDecimal odds, BigDecimal stake) {
		if (settlementResult == null) {
			throw new PayoutException("Settlement result is required");
		}
		if (stake == null || stake.compareTo(BigDecimal.ZERO) <= 0) {
			throw new PayoutException("Stake must be greater than 0");
		}
		if (odds == null || odds.compareTo(BigDecimal.ONE) <= 0) {
			throw new PayoutException("Odds must be greater than 1");
		}
		BigDecimal returnAmount = switch (settlementResult) {
			case WIN -> stake.multiply(odds);
			case HALF_WIN -> {
				BigDecimal halfStake = stake.divide(TWO);
				yield halfStake.multiply(odds).add(halfStake);
			}
			case PUSH -> stake;
			case HALF_LOSS -> stake.divide(TWO);
			case LOSS -> BigDecimal.ZERO;
		};
		return new PayoutResult(stake, returnAmount, returnAmount.subtract(stake));
	}

}
