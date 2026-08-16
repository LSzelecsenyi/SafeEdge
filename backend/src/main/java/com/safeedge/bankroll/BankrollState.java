package com.safeedge.bankroll;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Immutable owner-specific bankroll equity after settled outcomes.
 *
 * {@code activeBankroll} is the stake base. {@code vaultBalance} is protected
 * capital and is not part of the stake base. {@code totalEquity} is their sum.
 *
 * This object does not model open bets, reserved cash, or exposure.
 */
public record BankrollState(
		OwnerId ownerId,
		BigDecimal activeBankroll,
		BigDecimal vaultBalance,
		BigDecimal cumulativeRealizedPnl,
		BigDecimal profitHighWaterMark,
		BigDecimal activeDrawdownHighWaterMark,
		BigDecimal totalEquityHighWaterMark) {

	private static final MathContext RATE_CONTEXT = MathContext.DECIMAL128;

	public BankrollState {
		if (ownerId == null) {
			throw new BankrollException("Owner id is required");
		}
		activeBankroll = requireMoney(activeBankroll, "activeBankroll");
		vaultBalance = requireMoney(vaultBalance, "vaultBalance");
		cumulativeRealizedPnl = requireNonNull(cumulativeRealizedPnl, "cumulativeRealizedPnl");
		profitHighWaterMark = requireNonNull(profitHighWaterMark, "profitHighWaterMark");
		activeDrawdownHighWaterMark = requireMoney(activeDrawdownHighWaterMark, "activeDrawdownHighWaterMark");
		totalEquityHighWaterMark = requireMoney(totalEquityHighWaterMark, "totalEquityHighWaterMark");
		if (activeBankroll.compareTo(BigDecimal.ZERO) < 0) {
			throw new BankrollException("activeBankroll cannot be negative");
		}
		if (vaultBalance.compareTo(BigDecimal.ZERO) < 0) {
			throw new BankrollException("vaultBalance cannot be negative");
		}
		if (profitHighWaterMark.compareTo(cumulativeRealizedPnl) < 0) {
			throw new BankrollException("profitHighWaterMark cannot be below cumulativeRealizedPnl");
		}
		if (activeDrawdownHighWaterMark.compareTo(activeBankroll) < 0) {
			throw new BankrollException("activeDrawdownHighWaterMark cannot be below activeBankroll");
		}
		BigDecimal equity = activeBankroll.add(vaultBalance);
		if (totalEquityHighWaterMark.compareTo(equity) < 0) {
			throw new BankrollException("totalEquityHighWaterMark cannot be below totalEquity");
		}
	}

	public static BankrollState initial(OwnerId ownerId, BigDecimal startingBankroll) {
		BigDecimal starting = requireNonNull(startingBankroll, "startingBankroll");
		if (starting.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BankrollException("startingBankroll must be greater than 0");
		}
		BigDecimal normalized = starting.stripTrailingZeros();
		return new BankrollState(
				ownerId,
				normalized,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				normalized,
				normalized);
	}

	public BigDecimal totalEquity() {
		return activeBankroll.add(vaultBalance);
	}

	public BigDecimal activeDrawdownAmount() {
		return activeDrawdownHighWaterMark.subtract(activeBankroll).max(BigDecimal.ZERO);
	}

	public BigDecimal activeDrawdownRate() {
		return rate(activeDrawdownAmount(), activeDrawdownHighWaterMark, "activeDrawdownHighWaterMark");
	}

	public BigDecimal totalEquityDrawdownAmount() {
		return totalEquityHighWaterMark.subtract(totalEquity()).max(BigDecimal.ZERO);
	}

	public BigDecimal totalEquityDrawdownRate() {
		return rate(totalEquityDrawdownAmount(), totalEquityHighWaterMark, "totalEquityHighWaterMark");
	}

	private static BigDecimal rate(BigDecimal amount, BigDecimal highWaterMark, String name) {
		if (amount.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		if (highWaterMark.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BankrollException("Cannot compute drawdown rate against a non-positive " + name);
		}
		return amount.divide(highWaterMark, RATE_CONTEXT);
	}

	private static BigDecimal requireMoney(BigDecimal value, String name) {
		return requireNonNull(value, name).stripTrailingZeros();
	}

	private static BigDecimal requireNonNull(BigDecimal value, String name) {
		if (value == null) {
			throw new BankrollException(name + " is required");
		}
		return value.stripTrailingZeros();
	}

}
