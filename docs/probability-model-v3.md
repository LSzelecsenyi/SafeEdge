# Probability Model v3 — jointly fitted regularized Dixon-Coles

SafeEdge v3 estimates a **full-time score probability distribution** from historical match results known **before** the target date. It jointly fits latent team attack and defence strengths, a league intercept, home advantage, and Dixon-Coles ρ.

Frozen baselines:

- [Probability Model v1](probability-model-v1.md) (`PoissonFootballProbabilityModel`)
- [Probability Model v2](probability-model-v2.md) (`RegularizedDixonColesFootballProbabilityModel`)

v1 and v2 semantics are not changed. v3 is a new `FootballProbabilityModel`. CandidateEngine and BacktestEngine do not branch on model version.

```text
historical matches (date < target)
        ↓
JointDixonColesFootballProbabilityModel
        ↓
    ScoreProbabilityDistribution  (sums to 1)
        ↓
CandidateEngine
```

## Why v3 exists

v1 and v2 estimate attack/defence mostly as venue-specific historical ratios against league averages. Opponent strength is not in the fit. The hypothesis is that a proper joint team-strength model will improve **AH edge ranking and high-edge calibration**, not merely score log-loss.

## Mathematical model

Defence convention: **positive defence is stronger** (concedes fewer). It is subtracted on the log-link.

```text
log λ_home = intercept + homeAdvantage + attack(home) − defence(away)
log λ_away = intercept + attack(away) − defence(home)
ρ = rhoScale * tanh(z)
```

Properties:

- stronger attack → higher expected goals
- stronger defence → lower opponent expected goals
- home advantage → higher home expected goals

This is not the v1/v2 ratio-strength formula.

## Likelihood

For training match m, with time weight `w_m`:

```text
homeGoals ~ Poisson(λ_home)
awayGoals ~ Poisson(λ_away)
```

plus Dixon-Coles τ on 0-0 / 0-1 / 1-0 / 1-1.

```text
objective = Σ_m w_m (log Pois(y_h|λ_h) + log Pois(y_a|λ_a) + log τ)
          − λ_att Σ_i attack_i²
          − λ_def Σ_i defence_i²
```

Time weight:

```text
w = exp(−ln(2) * ageDays / decayHalfLifeDays)
```

Eligible matches: same competition and `matchDate < targetDate`. Same-day and future results never enter. Bookmaker odds, AH line, candidate edge, and ROI never enter.

## Identifiability

Joint attack/defence models are not identifiable without a constraint. After every optimizer step:

```text
Σ attack_i = 0
Σ defence_i = 0
```

Attack/defence gradients are projected to mean zero. This is deterministic centering, not an optimizer accident.

## Regularization

L2 / ridge on centered team parameters only. Intercept and home advantage are unpenalized.

Default `λ_att = λ_def = 5`. Scale: with ~20 teams and RMS(θ)≈0.2, `Σθ²≈0.8`, so the penalty is a few units versus a weighted log-likelihood of thousands. Implied Gaussian prior `σ ≈ 1/√(2λ) ≈ 0.32` on the log-rate.

These defaults are modeling assumptions declared **before** evaluation. They are not ROI-fitted optima. Do not grid-search them against AH ROI.

## Dixon-Coles ρ

ρ is jointly optimized with team parameters from **score likelihood only**.

```text
ρ = 0.4 * tanh(z)
```

so `|ρ| < 0.4`. If a trial step produces an invalid τ region, that step is rejected (`−∞` objective). Invalid ρ never silently yields negative probabilities. If the start is invalid or the best finite point cannot produce a valid τ, the prediction is `FITTING_FAILED`. There is no silent fallback to v1 or v2.

## Optimizer

Deterministic Barzilai–Borwein gradient ascent with Armijo backtracking.

Initialization (or mapped from an earlier-cutoff warm-start):

- `intercept = log(leagueAwayRate)`
- `homeAdvantage = log(leagueHomeRate) − intercept`
- attack = 0, defence = 0, z = 0 (ρ = 0)

Same input → same fitted parameters and score distribution. Non-convergence still publishes the best finite likelihood point. Only a non-finite / invalid objective is `FITTING_FAILED`.

Warm-start uses a previous fit only when that fit’s cutoff is **strictly before** the target date, and only as initial values. Each date still refits. Same-date targets share one fit because they share identical training.

## Cold start

Both target teams need at least `minimumTeamMatches` **any-venue** prior matches, and the league needs `minimumLeagueMatches` eligible prior matches.

Unseen or under-exposed teams → `INSUFFICIENT_HISTORY`, null distribution. No fake 50/50. No silent league-average substitution for a missing team.

## Score grid

Fitted λ_home / λ_away plus Dixon-Coles τ, then the same `ScoreGridNormalizer` as v1/v2. Probabilities in `[0, 1]` and sum to 1. `maxGoalsPerTeam` is unchanged.

## Frozen development config

`ProbabilityModelV3Config.defaults()`:

| Field | Default |
|---|---|
| `decayHalfLifeDays` | 180 |
| `maxGoalsPerTeam` | 10 |
| `minimumTeamMatches` | 5 (any venue) |
| `minimumLeagueMatches` | 20 |
| `attackRegularization` | 5.0 |
| `defenceRegularization` | 5.0 |
| `optimizerMaxIterations` | 80 |
| `gradientTolerance` | 1e-5 |
| `rhoScale` | 0.4 |

One frozen config across Premier League, Bundesliga, and Serie A. Do not retune between leagues.

## Predeclared success gates

Compare v3 to the **better of v1/v2**. Positive ROI is ignored.

**`MODEL_V3_CLEAR_IMPROVEMENT`** only if all of:

1. Spearman improvement ≥ +0.05 versus the better of v1/v2 in at least 2 of 3 leagues
2. Spearman not worse by ≥ 0.02 in the third
3. ≥ 3 percentage-point shrink in the ≥10% edge WIN **and** LOSS absolute calibration gaps versus the better of v1/v2 in at least 2 of 3 leagues
4. score log loss not worse by > 0.02 versus the better of v1/v2 in any league

**`MODEL_V3_PARTIAL_IMPROVEMENT`:** log-loss gate holds, and Spearman +≥0.02 in ≥2 leagues **or** WIN+LOSS gap shrink in ≥2, and Spearman not worse by ≥0.02 in more than one league.

**`MODEL_V3_REGRESSION`:** Spearman worse by ≥0.02 in ≥2 leagues, **or** log loss worse by >0.02 in ≥2, **or** ≥10% WIN gaps worsen by ≥0.03 in ≥2, without CLEAR/PARTIAL offset.

**`MODEL_V3_NO_MEANINGFUL_IMPROVEMENT`:** otherwise.

These gates are frozen before the live three-league run.

## Development vs validation

Development: Premier League, Bundesliga, Serie A.

Reserved validation: La Liga, Ligue 1. Do not run, inspect, or tune on them. The v3 development runner refuses those competitions.

## Manual run

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-probability-model-v3"
```

Writes `docs/results/probability-model-v3-development.md`. Does not overwrite Baseline 001–004 or the v2 development report. Quote source remains `MARKET_AVERAGE` (football-data.co.uk, not Tippmix).

Live development evaluation (frozen gates, one config, no reserved leagues): [probability-model-v3-development.md](results/probability-model-v3-development.md). Classification: `MODEL_V3_NO_MEANINGFUL_IMPROVEMENT`.
