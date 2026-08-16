package com.safeedge.settlement;

import java.math.BigDecimal;
import java.util.Objects;

public record PayoutResult(BigDecimal stake, BigDecimal returnAmount, BigDecimal profit) {

	public PayoutResult {
		Objects.requireNonNull(stake, "stake");
		Objects.requireNonNull(returnAmount, "returnAmount");
		Objects.requireNonNull(profit, "profit");
		if (profit.compareTo(returnAmount.subtract(stake)) != 0) {
			throw new PayoutException("profit must equal returnAmount - stake");
		}
	}

}
