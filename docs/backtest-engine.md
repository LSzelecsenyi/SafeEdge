# Backtest Engine v1

`BacktestEngine` is a pure, deterministic replay of an already-prepared historical opportunity stream. It does not predict probabilities, generate candidates, query persistence, or place bets.

```text
historical opportunity
      ↓
settle open bets with settlementAt <= decisionAt
      ↓
derive PortfolioExposure
      ↓
StrategyEngine
      ↓
ACCEPT / REJECT / PAUSED_DRAWDOWN
      ↓
open simulated bet (exposure only; bankroll unchanged)
      ↓
at settlementAt: SettlementEngine → PayoutCalculator → BankrollAccountingEngine
      ↓
next BankrollState
```

Live recommendation flows must use the same four engines. Backtest v1 only orchestrates them in time.

## Prepared input

The engine does not invent opportunities. Each `HistoricalBettingOpportunity` already contains a `BettingOpportunity` (odds, **edge = expected net return rate**, settlement probability distribution, event/league/betting date) plus the normalized `BettingMarket` / `BettingSelection` that would have been bet and the exact `decisionAt` instant.

The walk-forward builder that prepares that stream is documented in [historical walk-forward evaluation](historical-walk-forward-evaluation.md). Backtest Engine must not recalculate odds, edge, or settlement probabilities, and must not derive those probabilities from the event's final score. It still does not query persistence.

`BettingOpportunity.odds` is the decision-time price used for payout. `BettingSelection.odds` is the same observed decimal price, so the two must match. The engine never substitutes later or closing odds.

Final scores are **not** stored on the opportunity. They live on a separate `HistoricalEventResult(eventId, settlementAt, finalScore)`. At most one result per `eventId`. Every opportunity must have an exact `eventId` match. `decisionAt` must be strictly before `settlementAt`.

## Point-in-time / look-ahead

A decision at time `T` may use only information that would have been known at `T`.

- The final score may exist in the dataset, but the engine consumes it only when that event reaches `settlementAt`.
- Future wins cannot increase bankroll, reduce drawdown, or change stake for an earlier opportunity.
- Opportunities must already be in non-decreasing `decisionAt` order. The engine does not reorder by edge, odds, profit, or result. Identical timestamps keep caller order.

## Chronological execution

Start from `BankrollState.initial(ownerId, startingBankroll)`: Active = starting bankroll, Vault = 0, no open bets, no deposits/withdrawals.

For each opportunity in caller order:

1. Settle every currently open bet with `settlementAt <= decisionAt` (equal timestamps: settle first).
2. If the run is latched on drawdown STOP, skip (do not call `StrategyEngine`).
3. If `maxAcceptedBets` accepted bets already exist, skip as a bet-limit skip (do not call `StrategyEngine`).
4. Otherwise derive `PortfolioExposure` from current open bets and the daily accepted-stake map, then call `StrategyEngine.decide`.
5. ACCEPT opens a simulated bet and reserves stake through exposure only. REJECT records strategy reasons. `PAUSED_DRAWDOWN` latches the run.

After the last opportunity, settle remaining open bets by `settlementAt` ascending, then acceptance order. The result contains no open bets.

### Timeline example

```text
10:00  Opportunity A accepted (event A, league L)
10:30  Opportunity B accepted, stake reduced by league exposure (event B, league L)
12:00  Event A settles → bankroll/Vault/drawdown update; A's match/league exposure released
12:05  Opportunity C sees the updated bankroll; daily exposure still includes A's stake
13:00  Event B settles
14:00  Event C settles
```

## Exposure

Exposure is derived before each decision. It is never supplied as a precomputed input.

| Kind | v1 definition | Released on settlement? |
|---|---|---|
| Match | sum of **currently open** stakes with the same `eventId` | yes |
| League | sum of **currently open** stakes with the same `leagueId` | yes |
| Daily | cumulative stake **accepted** on `BettingOpportunity.bettingDate` | **no** |

Daily exposure is a daily betting budget, not current open risk. Same-day settlement does not restore that day's capacity. The date is the opportunity's `bettingDate`, not a UTC calendar inferred from `decisionAt`.

## Drawdown STOP latch

When `StrategyEngine` returns `PAUSED_DRAWDOWN`, the backtest stops accepting new bets for the rest of the run. Later open-bet wins that repair bankroll do **not** resume betting. There is no Vault rescue. Remaining opportunities are `opportunitiesSkippedByDrawdownPause`. Open bets still settle.

## maxAcceptedBets

Optional. If present it must be `> 0`.

It means: stop accepting after the first N StrategyEngine-accepted bets in chronological input order.

It does **not** mean "the N most profitable bets" or any retrospective ranking. After the cap, open bets still settle. Skips are `opportunitiesSkippedByBetLimit`, not strategy rejections.

## Settlement ordering

- Before a decision at `T`, settle `settlementAt <= T`.
- If `openBet.settlementAt == opportunity.decisionAt`, settle first; the result is known at that timestamp.
- Multiple bets settling at the same instant are settled in **acceptance order** (deterministic Vault/HWM accounting).
- Multiple accepted bets on one event each settle separately against the same `HistoricalEventResult`.

Payout uses the opportunity's historical odds and the accepted stake. Accounting uses `BankrollAccountingEngine.applyPayout` so Vault sweeps and high-water marks match future live usage.

## Metrics

- `totalStake` / `totalReturn` are sums of accepted-bet stake and `returnAmount` (turnover), not final bankroll.
- `totalProfit` is the sum of accepted-bet `PayoutResult.profit()`. With no deposits/withdrawals: `totalProfit == finalTotalEquity - startingBankroll`.
- `ROI = totalProfit / totalStake` (`DECIMAL128`). If `totalStake == 0`, ROI is `0`.
- Settlement counts keep WIN / HALF_WIN / PUSH / HALF_LOSS / LOSS distinct.
- Longest losing streak: consecutive settled bets with `profit < 0` (LOSS and HALF_LOSS). WIN, HALF_WIN, and PUSH reset the streak.
- Max drawdown rates are the maxima of `BankrollState` rates after the initial state and every accounting transition. The engine does not reimplement drawdown math.
- Averages (odds, stake, edge) are over accepted bets only; `0` if none.
- `rejectionReasonCounts` counts StrategyEngine rejection reasons only. Bet-limit and drawdown-pause skips are separate counters.
- Equity curve: initial point, then one point after every settled bet (not after rejections).
- `acceptedBetResults` are in settlement order (`settlementAt` ascending, then acceptance order), matching the equity curve.

## What v1 does not do

- Probability modeling or deriving distributions from final scores
- Candidate generation or Tippmix normalization
- Querying persistence (the walk-forward builder loads historical rows and passes a prepared `BacktestRequest`)
- Persistence (`backtest_*` tables), REST, or Angular UI
- Monte Carlo, bootstrap, or parameter optimization
- CAGR, Sharpe, Sortino, risk of ruin, monthly summaries
- Deposits, withdrawals, or Vault rescue during a run
- Branching on `StrategyPreset` (the engine consumes `StrategyConfig` only)
