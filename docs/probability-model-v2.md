# Probability Model v2 — regularized Dixon-Coles

SafeEdge v2 estimates a **full-time score probability distribution** from historical match results known **before** the target date. It is a football scoring model, not a betting strategy and not a claim of profitability.

Frozen baseline: [Probability Model v1](probability-model-v1.md) (`PoissonFootballProbabilityModel`). v1 semantics are not changed.

```text
historical matches (date < target)
        ↓
RegularizedDixonColesFootballProbabilityModel
        ↓
    ScoreProbabilityDistribution  (sums to 1)
        ↓
CandidateEngine
```

Both v1 and v2 implement `FootballProbabilityModel`. Walk-forward orchestration selects the model. CandidateEngine and BacktestEngine do not branch on model version.

## What is fitted

Only prior results in the **same canonical competition**:

- weighted league home/away scoring rates
- venue-specific attack and defence strengths, shrunk toward league average
- Dixon-Coles ρ from weighted score log-likelihood
- exponential time decay (same formula as v1)

Not used: bookmaker odds, Tippmix, candidate edge, ROI, `PreMatchFeatures`, xG, Elo, shots, injuries, lineups, or the target match’s own score.

## Shrinkage

`attackDefenceShrinkageStrength` is **weighted league-average pseudo-match exposure**, using the same exponential time weights as team history (`Σ timeWeight`), not raw match counts.

Default **5**. Same order of magnitude as `minimumTeamMatches`. Chosen before evaluation; not fitted to ROI.

```text
shrunkRate = (weightedTeamGoals + prior * leagueRate)
           / (weightedTeamExposure + prior)
shrunkStrength = shrunkRate / leagueRate
```

`prior = 0` reproduces the unregularized v1 ratio (Dixon-Coles can still differ). There is no hard clamp such as `[0.5, 1.5]`.

Same formula for home/away attack and defence. League rates remain point-in-time weighted averages of the same competition.

## Expected goals

```text
λ_home = leagueHomeRate * shrunkHomeAttack * shrunkAwayDefence
λ_away = leagueAwayRate * shrunkAwayAttack * shrunkHomeDefence
```

## Dixon-Coles

Correction applies only to 0-0, 0-1, 1-0, and 1-1:

```text
τ(0,0) = 1 − λμρ
τ(0,1) = 1 + λρ
τ(1,0) = 1 + μρ
τ(1,1) = 1 − ρ
τ(x,y) = 1 otherwise
```

Every used τ must be strictly positive. Invalid ρ is rejected; probabilities are never silently clamped negative. After τ, the finite grid is renormalized with the same `ScoreGridNormalizer` as v1.

ρ is fitted walk-forward by deterministic golden-section maximization of **weighted score log-likelihood** on matches with `matchDate < targetDate`. ρ = 0 is always a candidate. Search is bounded to a τ-valid interval inside `[-0.5, 0.5]`. Lambdas for that fit use as-of-T shrunk strengths (not a nested walk-forward per historical match). Market odds never enter.

## Insufficient history

Same `minimumTeamMatches` venue counts as v1 (default 5). Shrinkage is not permission to predict teams with zero meaningful history. Too few → `INSUFFICIENT_HISTORY`, null distribution.

## Configuration

`ProbabilityModelV2Config`:

| Field | Default | Meaning |
|---|---|---|
| `decayHalfLifeDays` | 180 | same as v1 |
| `maxGoalsPerTeam` | 10 | same as v1 |
| `minimumTeamMatches` | 5 | same as v1 |
| `attackDefenceShrinkageStrength` | 5 | weighted pseudo-matches toward league rate |
| `dixonColesEnabled` | true | fit ρ and apply τ |

These are hypotheses, not optima.

## Development vs validation leagues

Model development and v2 diagnostics use only:

- Premier League
- Bundesliga
- Serie A

**La Liga and Ligue 1 are reserved later validation.** Do not run them, import diagnostics for them, or tune v2 against them.

## Evaluation

Primary: edge Spearman/Pearson, edge-decile monotonicity, high-edge P(WIN)/P(LOSS) calibration, score log loss, goal/1X2 calibration, confidence compression.

ROI is secondary. Do not fit shrinkage or ρ to betting ROI.

Manual runner: `manual-probability-model-v2` writes `docs/results/probability-model-v2-development.md`. It does not overwrite Baseline 001–004.
