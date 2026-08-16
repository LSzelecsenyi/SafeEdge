package com.safeedge.bankroll;

import static com.safeedge.bankroll.BankrollFixtures.OWNER;
import static com.safeedge.bankroll.BankrollFixtures.initialBankroll;
import static com.safeedge.bankroll.BankrollFixtures.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.MathContext;
import org.junit.jupiter.api.Test;

class BankrollStateTest {

	@Test
	void initialStateUsesStartingBankrollOnlyInActive() {
		BankrollState state = initialBankroll();
		assertThat(state.ownerId()).isEqualTo(OWNER);
		assertThat(state.activeBankroll()).isEqualByComparingTo("100000");
		assertThat(state.vaultBalance()).isEqualByComparingTo("0");
		assertThat(state.totalEquity()).isEqualByComparingTo("100000");
		assertThat(state.cumulativeRealizedPnl()).isEqualByComparingTo("0");
		assertThat(state.profitHighWaterMark()).isEqualByComparingTo("0");
		assertThat(state.activeDrawdownHighWaterMark()).isEqualByComparingTo("100000");
		assertThat(state.totalEquityHighWaterMark()).isEqualByComparingTo("100000");
		assertThat(state.activeDrawdownAmount()).isEqualByComparingTo("0");
		assertThat(state.activeDrawdownRate()).isEqualByComparingTo("0");
		assertThat(state.totalEquityDrawdownAmount()).isEqualByComparingTo("0");
		assertThat(state.totalEquityDrawdownRate()).isEqualByComparingTo("0");
	}

	@Test
	void totalEquityIsAlwaysActivePlusVault() {
		BankrollState state = new BankrollState(
				OWNER,
				money("107000"),
				money("3000"),
				money("10000"),
				money("10000"),
				money("107000"),
				money("110000"));
		assertThat(state.totalEquity()).isEqualByComparingTo(state.activeBankroll().add(state.vaultBalance()));
		assertThat(state.totalEquity()).isEqualByComparingTo("110000");
	}

	@Test
	void drawdownUsesHighPrecisionDivision() {
		BankrollState state = new BankrollState(
				OWNER,
				money("97000"),
				money("3000"),
				money("0"),
				money("10000"),
				money("107000"),
				money("110000"));
		assertThat(state.activeDrawdownAmount()).isEqualByComparingTo("10000");
		assertThat(state.activeDrawdownRate())
				.isEqualByComparingTo(new BigDecimal("10000").divide(new BigDecimal("107000"), MathContext.DECIMAL128));
		assertThat(state.totalEquityDrawdownAmount()).isEqualByComparingTo("10000");
		assertThat(state.totalEquityDrawdownRate())
				.isEqualByComparingTo(new BigDecimal("10000").divide(new BigDecimal("110000"), MathContext.DECIMAL128));
	}

	@Test
	void startingBankrollMustBePositive() {
		assertThatThrownBy(() -> BankrollState.initial(OWNER, BigDecimal.ZERO)).isInstanceOf(BankrollException.class);
		assertThatThrownBy(() -> BankrollState.initial(OWNER, money("-1"))).isInstanceOf(BankrollException.class);
		assertThatThrownBy(() -> BankrollState.initial(OWNER, null)).isInstanceOf(BankrollException.class);
		assertThatThrownBy(() -> BankrollState.initial(null, money("100000"))).isInstanceOf(BankrollException.class);
	}

	@Test
	void negativeBalancesAreRejected() {
		assertThatThrownBy(() -> new BankrollState(
						OWNER,
						money("-1"),
						BigDecimal.ZERO,
						BigDecimal.ZERO,
						BigDecimal.ZERO,
						money("100000"),
						money("100000")))
				.isInstanceOf(BankrollException.class);
	}

}
