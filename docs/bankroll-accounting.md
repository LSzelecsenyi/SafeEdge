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

A sweep is an internal transfer: active decreases, Vault increases, total equity is unchanged. Drawdown high-water marks are **rescaled** so the sweep does not change the post-settlement active drawdown **rate**. The total-equity HWM is unchanged because equity is unchanged.

Vault ON/OFF is not an accounting operation. It comes from `StrategyConfig` on the next settlement. Existing Vault stays in Vault.

## Capital flows and drawdown rate

External and internal capital flows adjust drawdown reference values so that the drawdown **percentage** immediately before and after the flow is unchanged. They do not change realized betting P&L or the profit high-water mark.

The HWM is rescaled from the measured drawdown rate so that rate is unchanged (`newHwm = newBalance / (1 - rate)`, `DECIMAL128` when needed):

- **Deposit to Active** — not profit; no sweep. Active and total-equity HWMs scale with the new balances.
- **Withdraw from Active / Vault** — not betting loss. The affected HWM(s) scale down with the new balances.
- **Vault → Active** — not betting recovery. Equity and total-equity HWM are unchanged. Active HWM scales with the new active balance.

Simply adding or subtracting the cash-flow amount from a HWM preserves drawdown *amount* and **changes** the rate. That is treated as fake performance and is not used.

Full withdrawal of active bankroll is allowed only when active drawdown is already zero (the zero/zero reference is defined as 0% drawdown). If a non-zero drawdown rate cannot be preserved at a zero balance, the operation is rejected.

## Transfers and capital flows

- **Vault → Active** is explicit only. It is not betting P&L. Total equity is unchanged. No strategy stop may call this automatically.
- **Deposit to Active** is external capital, not profit. No sweep.
- **Withdraw from Active / Vault** are capital outflows, not betting losses.

## Regression example

Start Active 100000, Vault 0. Vault ON, sweep 30%.

| Step | Newly created profit | Sweep | Active | Vault | Equity | Realized P&L | Profit HWM |
|---|---|---|---|---|---|---|---|
| +10000 | 10000 | 3000 | 107000 | 3000 | 110000 | 10000 | 10000 |
| -15000 | 0 | 0 | 92000 | 3000 | 95000 | -5000 | 10000 |
| +10000 | 0 (still below 10000) | 0 | 102000 | 3000 | 105000 | 5000 | 10000 |
| +10000 | 5000 | 1500 | 110500 | 4500 | 115000 | 15000 | 15000 |

`BankrollAccountingEngine` returns immutable `AccountingResult` with ledger-shaped `BankrollTransaction` events (not persisted). `occurredAt` and `referenceId` are supplied by the caller. There is no `Clock` and no generated ids.
