package com.safeedge.bankroll;

import static com.safeedge.bankroll.BankrollFixtures.OCCURRED_AT;
import static com.safeedge.bankroll.BankrollFixtures.OWNER;
import static com.safeedge.bankroll.BankrollFixtures.REFERENCE;
import static com.safeedge.bankroll.BankrollFixtures.initialBankroll;
import static com.safeedge.bankroll.BankrollFixtures.money;
import static com.safeedge.bankroll.BankrollFixtures.payout;
import static com.safeedge.bankroll.BankrollFixtures.settlementFor;
import static com.safeedge.bankroll.BankrollFixtures.signedProfit;
import static com.safeedge.bankroll.BankrollFixtures.vaultOff;
import static com.safeedge.bankroll.BankrollFixtures.vaultOn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.settlement.PayoutResult;
import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BankrollAccountingEngineTest {

	private final BankrollAccountingEngine engine = new BankrollAccountingEngine();

	@ParameterizedTest
	@CsvSource({
			"WIN, 100250, 250, BET_SETTLED_WIN",
			"HALF_WIN, 100125, 125, BET_SETTLED_HALF_WIN",
			"PUSH, 100000, 0, BET_SETTLED_PUSH",
			"HALF_LOSS, 99500, -500, BET_SETTLED_HALF_LOSS",
			"LOSS, 99000, -1000, BET_SETTLED_LOSS"
	})
	void settledPayoutChangesActiveByNetProfitWithoutVault(
			SettlementResult settlementResult,
			String expectedActive,
			String expectedPnl,
			BankrollTransactionType expectedType) {
		BankrollState start = initialBankroll();
		PayoutResult payout = payout(settlementResult);
		AccountingResult result = engine.applyPayout(start, settlementResult, payout, vaultOff(), REFERENCE, OCCURRED_AT);
		assertThat(payout.profit()).isEqualByComparingTo(expectedPnl);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo(expectedActive);
		assertThat(result.state().vaultBalance()).isEqualByComparingTo("0");
		assertThat(result.state().cumulativeRealizedPnl()).isEqualByComparingTo(expectedPnl);
		assertThat(result.state().totalEquity())
				.isEqualByComparingTo(start.totalEquity().add(payout.profit()));
		assertThat(result.transactions()).hasSize(1);
		BankrollTransaction tx = result.transactions().getFirst();
		assertThat(tx.type()).isEqualTo(expectedType);
		assertThat(tx.activeBankrollDelta()).isEqualByComparingTo(expectedPnl);
		assertThat(tx.vaultDelta()).isEqualByComparingTo("0");
		assertThat(tx.realizedPnlDelta()).isEqualByComparingTo(expectedPnl);
		assertThat(tx.referenceId()).isEqualTo(REFERENCE);
		assertThat(tx.occurredAt()).isEqualTo(OCCURRED_AT);
		assertThat(tx.ownerId()).isEqualTo(OWNER);
		assertThat(start.activeBankroll()).isEqualByComparingTo("100000");
	}

	@Test
	void vaultSweepOnFirstProfitPeak() {
		AccountingResult result = applyProfit(initialBankroll(), "10000", vaultOn("0.30"));
		BankrollState state = result.state();
		assertThat(state.activeBankroll()).isEqualByComparingTo("107000");
		assertThat(state.vaultBalance()).isEqualByComparingTo("3000");
		assertThat(state.totalEquity()).isEqualByComparingTo("110000");
		assertThat(state.cumulativeRealizedPnl()).isEqualByComparingTo("10000");
		assertThat(state.profitHighWaterMark()).isEqualByComparingTo("10000");
		assertThat(state.activeDrawdownHighWaterMark()).isEqualByComparingTo("107000");
		assertThat(state.totalEquityHighWaterMark()).isEqualByComparingTo("110000");
		assertThat(state.activeDrawdownAmount()).isEqualByComparingTo("0");
		assertThat(state.totalEquityDrawdownAmount()).isEqualByComparingTo("0");
		assertThat(result.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.BET_SETTLED_WIN, BankrollTransactionType.VAULT_SWEEP);
		assertDeltas(result.transactions().get(0), "10000", "0", "10000");
		assertDeltas(result.transactions().get(1), "-3000", "3000", "0");
		assertThat(result.transactions().get(1).totalEquityDelta()).isEqualByComparingTo("0");
		assertThat(netActive(result.transactions())).isEqualByComparingTo("7000");
		assertThat(netVault(result.transactions())).isEqualByComparingTo("3000");
		assertThat(netRealized(result.transactions())).isEqualByComparingTo("10000");
		assertThat(state.totalEquity()).isEqualByComparingTo(initialBankroll().totalEquity().add(money("10000")));
	}

	@Test
	void vaultOffPerformsNoSweepButAdvancesProfitHighWaterMark() {
		AccountingResult result = applyProfit(initialBankroll(), "10000", vaultOff());
		BankrollState state = result.state();
		assertThat(state.activeBankroll()).isEqualByComparingTo("110000");
		assertThat(state.vaultBalance()).isEqualByComparingTo("0");
		assertThat(state.profitHighWaterMark()).isEqualByComparingTo("10000");
		assertThat(state.cumulativeRealizedPnl()).isEqualByComparingTo("10000");
		assertThat(result.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.BET_SETTLED_WIN);
	}

	@Test
	void enablingVaultLaterDoesNotRetroactivelySweepOldProfit() {
		BankrollState afterOff = applyProfit(initialBankroll(), "10000", vaultOff()).state();
		AccountingResult unchangedPeak = applyProfit(afterOff, "0", vaultOn("0.30"));
		assertThat(unchangedPeak.state().vaultBalance()).isEqualByComparingTo("0");
		assertThat(unchangedPeak.state().profitHighWaterMark()).isEqualByComparingTo("10000");
		assertThat(unchangedPeak.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.BET_SETTLED_PUSH);
		AccountingResult newPeak = applyProfit(unchangedPeak.state(), "2000", vaultOn("0.30"));
		assertThat(newPeak.state().cumulativeRealizedPnl()).isEqualByComparingTo("12000");
		assertThat(newPeak.state().profitHighWaterMark()).isEqualByComparingTo("12000");
		assertThat(newPeak.state().vaultBalance()).isEqualByComparingTo("600");
		assertThat(newPeak.state().activeBankroll()).isEqualByComparingTo("111400");
		assertThat(newPeak.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.BET_SETTLED_WIN, BankrollTransactionType.VAULT_SWEEP);
		assertDeltas(newPeak.transactions().get(1), "-600", "600", "0");
	}

	@Test
	void recoveredProfitBelowOldHighWaterMarkIsNotSwept() {
		BankrollState afterFirstSweep = applyProfit(initialBankroll(), "10000", vaultOn("0.30")).state();
		BankrollState afterLoss = applyProfit(afterFirstSweep, "-15000", vaultOn("0.30")).state();
		assertThat(afterLoss.vaultBalance()).isEqualByComparingTo("3000");
		assertThat(afterLoss.activeBankroll()).isEqualByComparingTo("92000");
		assertThat(afterLoss.cumulativeRealizedPnl()).isEqualByComparingTo("-5000");
		assertThat(afterLoss.profitHighWaterMark()).isEqualByComparingTo("10000");
		BankrollState recovered = applyProfit(afterLoss, "10000", vaultOn("0.30")).state();
		assertThat(recovered.cumulativeRealizedPnl()).isEqualByComparingTo("5000");
		assertThat(recovered.vaultBalance()).isEqualByComparingTo("3000");
		assertThat(recovered.activeBankroll()).isEqualByComparingTo("102000");
		assertThat(recovered.profitHighWaterMark()).isEqualByComparingTo("10000");
	}

	@Test
	void onlyProfitAboveOldHighWaterMarkIsSwept() {
		BankrollState recovered = applySequence(initialBankroll(), vaultOn("0.30"), "10000", "-15000", "10000");
		AccountingResult result = applyProfit(recovered, "10000", vaultOn("0.30"));
		assertThat(result.state().cumulativeRealizedPnl()).isEqualByComparingTo("15000");
		assertThat(result.state().profitHighWaterMark()).isEqualByComparingTo("15000");
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("110500");
		assertThat(result.state().vaultBalance()).isEqualByComparingTo("4500");
		assertThat(result.state().totalEquity()).isEqualByComparingTo("115000");
		assertDeltas(result.transactions().get(1), "-1500", "1500", "0");
	}

	@Test
	void vaultSweepLeavesTotalEquityUnchangedAndCreatesNoActiveDrawdown() {
		BankrollState start = initialBankroll();
		AccountingResult result = applyProfit(start, "10000", vaultOn("0.30"));
		assertThat(result.state().totalEquity()).isEqualByComparingTo(start.totalEquity().add(money("10000")));
		assertThat(result.transactions().get(1).totalEquityDelta()).isEqualByComparingTo("0");
		assertThat(result.state().activeDrawdownAmount()).isEqualByComparingTo("0");
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo("0");
	}

	@Test
	void vaultNeverAutoTransfersToActiveAfterLoss() {
		BankrollState afterSweep = applyProfit(initialBankroll(), "10000", vaultOn("0.30")).state();
		AccountingResult afterLoss = applyProfit(afterSweep, "-15000", vaultOn("0.30"));
		assertThat(afterLoss.state().vaultBalance()).isEqualByComparingTo("3000");
		assertThat(afterLoss.state().activeBankroll()).isEqualByComparingTo("92000");
		assertThat(afterLoss.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.BET_SETTLED_LOSS);
		assertThat(afterLoss.transactions().getFirst().vaultDelta()).isEqualByComparingTo("0");
	}

	@Test
	void explicitVaultToActiveTransferPreservesActiveDrawdownRate() {
		BankrollState start = new BankrollState(
				OWNER,
				money("80000"),
				money("20000"),
				money("-20000"),
				BigDecimal.ZERO,
				money("100000"),
				money("110000"));
		BigDecimal rateBefore = start.activeDrawdownRate();
		AccountingResult result = engine.transferVaultToActive(start, money("10000"), "manual-1", OCCURRED_AT);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("90000");
		assertThat(result.state().vaultBalance()).isEqualByComparingTo("10000");
		assertThat(result.state().activeDrawdownHighWaterMark()).isEqualByComparingTo("112500");
		assertThat(result.state().totalEquityHighWaterMark()).isEqualByComparingTo("110000");
		assertThat(result.state().totalEquity()).isEqualByComparingTo(start.totalEquity());
		assertThat(result.state().cumulativeRealizedPnl()).isEqualByComparingTo("-20000");
		assertThat(result.state().profitHighWaterMark()).isEqualByComparingTo("0");
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(rateBefore);
		assertThat(result.transactions()).hasSize(1);
		assertThat(result.transactions().getFirst().type()).isEqualTo(BankrollTransactionType.VAULT_TRANSFER_TO_ACTIVE);
		assertDeltas(result.transactions().getFirst(), "10000", "-10000", "0");
		assertThat(result.transactions().getFirst().totalEquityDelta()).isEqualByComparingTo("0");
	}

	@Test
	void vaultTransferLargerThanBalanceFails() {
		BankrollState state = applyProfit(initialBankroll(), "10000", vaultOn("0.30")).state();
		assertThatThrownBy(() -> engine.transferVaultToActive(state, money("3000.01"), REFERENCE, OCCURRED_AT))
				.isInstanceOf(BankrollException.class);
	}

	@Test
	void lossAfterVaultSweepProducesActiveAndTotalEquityDrawdown() {
		BankrollState afterSweep = applyProfit(initialBankroll(), "10000", vaultOn("0.30")).state();
		BankrollState afterLoss = applyProfit(afterSweep, "-10000", vaultOn("0.30")).state();
		assertThat(afterLoss.activeBankroll()).isEqualByComparingTo("97000");
		assertThat(afterLoss.vaultBalance()).isEqualByComparingTo("3000");
		assertThat(afterLoss.totalEquity()).isEqualByComparingTo("100000");
		assertThat(afterLoss.activeDrawdownAmount()).isEqualByComparingTo("10000");
		assertThat(afterLoss.activeDrawdownRate())
				.isEqualByComparingTo(new BigDecimal("10000").divide(new BigDecimal("107000"), MathContext.DECIMAL128));
		assertThat(afterLoss.totalEquityDrawdownAmount()).isEqualByComparingTo("10000");
		assertThat(afterLoss.totalEquityDrawdownRate())
				.isEqualByComparingTo(new BigDecimal("10000").divide(new BigDecimal("110000"), MathContext.DECIMAL128));
	}

	@Test
	void depositIncreasesActiveWithoutCountingAsProfitOrSweeping() {
		BankrollState start = initialBankroll();
		AccountingResult result = engine.depositToActive(start, money("25000"), "deposit-1", OCCURRED_AT);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("125000");
		assertThat(result.state().totalEquity()).isEqualByComparingTo("125000");
		assertThat(result.state().vaultBalance()).isEqualByComparingTo("0");
		assertThat(result.state().cumulativeRealizedPnl()).isEqualByComparingTo("0");
		assertThat(result.state().profitHighWaterMark()).isEqualByComparingTo("0");
		assertThat(result.state().activeDrawdownHighWaterMark()).isEqualByComparingTo("125000");
		assertThat(result.state().totalEquityHighWaterMark()).isEqualByComparingTo("125000");
		assertThat(result.state().activeDrawdownAmount()).isEqualByComparingTo("0");
		assertThat(result.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.DEPOSIT);
		assertThat(result.transactions().getFirst().realizedPnlDelta()).isEqualByComparingTo("0");
	}

	@Test
	void depositDoesNotReduceExistingDrawdownRate() {
		BankrollState drawnDown = applyProfit(initialBankroll(), "-20000", vaultOn("0.30")).state();
		BigDecimal rateBefore = drawnDown.activeDrawdownRate();
		AccountingResult result = engine.depositToActive(drawnDown, money("5000"), "deposit-2", OCCURRED_AT);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("85000");
		assertThat(result.state().activeDrawdownHighWaterMark()).isEqualByComparingTo("106250");
		assertThat(result.state().activeDrawdownRate()).isEqualByComparingTo(rateBefore);
		assertThat(result.state().cumulativeRealizedPnl()).isEqualByComparingTo("-20000");
		assertThat(result.state().profitHighWaterMark()).isEqualByComparingTo("0");
		assertThat(result.state().vaultBalance()).isEqualByComparingTo("0");
	}

	@Test
	void activeWithdrawalIsCapitalFlowNotProfit() {
		AccountingResult result = engine.withdrawFromActive(initialBankroll(), money("20000"), "wd-1", OCCURRED_AT);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("80000");
		assertThat(result.state().totalEquity()).isEqualByComparingTo("80000");
		assertThat(result.state().cumulativeRealizedPnl()).isEqualByComparingTo("0");
		assertThat(result.state().profitHighWaterMark()).isEqualByComparingTo("0");
		assertThat(result.state().activeDrawdownHighWaterMark()).isEqualByComparingTo("80000");
		assertThat(result.state().totalEquityHighWaterMark()).isEqualByComparingTo("80000");
		assertThat(result.state().activeDrawdownAmount()).isEqualByComparingTo("0");
		assertThat(result.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.ACTIVE_WITHDRAWAL);
		assertDeltas(result.transactions().getFirst(), "-20000", "0", "0");
	}

	@Test
	void vaultWithdrawalRemovesCapitalWithoutMovingToActive() {
		BankrollState afterSweep = applyProfit(initialBankroll(), "10000", vaultOn("0.30")).state();
		AccountingResult result = engine.withdrawFromVault(afterSweep, money("1000"), "vault-wd", OCCURRED_AT);
		assertThat(result.state().activeBankroll()).isEqualByComparingTo("107000");
		assertThat(result.state().vaultBalance()).isEqualByComparingTo("2000");
		assertThat(result.state().totalEquity()).isEqualByComparingTo("109000");
		assertThat(result.state().cumulativeRealizedPnl()).isEqualByComparingTo("10000");
		assertThat(result.state().profitHighWaterMark()).isEqualByComparingTo("10000");
		assertThat(result.state().totalEquityHighWaterMark()).isEqualByComparingTo("109000");
		assertThat(result.state().activeDrawdownHighWaterMark()).isEqualByComparingTo("107000");
		assertThat(result.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.VAULT_WITHDRAWAL);
		assertDeltas(result.transactions().getFirst(), "0", "-1000", "0");
	}

	@Test
	void overWithdrawalFails() {
		assertThatThrownBy(() -> engine.withdrawFromActive(initialBankroll(), money("100000.01"), REFERENCE, OCCURRED_AT))
				.isInstanceOf(BankrollException.class);
		BankrollState afterSweep = applyProfit(initialBankroll(), "10000", vaultOn("0.30")).state();
		assertThatThrownBy(() -> engine.withdrawFromVault(afterSweep, money("3001"), REFERENCE, OCCURRED_AT))
				.isInstanceOf(BankrollException.class);
	}

	@Test
	void zeroAndNegativeAmountsFail() {
		BankrollState state = initialBankroll();
		assertThatThrownBy(() -> engine.depositToActive(state, BigDecimal.ZERO, REFERENCE, OCCURRED_AT))
				.isInstanceOf(BankrollException.class);
		assertThatThrownBy(() -> engine.withdrawFromActive(state, money("-1"), REFERENCE, OCCURRED_AT))
				.isInstanceOf(BankrollException.class);
		assertThatThrownBy(() -> engine.transferVaultToActive(state, money("1"), REFERENCE, OCCURRED_AT))
				.isInstanceOf(BankrollException.class);
	}

	@Test
	void settlementThatWouldMakeActiveNegativeFails() {
		BankrollState small = BankrollState.initial(OWNER, money("500"));
		assertThatThrownBy(() -> engine.applyPayout(
						small, SettlementResult.LOSS, payout(SettlementResult.LOSS), vaultOff(), REFERENCE, OCCURRED_AT))
				.isInstanceOf(BankrollException.class);
	}

	@Test
	void returnedTransactionsAreImmutable() {
		AccountingResult result = applyProfit(initialBankroll(), "10000", vaultOn("0.30"));
		assertThatThrownBy(() -> result.transactions().add(result.transactions().getFirst()))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void winWithVaultSweepEmitsOrderedAuditEventsWhoseDeltasMatchState() {
		BankrollState start = initialBankroll();
		AccountingResult result = engine.applyPayout(
				start, SettlementResult.WIN, payout(SettlementResult.WIN), vaultOn("0.30"), REFERENCE, OCCURRED_AT);
		assertThat(result.transactions()).extracting(BankrollTransaction::type)
				.containsExactly(BankrollTransactionType.BET_SETTLED_WIN, BankrollTransactionType.VAULT_SWEEP);
		assertDeltas(result.transactions().get(0), "250", "0", "250");
		assertDeltas(result.transactions().get(1), "-75", "75", "0");
		assertThat(start.activeBankroll().add(netActive(result.transactions())))
				.isEqualByComparingTo(result.state().activeBankroll());
		assertThat(start.vaultBalance().add(netVault(result.transactions())))
				.isEqualByComparingTo(result.state().vaultBalance());
		assertThat(start.cumulativeRealizedPnl().add(netRealized(result.transactions())))
				.isEqualByComparingTo(result.state().cumulativeRealizedPnl());
		assertThat(result.state().totalEquity()).isEqualByComparingTo(start.totalEquity().add(money("250")));
	}

	@Test
	void endToEndProfitLossRecoverySweepsOnlyNewPeaks() {
		BankrollState finalState = applySequence(initialBankroll(), vaultOn("0.30"), "10000", "-15000", "10000", "10000");
		assertThat(finalState.cumulativeRealizedPnl()).isEqualByComparingTo("15000");
		assertThat(finalState.profitHighWaterMark()).isEqualByComparingTo("15000");
		assertThat(finalState.activeBankroll()).isEqualByComparingTo("110500");
		assertThat(finalState.vaultBalance()).isEqualByComparingTo("4500");
		assertThat(finalState.totalEquity()).isEqualByComparingTo("115000");
		assertThat(finalState.totalEquity()).isEqualByComparingTo(finalState.activeBankroll().add(finalState.vaultBalance()));
	}

	private AccountingResult applyProfit(BankrollState state, String profit, StrategyConfig config) {
		return engine.applyPayout(
				state, settlementFor(profit), signedProfit(profit), config, REFERENCE, OCCURRED_AT);
	}

	private BankrollState applySequence(BankrollState start, StrategyConfig config, String... profits) {
		BankrollState current = start;
		for (String profit : profits) {
			current = applyProfit(current, profit, config).state();
		}
		return current;
	}

	private static void assertDeltas(
			BankrollTransaction transaction, String active, String vault, String realized) {
		assertThat(transaction.activeBankrollDelta()).isEqualByComparingTo(active);
		assertThat(transaction.vaultDelta()).isEqualByComparingTo(vault);
		assertThat(transaction.realizedPnlDelta()).isEqualByComparingTo(realized);
	}

	private static BigDecimal netActive(List<BankrollTransaction> transactions) {
		return transactions.stream()
				.map(BankrollTransaction::activeBankrollDelta)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal netVault(List<BankrollTransaction> transactions) {
		return transactions.stream()
				.map(BankrollTransaction::vaultDelta)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal netRealized(List<BankrollTransaction> transactions) {
		return transactions.stream()
				.map(BankrollTransaction::realizedPnlDelta)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

}
