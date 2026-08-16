package com.safeedge.bankroll;

import com.safeedge.settlement.PayoutCalculator;
import com.safeedge.settlement.PayoutResult;
import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.StakingMode;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

final class BankrollFixtures {

	static final OwnerId OWNER = new OwnerId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
	static final Instant OCCURRED_AT = Instant.parse("2026-08-16T12:00:00Z");
	static final String REFERENCE = "bet-1";
	static final BigDecimal STAKE = new BigDecimal("1000");
	static final BigDecimal ODDS = new BigDecimal("1.25");

	private static final PayoutCalculator PAYOUTS = new PayoutCalculator();

	private BankrollFixtures() {
	}

	static BigDecimal money(String value) {
		return new BigDecimal(value);
	}

	static BankrollState initialBankroll() {
		return BankrollState.initial(OWNER, money("100000"));
	}

	static StrategyConfig vaultOn(String sweepRate) {
		return vaultConfig(true, sweepRate);
	}

	static StrategyConfig vaultOff() {
		return vaultConfig(false, "0");
	}

	static PayoutResult payout(SettlementResult settlementResult) {
		return PAYOUTS.calculate(settlementResult, ODDS, STAKE);
	}

	static PayoutResult signedProfit(String profitValue) {
		BigDecimal profit = money(profitValue);
		if (profit.compareTo(BigDecimal.ZERO) == 0) {
			return new PayoutResult(STAKE, STAKE, profit);
		}
		if (profit.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal stake = profit;
			return new PayoutResult(stake, stake.add(profit), profit);
		}
		BigDecimal stake = profit.negate();
		return new PayoutResult(stake, BigDecimal.ZERO, profit);
	}

	static SettlementResult settlementFor(String profitValue) {
		int sign = money(profitValue).compareTo(BigDecimal.ZERO);
		if (sign > 0) {
			return SettlementResult.WIN;
		}
		if (sign < 0) {
			return SettlementResult.LOSS;
		}
		return SettlementResult.PUSH;
	}

	private static StrategyConfig vaultConfig(boolean vaultEnabled, String sweepRate) {
		return new StrategyConfig(
				vaultEnabled,
				money(sweepRate),
				StakingMode.FRACTIONAL_KELLY,
				money("0.25"),
				null,
				money("0.02"),
				money("0.03"),
				money("0.03"),
				money("0.05"),
				money("0.10"),
				money("0.10"),
				money("0.15"),
				money("0.50"),
				money("0.20"));
	}

}
