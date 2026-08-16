package com.safeedge.bankroll;

import static com.safeedge.bankroll.BankrollFixtures.OCCURRED_AT;
import static com.safeedge.bankroll.BankrollFixtures.OWNER;
import static com.safeedge.bankroll.BankrollFixtures.REFERENCE;
import static com.safeedge.bankroll.BankrollFixtures.initialBankroll;
import static com.safeedge.bankroll.BankrollFixtures.money;
import static com.safeedge.bankroll.BankrollFixtures.payout;
import static com.safeedge.bankroll.BankrollFixtures.settlementFor;
import static com.safeedge.bankroll.BankrollFixtures.signedProfit;
import static com.safeedge.bankroll.BankrollFixtures.vaultOn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BankrollCapitalFlowDrawdownTest {

	private static final String TWENTY_PERCENT = "0.2";

	private final BankrollAccountingEngine engine = new BankrollAccountingEngine();

	@Test
	void activeWithdrawalDuringDrawdownPreservesTwentyPercentRate() {
		BankrollState start = activeDrawdownTwentyPercent();
		assertThat(start.activeDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		AccountingResult result = engine.withdrawFromActive(start, money("20000"), "wd-dd", OCCURRED_AT);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("60000");
		assertThat(result.state().activeDrawdownHighWaterMark()).isEqualByComparingTo("75000");
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(start.activeDrawdownRate());
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		assertUnchangedPerformanceAndVault(start, result.state());
		assertThat(result.state().totalEquity()).isEqualByComparingTo("60000");
	}

	@Test
	void depositDuringDrawdownPreservesTwentyPercentRateAndDoesNotLookLikeRecovery() {
		BankrollState start = activeDrawdownTwentyPercent();
		AccountingResult result = engine.depositToActive(start, money("20000"), "dep-dd", OCCURRED_AT);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("100000");
		assertThat(result.state().activeDrawdownHighWaterMark()).isEqualByComparingTo("125000");
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(start.activeDrawdownRate());
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		assertUnchangedPerformanceAndVault(start, result.state());
		assertThat(result.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.DEPOSIT);
		assertThat(result.transactions().getFirst().realizedPnlDelta()).isEqualByComparingTo("0");
	}

	@Test
	void vaultTransferToActiveDuringDrawdownPreservesTwentyPercentRate() {
		BankrollState start = new BankrollState(
				OWNER,
				money("80000"),
				money("20000"),
				money("-20000"),
				BigDecimal.ZERO,
				money("100000"),
				money("100000"));
		assertThat(start.activeDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		AccountingResult result = engine.transferVaultToActive(start, money("10000"), "xfer-dd", OCCURRED_AT);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("90000");
		assertThat(result.state().vaultBalance()).isEqualByComparingTo("10000");
		assertThat(result.state().totalEquity()).isEqualByComparingTo(start.totalEquity());
		assertThat(result.state().activeDrawdownHighWaterMark()).isEqualByComparingTo("112500");
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(start.activeDrawdownRate());
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		assertThat(result.state().cumulativeRealizedPnl()).isEqualByComparingTo(start.cumulativeRealizedPnl());
		assertThat(result.state().profitHighWaterMark()).isEqualByComparingTo(start.profitHighWaterMark());
		assertThat(result.state().totalEquityHighWaterMark()).isEqualByComparingTo(start.totalEquityHighWaterMark());
		assertThat(result.transactions().getFirst().totalEquityDelta()).isEqualByComparingTo("0");
	}

	@Test
	void vaultSweepDuringExistingDrawdownDoesNotIncreaseActiveDrawdownRate() {
		BankrollState afterLoss = applyProfit(initialBankroll(), "-20000", vaultOn("0.30")).state();
		BankrollState afterDeposit = engine.depositToActive(afterLoss, money("40000"), "dep-inflate", OCCURRED_AT)
				.state();
		assertThat(afterDeposit.activeDrawdownRate()).isEqualByComparingTo(afterLoss.activeDrawdownRate());
		AccountingResult result = applyProfit(afterDeposit, "25000", vaultOn("0.30"));
		BankrollState after = result.state();
		BigDecimal activeBeforeSweep = afterDeposit.activeBankroll().add(money("25000"));
		BigDecimal vaultBeforeSweep = afterDeposit.vaultBalance();
		BigDecimal equityBeforeSweep = activeBeforeSweep.add(vaultBeforeSweep);
		BankrollState postPnlBeforeSweep = new BankrollState(
				OWNER,
				activeBeforeSweep,
				vaultBeforeSweep,
				afterDeposit.cumulativeRealizedPnl().add(money("25000")),
				money("5000"),
				afterDeposit.activeDrawdownHighWaterMark().max(activeBeforeSweep),
				afterDeposit.totalEquityHighWaterMark().max(equityBeforeSweep));
		assertThat(postPnlBeforeSweep.activeDrawdownAmount()).isPositive();
		assertThat(result.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.BET_SETTLED_WIN, BankrollTransactionType.VAULT_SWEEP);
		assertThat(after.activeBankroll()).isEqualByComparingTo(activeBeforeSweep.subtract(money("1500")));
		assertThat(after.vaultBalance()).isEqualByComparingTo(vaultBeforeSweep.add(money("1500")));
		assertThat(after.totalEquity()).isEqualByComparingTo(equityBeforeSweep);
		assertThat(after.activeDrawdownAmount()).isPositive();
		assertThat(after.activeDrawdownRate()).isLessThanOrEqualTo(postPnlBeforeSweep.activeDrawdownRate());
		assertThat(after.cumulativeRealizedPnl()).isEqualByComparingTo("5000");
		assertThat(after.profitHighWaterMark()).isEqualByComparingTo("5000");
		assertThat(result.transactions().get(1).realizedPnlDelta()).isEqualByComparingTo("0");
	}

	@Test
	void nearFullActiveWithdrawalDuringDrawdownPreservesRatioWithoutNegativeOrOverHundredPercent() {
		BankrollState start = new BankrollState(
				OWNER,
				money("1000"),
				BigDecimal.ZERO,
				money("-250"),
				BigDecimal.ZERO,
				money("1250"),
				money("1250"));
		assertThat(start.activeDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		AccountingResult result = engine.withdrawFromActive(start, money("900"), "wd-near-full", OCCURRED_AT);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("100");
		assertThat(result.state().activeDrawdownHighWaterMark()).isEqualByComparingTo("125");
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(start.activeDrawdownRate());
		assertThat(result.state().activeDrawdownHighWaterMark()).isPositive();
		assertThat(result.state().activeDrawdownRate()).isLessThanOrEqualTo(BigDecimal.ONE);
		assertThat(result.state().cumulativeRealizedPnl()).isEqualByComparingTo("-250");
	}

	@Test
	void fullActiveWithdrawalAtZeroDrawdownAllowsZeroBalanceWithZeroDrawdownRate() {
		BankrollState start = initialBankroll();
		AccountingResult result = engine.withdrawFromActive(start, start.activeBankroll(), "wd-all-peak", OCCURRED_AT);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("0");
		assertThat(result.state().activeDrawdownHighWaterMark()).isEqualByComparingTo("0");
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo("0");
		assertThat(result.state().totalEquityDrawdownRate()).isEqualByComparingTo("0");
		assertThat(result.state().cumulativeRealizedPnl()).isEqualByComparingTo("0");
		assertThat(result.state().profitHighWaterMark()).isEqualByComparingTo("0");
	}

	@Test
	void fullActiveWithdrawalDuringDrawdownIsRejectedBecauseRateCannotBePreserved() {
		BankrollState start = activeDrawdownTwentyPercent();
		assertThatThrownBy(() -> engine.withdrawFromActive(start, start.activeBankroll(), "wd-all-dd", OCCURRED_AT))
				.isInstanceOf(BankrollException.class)
				.hasMessageContaining("drawdown rate cannot be preserved");
	}

	@Test
	void vaultWithdrawalDuringTotalEquityDrawdownPreservesEquityDrawdownRate() {
		BankrollState start = new BankrollState(
				OWNER,
				money("60000"),
				money("20000"),
				money("-20000"),
				BigDecimal.ZERO,
				money("75000"),
				money("100000"));
		assertThat(start.totalEquity()).isEqualByComparingTo("80000");
		assertThat(start.totalEquityDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		BigDecimal activeRateBefore = start.activeDrawdownRate();
		AccountingResult result = engine.withdrawFromVault(start, money("10000"), "vault-wd-dd", OCCURRED_AT);
		assertThat(result.state().vaultBalance()).isEqualByComparingTo("10000");
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("60000");
		assertThat(result.state().totalEquity()).isEqualByComparingTo("70000");
		assertThat(result.state().totalEquityHighWaterMark()).isEqualByComparingTo("87500");
		assertThat(result.state().totalEquityDrawdownRate()).isEqualByComparingTo(start.totalEquityDrawdownRate());
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(activeRateBefore);
		assertUnchangedPerformance(start, result.state());
	}

	@Test
	void depositDuringTotalEquityDrawdownPreservesEquityDrawdownRate() {
		BankrollState start = new BankrollState(
				OWNER,
				money("60000"),
				money("20000"),
				money("-20000"),
				BigDecimal.ZERO,
				money("75000"),
				money("100000"));
		assertThat(start.totalEquityDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		AccountingResult result = engine.depositToActive(start, money("20000"), "dep-eq-dd", OCCURRED_AT);
		assertThat(result.state().totalEquity()).isEqualByComparingTo("100000");
		assertThat(result.state().totalEquityHighWaterMark()).isEqualByComparingTo("125000");
		assertThat(result.state().totalEquityDrawdownRate()).isEqualByComparingTo(start.totalEquityDrawdownRate());
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(start.activeDrawdownRate());
		assertUnchangedPerformance(start, result.state());
		assertThat(result.state().vaultBalance()).isEqualByComparingTo("20000");
		assertThat(result.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.DEPOSIT);
	}

	@Test
	void sequentialMixedCapitalFlowsDoNotFakePerformanceOrRescueFromVault() {
		BankrollState afterLoss = applyProfit(initialBankroll(), "-20000", vaultOn("0.30")).state();
		assertThat(afterLoss.activeBankroll()).isEqualByComparingTo("80000");
		assertThat(afterLoss.activeDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		assertThat(afterLoss.vaultBalance()).isEqualByComparingTo("0");
		BankrollState afterDeposit = engine.depositToActive(afterLoss, money("10000"), "mix-dep", OCCURRED_AT).state();
		assertThat(afterDeposit.activeDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		assertThat(afterDeposit.cumulativeRealizedPnl()).isEqualByComparingTo("-20000");
		BankrollState afterWithdraw = engine.withdrawFromActive(afterDeposit, money("5000"), "mix-wd", OCCURRED_AT)
				.state();
		assertThat(afterWithdraw.activeDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		BankrollState withVault = new BankrollState(
				OWNER,
				afterWithdraw.activeBankroll(),
				money("20000"),
				afterWithdraw.cumulativeRealizedPnl(),
				afterWithdraw.profitHighWaterMark(),
				afterWithdraw.activeDrawdownHighWaterMark(),
				afterWithdraw.totalEquityHighWaterMark()
						.max(afterWithdraw.activeBankroll().add(money("20000"))));
		AccountingResult transfer = engine.transferVaultToActive(withVault, money("5000"), "mix-xfer", OCCURRED_AT);
		assertThat(transfer.state().activeDrawdownRate()).isEqualByComparingTo(TWENTY_PERCENT);
		assertThat(transfer.state().cumulativeRealizedPnl()).isEqualByComparingTo("-20000");
		assertThat(transfer.state().profitHighWaterMark()).isEqualByComparingTo("0");
		assertThat(transfer.state().totalEquity()).isEqualByComparingTo(
				transfer.state().activeBankroll().add(transfer.state().vaultBalance()));
		assertThat(transfer.state().activeDrawdownHighWaterMark()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
		assertThat(transfer.state().vaultBalance()).isEqualByComparingTo("15000");
		assertThat(transfer.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.VAULT_TRANSFER_TO_ACTIVE);
	}

	@Test
	void depositThenMatchingWithdrawalRestoresDrawdownRates() {
		BankrollState start = activeDrawdownTwentyPercent();
		BankrollState afterDeposit = engine.depositToActive(start, money("10000"), "rev-dep", OCCURRED_AT).state();
		BankrollState restored = engine.withdrawFromActive(afterDeposit, money("10000"), "rev-wd", OCCURRED_AT).state();
		assertThat(restored.activeBankroll()).isEqualByComparingTo(start.activeBankroll());
		assertThat(restored.vaultBalance()).isEqualByComparingTo(start.vaultBalance());
		assertThat(restored.cumulativeRealizedPnl()).isEqualByComparingTo(start.cumulativeRealizedPnl());
		assertThat(restored.profitHighWaterMark()).isEqualByComparingTo(start.profitHighWaterMark());
		assertThat(restored.activeDrawdownRate()).isEqualByComparingTo(start.activeDrawdownRate());
		assertThat(restored.totalEquityDrawdownRate()).isEqualByComparingTo(start.totalEquityDrawdownRate());
		assertThat(restored.activeDrawdownHighWaterMark()).isEqualByComparingTo(start.activeDrawdownHighWaterMark());
		assertThat(restored.totalEquityHighWaterMark()).isEqualByComparingTo(start.totalEquityHighWaterMark());
	}

	@Test
	void pushDuringDrawdownDoesNotChangeReferencesOrSweep() {
		BankrollState start = activeDrawdownTwentyPercent();
		AccountingResult result = engine.applyPayout(
				start, SettlementResult.PUSH, payout(SettlementResult.PUSH), vaultOn("0.30"), REFERENCE, OCCURRED_AT);
		assertThat(result.state()).isEqualTo(start);
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(start.activeDrawdownRate());
		assertThat(result.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.BET_SETTLED_PUSH);
		assertThat(result.transactions().getFirst().realizedPnlDelta()).isEqualByComparingTo("0");
	}

	private AccountingResult applyProfit(BankrollState state, String profit, StrategyConfig config) {
		return engine.applyPayout(
				state, settlementFor(profit), signedProfit(profit), config, REFERENCE, OCCURRED_AT);
	}

	private static BankrollState activeDrawdownTwentyPercent() {
		return new BankrollState(
				OWNER,
				money("80000"),
				BigDecimal.ZERO,
				money("-20000"),
				BigDecimal.ZERO,
				money("100000"),
				money("100000"));
	}

	private static void assertUnchangedPerformanceAndVault(BankrollState before, BankrollState after) {
		assertUnchangedPerformance(before, after);
		assertThat(after.vaultBalance()).isEqualByComparingTo(before.vaultBalance());
	}

	private static void assertUnchangedPerformance(BankrollState before, BankrollState after) {
		assertThat(after.cumulativeRealizedPnl()).isEqualByComparingTo(before.cumulativeRealizedPnl());
		assertThat(after.profitHighWaterMark()).isEqualByComparingTo(before.profitHighWaterMark());
	}

}
