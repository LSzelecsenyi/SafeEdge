# Probability Model v1 — time-decayed independent Poisson

SafeEdge v1 estimates a **full-time score probability distribution** from historical match results that were known **before** the target date. It is a football scoring model, not a betting strategy and not a claim of profitability.

```text
historical matches (date < target)
        ↓
PoissonFootballProbabilityModel
        ↓
ScoreProbabilityDistribution  (sums to 1)
        ↓
CandidateEngine
        ↓
BettingOpportunity
```

This is an **independent Poisson** baseline. Dixon-Coles low-score dependence is **not** implemented: fitting ρ honestly needs extra estimation machinery, and a hardcoded ρ (for example `-0.1`) would be fake sophistication.

## What is fitted

Only prior results in the **same canonical competition**:

- weighted league home/away scoring rates
- venue-specific attack and defence ratios for the two teams
- exponential time decay

Not used: bookmaker odds, Tippmix, `PreMatchFeatures` last5/last10, xG, Elo, shots, injuries, lineups, or the target match’s own score.

## Time decay

```text
weight = exp(-ln(2) * ageDays / halfLifeDays)
```

A match exactly `halfLifeDays` old has weight `0.5`. `exp`/`ln` use deterministic `double`; lambdas and the published distribution are `BigDecimal`. Default half-life: **180 days** (an assumption, not an optimum).

## League baseline

Eligible training matches: same competition and `matchDate < targetDate` (same-day rows are excluded, matching Historical Feature Builder date-batching).

```text
leagueHomeGoalRate = weighted average home goals
leagueAwayGoalRate = weighted average away goals
```

No league history → `NO_LEAGUE_HISTORY`. v1 does not invent 1.4 / 1.1 defaults. A zero scoring rate on one side (degenerate sample) fails rather than dividing by zero.

## Attack / defence and λ

Venue-specific, relative to the league rates above:

```text
homeAttack  = home team’s weighted goals scored at HOME / leagueHomeGoalRate
homeDefence = home team’s weighted goals conceded at HOME / leagueAwayGoalRate
awayAttack  = away team’s weighted goals scored AWAY / leagueAwayGoalRate
awayDefence = away team’s weighted goals conceded AWAY / leagueHomeGoalRate

λ_home = leagueHomeGoalRate * homeAttack * awayDefence
λ_away = leagueAwayGoalRate * awayAttack * homeDefence
```

Previous seasons in the same competition **do** count; decay reduces old influence. Cross-league history does not.

## Insufficient history

`minimumTeamMatches` (default **5**) applies to venue-relevant samples:

- home team prior **home** matches
- away team prior **away** matches

Too few → `INSUFFICIENT_HISTORY` with a null distribution. Promoted/new clubs are skipped, not given league-mean strength.

## Score grid

Independent Poisson:

```text
P(X = k) = exp(-λ) λ^k / k!     k = 0..maxGoalsPerTeam
P(i, j)  = P_home(i) * P_away(j)
```

Default `maxGoalsPerTeam = 10` (121 scorelines). The truncated mass is **renormalized** so `ScoreProbabilityDistribution` sums to **exactly 1**. Captured mass before normalization is returned for diagnostics; for realistic λ it is extremely close to 1.

## Configuration

`ProbabilityModelConfig` (not strategy presets):

| Field | Default |
|---|---|
| `decayHalfLifeDays` | 180 |
| `maxGoalsPerTeam` | 10 |
| `minimumTeamMatches` | 5 |

## Known weaknesses

- Independent goals (no 0-0 / 1-1 dependence)
- Promoted teams often unavailable
- No transfer/manager/injury/xG/Elo information
- Exact source team names only (`Man United` ≠ `Manchester United`)
- Truncation at 10 goals
- Half-life and max goals are uncalibrated assumptions

v1 exists so later models can be compared against a clean, leak-free baseline.

## Evaluation helper

`ScoreProbabilityEvaluator.logLoss(distribution, actual)` is `-ln(P(actual))`. If the score is off the truncated grid (or has probability 0), the result is empty — not a fake log(0).

## Manual / API

None. In-memory only. No model tables, REST, or UI.

Walk-forward candidate generation and strategy comparison: [historical walk-forward evaluation](historical-walk-forward-evaluation.md).
