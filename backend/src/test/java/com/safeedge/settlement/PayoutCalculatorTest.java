package com.safeedge.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class PayoutCalculatorTest {

	private static final BigDecimal STAKE = new BigDecimal("1000");
	private static final BigDecimal ODDS = new BigDecimal("1.25");
	private static final BigDecimal TWO = new BigDecimal("2");

	private final PayoutCalculator calculator = new PayoutCalculator();

	@Test
	void winAtOneTwentyFive() {
		assertPayout(SettlementResult.WIN, STAKE, ODDS, "1250", "250");
	}

	@Test
	void halfWinAtOneTwentyFive() {
		assertPayout(SettlementResult.HALF_WIN, STAKE, ODDS, "1125", "125");
	}

	@Test
	void pushAtOneTwentyFive() {
		assertPayout(SettlementResult.PUSH, STAKE, ODDS, "1000", "0");
	}

	@Test
	void halfLossAtOneTwentyFive() {
		assertPayout(SettlementResult.HALF_LOSS, STAKE, ODDS, "500", "-500");
	}

	@Test
	void lossAtOneTwentyFive() {
		assertPayout(SettlementResult.LOSS, STAKE, ODDS, "0", "-1000");
	}

	@Test
	void decimalStakeAndOddsHaveNoIntegerAssumption() {
		BigDecimal stake = new BigDecimal("1234.56");
		BigDecimal odds = new BigDecimal("1.23");
		PayoutResult win = calculator.calculate(SettlementResult.WIN, odds, stake);
		assertThat(win.returnAmount()).isEqualByComparingTo("1518.5088");
		assertThat(win.profit()).isEqualByComparingTo("283.9488");
		PayoutResult halfWin = calculator.calculate(SettlementResult.HALF_WIN, odds, stake);
		assertThat(halfWin.profit()).isEqualByComparingTo(win.profit().divide(TWO));
	}

	@ParameterizedTest
	@CsvSource({
			"1000, 1.25",
			"1234.56, 1.23",
			"10, 2",
			"0.5, 3.5",
			"99.99, 1.01"
	})
	void profitInvariantsHold(String stakeValue, String oddsValue) {
		BigDecimal stake = new BigDecimal(stakeValue);
		BigDecimal odds = new BigDecimal(oddsValue);
		BigDecimal winProfit = calculator.calculate(SettlementResult.WIN, odds, stake).profit();
		assertThat(winProfit).isEqualByComparingTo(stake.multiply(odds.subtract(BigDecimal.ONE)));
		assertThat(calculator.calculate(SettlementResult.HALF_WIN, odds, stake).profit())
				.isEqualByComparingTo(winProfit.divide(TWO));
		assertThat(calculator.calculate(SettlementResult.PUSH, odds, stake).profit())
				.isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(calculator.calculate(SettlementResult.HALF_LOSS, odds, stake).profit())
				.isEqualByComparingTo(stake.divide(TWO).negate());
		assertThat(calculator.calculate(SettlementResult.LOSS, odds, stake).profit())
				.isEqualByComparingTo(stake.negate());
	}

	@ParameterizedTest
	@EnumSource(SettlementResult.class)
	void profitEqualsReturnMinusStake(SettlementResult settlementResult) {
		PayoutResult payout = calculator.calculate(settlementResult, ODDS, STAKE);
		assertThat(payout.profit()).isEqualByComparingTo(payout.returnAmount().subtract(payout.stake()));
		assertThat(payout.stake()).isEqualByComparingTo(STAKE);
	}

	@Test
	void rejectsInvalidInputs() {
		assertThatThrownBy(() -> calculator.calculate(null, ODDS, STAKE))
				.isInstanceOf(PayoutException.class)
				.hasMessageContaining("Settlement result");
		assertThatThrownBy(() -> calculator.calculate(SettlementResult.WIN, null, STAKE))
				.isInstanceOf(PayoutException.class)
				.hasMessageContaining("Odds");
		assertThatThrownBy(() -> calculator.calculate(SettlementResult.LOSS, BigDecimal.ONE, STAKE))
				.isInstanceOf(PayoutException.class)
				.hasMessageContaining("Odds");
		assertThatThrownBy(() -> calculator.calculate(SettlementResult.WIN, ODDS, null))
				.isInstanceOf(PayoutException.class)
				.hasMessageContaining("Stake");
		assertThatThrownBy(() -> calculator.calculate(SettlementResult.WIN, ODDS, BigDecimal.ZERO))
				.isInstanceOf(PayoutException.class)
				.hasMessageContaining("Stake");
	}

	private void assertPayout(
			SettlementResult settlementResult,
			BigDecimal stake,
			BigDecimal odds,
			String expectedReturn,
			String expectedProfit) {
		PayoutResult payout = calculator.calculate(settlementResult, odds, stake);
		assertThat(payout.returnAmount()).isEqualByComparingTo(expectedReturn);
		assertThat(payout.profit()).isEqualByComparingTo(expectedProfit);
	}

}
