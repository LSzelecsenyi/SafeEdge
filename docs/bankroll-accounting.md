# Bankroll accounting

SafeEdge accounts for **equity after settled bets**. This domain is pure and deterministic. It is not persisted yet. It does not model open bets, reserved cash, or exposure.

```text
stakeBase   = activeBankroll
totalEquity = activeBankroll + vaultBalance
```

Vault is protected capital. It is not part of the stake base and is never used automatically to fund losses.

## State

`BankrollState` is owner-specific (`OwnerId`). There is no global singleton bankroll.

- **activeBankroll** — betting capital / stake base
- **vaultBalance** — protected capital
- **cumulativeRealizedPnl** — sum of settled `PayoutResult.profit()` values
- **profitHighWaterMark** — highest cumulative realized P&L already processed (sweep eligibility)
- **activeDrawdownHighWaterMark** — reference for betting-capital drawdown
- **totalEquityHighWaterMark** — reference for total-equity drawdown

Drawdown amount is `max(0, HWM - current)`. Rate uses `MathContext.DECIMAL128`. Accounting does not act on strategy warning/reduction/stop thresholds.

A settled bet changes active bankroll by **net P&L**, not by `returnAmount`. Stake is not reserved in this model, so a +250 profit on 100000 becomes 100250, not 101250.

## Vault sweep

Sweep uses **new** cumulative realized profit above `profitHighWaterMark` only:

```text
newlyCreatedProfit = max(0, cumulativeRealizedPnl - profitHighWaterMark)
sweepAmount        = newlyCreatedProfit * vaultSweepRate   // if Vault is ON and newlyCreatedProfit > 0
```

The profit high-water mark advances whenever a new realized-P&L peak is reached, **including when Vault is OFF**. Turning Vault ON later does not sweep old unswept profit. Recovery back to an old peak does not sweep again.

A sweep is an internal transfer: active decreases, Vault increases, total equity is unchanged. The active drawdown HWM is reduced by the sweep amount so the sweep cannot create fake drawdown. The total-equity HWM is unchanged.

Vault ON/OFF is not an accounting operation. It comes from `StrategyConfig` on the next settlement. Existing Vault stays in Vault.

## Transfers and capital flows

- **Vault → Active** is explicit only. It is not betting P&L. Total equity is unchanged. Active drawdown HWM increases by the transfer so the move is not treated as performance recovery. No strategy stop may call this automatically.
- **Deposit to Active** is external capital, not profit. Realized P&L and profit HWM are unchanged. Both drawdown HWMs increase by the deposit. No sweep.
- **Withdraw from Active / Vault** are capital outflows, not betting losses. Realized P&L is unchanged. HWMs are reduced by the same cash flow and must not go negative.

## Regression example

Start Active 100000, Vault 0. Vault ON, sweep 30%.

| Step | Newly created profit | Sweep | Active | Vault | Equity | Realized P&L | Profit HWM |
|---|---|---|---|---|---|---|---|
| +10000 | 10000 | 3000 | 107000 | 3000 | 110000 | 10000 | 10000 |
| -15000 | 0 | 0 | 92000 | 3000 | 95000 | -5000 | 10000 |
| +10000 | 0 (still below 10000) | 0 | 102000 | 3000 | 105000 | 5000 | 10000 |
| +10000 | 5000 | 1500 | 110500 | 4500 | 115000 | 15000 | 15000 |

`BankrollAccountingEngine` returns immutable `AccountingResult` with ledger-shaped `BankrollTransaction` events (not persisted). `occurredAt` and `referenceId` are supplied by the caller. There is no `Clock` and no generated ids.
