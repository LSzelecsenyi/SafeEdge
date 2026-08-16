# Candidate Engine v1

`CandidateEngine` is the provider-independent layer between a future football probability model and the existing Strategy / Backtest engines. It evaluates **value**. It does not size stakes, apply exposure, or pause on drawdown.

```text
future football probability model
          ↓
  ScoreProbabilityDistribution
          ↓
      CandidateEngine
          ↓
  settlement probabilities
  expectedReturnRate  (= edge)
          ↓
     BettingOpportunity
          ↓
      StrategyEngine
```

There is **no trained predictive model** in this layer. Tests may supply synthetic distributions **or** a point-in-time `ScoreProbabilityDistribution` from [Probability Model v1](probability-model-v1.md). Bookmaker odds are a price, not a probability estimate. An event's final score must never be used to build the distribution for that same event.

The v1 football model is `PoissonFootballProbabilityModel`. v2 is `RegularizedDixonColesFootballProbabilityModel`. CandidateEngine still only consumes `ScoreProbabilityDistribution`.

## Score distribution

The same full-time score mass can price Asian Handicap (including quarter lines), European Handicap, Double Chance, and later totals, without a separate probability model per market.

Probabilities must each lie in `[0, 1]`, scorelines must be unique, and the total must **sum to 1**. They are not silently normalized. This type does not impose a maximum goal count; a truncated model must already sum to 1 before entering this layer.

## Settlement probabilities

For every scoreline, CandidateEngine calls `SettlementEngine.settle(market, selection, score)` and adds that score's probability to WIN / HALF_WIN / PUSH / HALF_LOSS / LOSS.

Live settlement and candidate evaluation therefore use the same market rules. CandidateEngine does not reimplement Asian Handicap line math.

European Handicap and Double Chance naturally produce WIN/LOSS only (half and push mass stay 0) because that is what `SettlementEngine` returns.

## Canonical value: expected net return

Asian Handicap is not a binary WIN/LOSS bet. `1 / odds` is **not** a sufficient break-even probability.

For decimal odds `O`, net return per unit stake is:

| Outcome | R |
|---|---|
| WIN | `O - 1` |
| HALF_WIN | `(O - 1) / 2` |
| PUSH | `0` |
| HALF_LOSS | `-0.5` |
| LOSS | `-1` |

```text
expectedReturnRate = Σ probability(outcome) * R(outcome)
```

Example: `0.04` means **+4% expected net return per unit stake**. It is not a guaranteed ROI.

This is the same `Σ p_i R_i` already used by `GeneralizedKellyCalculator`. CandidateEngine reuses that calculation.

### `edge := expectedReturnRate`

`BettingOpportunity.edge` is this expected net return rate. `StrategyConfig.minimumEdge = 0.03` means: require at least +3% modelled expected net return per unit stake.

CandidateEngine writes `edge` from the model distribution and observed odds. Callers must not supply a separate, conflicting edge.

`edge` may be negative. It is not a probability-point difference (`p - 1/odds`) and is not bounded to `[0, 1]`.

## Implied probability reference

```text
impliedProbabilityReference = 1 / observedOdds     (DECIMAL128)
```

This is the bookmaker's simple decimal-odds implied probability. It is **not** a complete break-even model for multi-settlement Asian Handicap markets and is **not** used to compute EV. It exists for UI/reference and binary-market comparison.

## Candidate vs strategy

| Layer | Responsibility |
|---|---|
| CandidateEngine | settlement distribution, expected return, POSITIVE / ZERO / NEGATIVE EV |
| StrategyEngine | `minimumEdge`, Kelly, max stake, exposure, drawdown |

A candidate is positive EV iff `expectedReturnRate > 0`. Zero is not positive. There is no 3% threshold here.

## Live and backtest

The same `BettingOpportunity` feeds:

- live / current Tippmix offers (point-in-time metadata in `CandidateContext`)
- `HistoricalBettingOpportunity` → `BacktestEngine` (no recalculation of odds, edge, or settlement probabilities)

The engine has no historical-only fields. It does not generate ids, read a clock, or see a final result.

## What v1 does not do

- Dixon-Coles, Elo, xG, logistic regression, ML, or form heuristics inside CandidateEngine
- `p = 1 / bookmaker odds` as a "model"
- probabilities from `MatchResult` / final score / `SettlementResult`
- StrategyConfig gates
- persistence, REST, or Angular UI

Score distributions may come from [Probability Model v1](probability-model-v1.md) (independent Poisson). CandidateEngine does not fit that model.
