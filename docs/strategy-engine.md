# Strategy Engine

The Strategy Engine answers whether SafeEdge should accept an already-evaluated opportunity and, if so, what stake is allowed. It does not predict probabilities, compute candidate edge, mutate bankroll/exposure, sweep Vault, or place bets.

```text
StrategyConfig + BettingOpportunity + BankrollState + PortfolioExposure
        ↓
  StrategyEngine
        ↓
    StrategyDecision
```

Live recommendations and backtests must call this same engine. There is no preset-specific engine. `StrategyEngine` consumes `StrategyConfig` only.

## Inputs

- **edge** is expected net return per unit stake (`Σ p_i R_i`, 3% → `0.03`). [CandidateEngine](candidate-engine.md) computes it from a point-in-time score distribution and observed odds. This engine only checks `edge >= minimumEdge`. `minimumEdge = 0.03` means: require at least +3% modelled expected net return per unit stake. Edge may be negative; it is not `1/odds` and not a probability-point difference.
- **SettlementProbabilityDistribution** is the five-outcome mass for WIN / HALF_WIN / PUSH / HALF_LOSS / LOSS. Probabilities must each lie in `[0, 1]` and **sum to 1**. They are not silently normalized. CandidateEngine derives this by calling `SettlementEngine` on each scoreline.
- **PortfolioExposure** is current committed stake **amounts** (not rates). If current exposure already exceeds the configured limit, remaining capacity is zero; the exposure object is still valid.
- Stake base is `BankrollState.activeBankroll()`. Vault is never part of the stake base.

Invalid odds (`<= 1`) are domain input errors, not normal rejections.

## Generalized Kelly

Asian Handicap is not binary. For unit stake at decimal odds `O`, net return `R` is:

| Outcome | R |
|---|---|
| WIN | `O - 1` |
| HALF_WIN | `(O - 1) / 2` |
| PUSH | `0` |
| HALF_LOSS | `-0.5` |
| LOSS | `-1` |

Full Kelly maximizes `G(f) = Σ p_i ln(1 + f R_i)` by solving `G'(f) = Σ p_i R_i / (1 + f R_i) = 0` with BigDecimal bisection on `[0, 1 - 10^-12]`, `MathContext.DECIMAL128`, 128 iterations or interval `<= 10^-24`.

If `G'(0) <= 0` (expected return per unit stake), full Kelly is `0`. The engine never returns `f = 1`.

`expectedReturnRate` is `Σ p_i R_i`. When the opportunity was produced by CandidateEngine, `BettingOpportunity.edge` is that same value. StrategyEngine still computes expected return from the settlement distribution for Kelly; it does not trust a second independent formula.

## Stake order

1. Validate inputs
2. Minimum-edge gate
3. Drawdown STOP (`activeDrawdownRate >= stop` → `PAUSED_DRAWDOWN`, stake 0, no Vault rescue)
4. Staking-mode size: fractional Kelly (`active * fullKelly * kellyFraction`) or flat (`active * flatStakeRate`). Flat stake does **not** run Kelly and does **not** reject on non-positive expected return. Fractional Kelly does.
5. Max-stake hard cap
6. Drawdown reduction multiplier if `reduction <= drawdown < stop` (after the cap). Warning band adds `DRAWDOWN_WARNING` without changing stake.
7. Match / league / daily remaining capacity: `max(0, active * rate - currentAmount)`
8. ACCEPT if `finalStake > 0`

An exposure cap **reduces** stake when remaining capacity is positive but smaller than the proposed stake. It rejects only when remaining capacity is 0. A reason is recorded for every cap whose remaining capacity is strictly below the pre-exposure stake.

## Worked example

Active bankroll 100000, `maxStakeRate` 2%, quarter Kelly on a 2.00 / 60% WIN binary opportunity (full Kelly 0.20 → raw stake 5000), drawdown in the reduction band with multiplier 0.50, match remaining 700:

```text
5000 → max-stake cap 2000 → drawdown ×0.50 → 1000 → match cap 700
ACCEPTED stake=700
reasons: MAX_STAKE_CAPPED, DRAWDOWN_REDUCTION_APPLIED, MATCH_EXPOSURE_CAPPED
```

The same active bankroll with a different Vault balance produces the same decision.
