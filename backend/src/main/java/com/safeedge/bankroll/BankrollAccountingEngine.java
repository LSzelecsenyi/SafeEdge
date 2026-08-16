package com.safeedge.bankroll;

import com.safeedge.settlement.PayoutResult;
import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure, deterministic bankroll and Vault accounting. Callers supply
 * {@code occurredAt} and {@code referenceId}; this engine never reads a clock
 * or generates identifiers.
 *
 * Vault settings are taken from {@link StrategyConfig} only. This engine does
 * not branch on preset identity, size stakes, or act on drawdown thresholds.
 */
public final class BankrollAccountingEngine {

	public AccountingResult applyPayout(
			BankrollState state,
			SettlementResult settlementResult,
			PayoutResult payout,
			StrategyConfig strategyConfig,
			String referenceId,
			Instant occurredAt) {
		requireState(state);
		if (settlementResult == null) {
			throw new BankrollException("Settlement result is required");
		}
		if (payout == null) {
			throw new BankrollException("Payout result is required");
		}
		if (strategyConfig == null) {
			throw new BankrollException("Strategy config is required");
		}
		requireReference(referenceId, occurredAt);
		if (payout.stake().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BankrollException("Payout stake must be greater than 0");
		}
		BigDecimal pnl = requireNonNull(payout.profit(), "payout.profit");
		BigDecimal newActive = state.activeBankroll().add(pnl);
		rejectNegative(newActive, "activeBankroll");
		BigDecimal newCumulative = state.cumulativeRealizedPnl().add(pnl);
		BigDecimal newActiveHwm = state.activeDrawdownHighWaterMark().max(newActive);
		BigDecimal newVault = state.vaultBalance();
		BigDecimal newEquity = newActive.add(newVault);
		BigDecimal newTotalHwm = state.totalEquityHighWaterMark().max(newEquity);
		BigDecimal newProfitHwm = state.profitHighWaterMark();
		BigDecimal newlyCreatedProfit = BigDecimal.ZERO;
		if (newCumulative.compareTo(newProfitHwm) > 0) {
			newlyCreatedProfit = newCumulative.subtract(newProfitHwm);
			newProfitHwm = newCumulative;
		}
		List<BankrollTransaction> transactions = new ArrayList<>();
		transactions.add(transaction(
				state.ownerId(),
				settledType(settlementResult),
				pnl,
				BigDecimal.ZERO,
				pnl,
				occurredAt,
				referenceId));
		if (newlyCreatedProfit.compareTo(BigDecimal.ZERO) > 0 && strategyConfig.vaultEnabled()) {
			BigDecimal sweepAmount = newlyCreatedProfit.multiply(strategyConfig.vaultSweepRate());
			if (sweepAmount.compareTo(BigDecimal.ZERO) > 0) {
				newActive = newActive.subtract(sweepAmount);
				newVault = newVault.add(sweepAmount);
				newActiveHwm = newActiveHwm.subtract(sweepAmount);
				rejectNegative(newActive, "activeBankroll");
				rejectNegative(newActiveHwm, "activeDrawdownHighWaterMark");
				transactions.add(transaction(
						state.ownerId(),
						BankrollTransactionType.VAULT_SWEEP,
						sweepAmount.negate(),
						sweepAmount,
						BigDecimal.ZERO,
						occurredAt,
						referenceId));
			}
		}
		BankrollState next = new BankrollState(
				state.ownerId(),
				newActive,
				newVault,
				newCumulative,
				newProfitHwm,
				newActiveHwm,
				newTotalHwm);
		return new AccountingResult(next, transactions);
	}

	public AccountingResult transferVaultToActive(
			BankrollState state, BigDecimal amount, String referenceId, Instant occurredAt) {
		requireState(state);
		requireReference(referenceId, occurredAt);
		BigDecimal transfer = requirePositiveAmount(amount);
		if (transfer.compareTo(state.vaultBalance()) > 0) {
			throw new BankrollException("Vault transfer exceeds vaultBalance");
		}
		BankrollState next = new BankrollState(
				state.ownerId(),
				state.activeBankroll().add(transfer),
				state.vaultBalance().subtract(transfer),
				state.cumulativeRealizedPnl(),
				state.profitHighWaterMark(),
				state.activeDrawdownHighWaterMark().add(transfer),
				state.totalEquityHighWaterMark());
		return new AccountingResult(
				next,
				List.of(transaction(
						state.ownerId(),
						BankrollTransactionType.VAULT_TRANSFER_TO_ACTIVE,
						transfer,
						transfer.negate(),
						BigDecimal.ZERO,
						occurredAt,
						referenceId)));
	}

	public AccountingResult depositToActive(
			BankrollState state, BigDecimal amount, String referenceId, Instant occurredAt) {
		requireState(state);
		requireReference(referenceId, occurredAt);
		BigDecimal deposit = requirePositiveAmount(amount);
		BankrollState next = new BankrollState(
				state.ownerId(),
				state.activeBankroll().add(deposit),
				state.vaultBalance(),
				state.cumulativeRealizedPnl(),
				state.profitHighWaterMark(),
				state.activeDrawdownHighWaterMark().add(deposit),
				state.totalEquityHighWaterMark().add(deposit));
		return new AccountingResult(
				next,
				List.of(transaction(
						state.ownerId(),
						BankrollTransactionType.DEPOSIT,
						deposit,
						BigDecimal.ZERO,
						BigDecimal.ZERO,
						occurredAt,
						referenceId)));
	}

	public AccountingResult withdrawFromActive(
			BankrollState state, BigDecimal amount, String referenceId, Instant occurredAt) {
		requireState(state);
		requireReference(referenceId, occurredAt);
		BigDecimal withdrawal = requirePositiveAmount(amount);
		if (withdrawal.compareTo(state.activeBankroll()) > 0) {
			throw new BankrollException("Active withdrawal exceeds activeBankroll");
		}
		BigDecimal newActiveHwm = state.activeDrawdownHighWaterMark().subtract(withdrawal);
		BigDecimal newTotalHwm = state.totalEquityHighWaterMark().subtract(withdrawal);
		rejectNegative(newActiveHwm, "activeDrawdownHighWaterMark");
		rejectNegative(newTotalHwm, "totalEquityHighWaterMark");
		BankrollState next = new BankrollState(
				state.ownerId(),
				state.activeBankroll().subtract(withdrawal),
				state.vaultBalance(),
				state.cumulativeRealizedPnl(),
				state.profitHighWaterMark(),
				newActiveHwm,
				newTotalHwm);
		return new AccountingResult(
				next,
				List.of(transaction(
						state.ownerId(),
						BankrollTransactionType.ACTIVE_WITHDRAWAL,
						withdrawal.negate(),
						BigDecimal.ZERO,
						BigDecimal.ZERO,
						occurredAt,
						referenceId)));
	}

	public AccountingResult withdrawFromVault(
			BankrollState state, BigDecimal amount, String referenceId, Instant occurredAt) {
		requireState(state);
		requireReference(referenceId, occurredAt);
		BigDecimal withdrawal = requirePositiveAmount(amount);
		if (withdrawal.compareTo(state.vaultBalance()) > 0) {
			throw new BankrollException("Vault withdrawal exceeds vaultBalance");
		}
		BigDecimal newTotalHwm = state.totalEquityHighWaterMark().subtract(withdrawal);
		rejectNegative(newTotalHwm, "totalEquityHighWaterMark");
		BankrollState next = new BankrollState(
				state.ownerId(),
				state.activeBankroll(),
				state.vaultBalance().subtract(withdrawal),
				state.cumulativeRealizedPnl(),
				state.profitHighWaterMark(),
				state.activeDrawdownHighWaterMark(),
				newTotalHwm);
		return new AccountingResult(
				next,
				List.of(transaction(
						state.ownerId(),
						BankrollTransactionType.VAULT_WITHDRAWAL,
						BigDecimal.ZERO,
						withdrawal.negate(),
						BigDecimal.ZERO,
						occurredAt,
						referenceId)));
	}

	private static BankrollTransactionType settledType(SettlementResult settlementResult) {
		return switch (settlementResult) {
			case WIN -> BankrollTransactionType.BET_SETTLED_WIN;
			case HALF_WIN -> BankrollTransactionType.BET_SETTLED_HALF_WIN;
			case PUSH -> BankrollTransactionType.BET_SETTLED_PUSH;
			case HALF_LOSS -> BankrollTransactionType.BET_SETTLED_HALF_LOSS;
			case LOSS -> BankrollTransactionType.BET_SETTLED_LOSS;
		};
	}

	private static BankrollTransaction transaction(
			OwnerId ownerId,
			BankrollTransactionType type,
			BigDecimal activeDelta,
			BigDecimal vaultDelta,
			BigDecimal realizedPnlDelta,
			Instant occurredAt,
			String referenceId) {
		return new BankrollTransaction(
				ownerId, type, activeDelta, vaultDelta, realizedPnlDelta, occurredAt, referenceId);
	}

	private static void requireState(BankrollState state) {
		if (state == null) {
			throw new BankrollException("Bankroll state is required");
		}
	}

	private static void requireReference(String referenceId, Instant occurredAt) {
		if (referenceId == null) {
			throw new BankrollException("referenceId is required");
		}
		if (occurredAt == null) {
			throw new BankrollException("occurredAt is required");
		}
	}

	private static BigDecimal requirePositiveAmount(BigDecimal amount) {
		BigDecimal value = requireNonNull(amount, "amount");
		if (value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BankrollException("amount must be greater than 0");
		}
		return value;
	}

	private static BigDecimal requireNonNull(BigDecimal value, String name) {
		if (value == null) {
			throw new BankrollException(name + " is required");
		}
		return value;
	}

	private static void rejectNegative(BigDecimal value, String name) {
		if (value.compareTo(BigDecimal.ZERO) < 0) {
			throw new BankrollException(name + " cannot be negative");
		}
	}

}
