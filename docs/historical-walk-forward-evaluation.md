# Historical walk-forward evaluation v1

This is the first application layer that turns persisted football-data.co.uk matches and Asian Handicap quotes into point-in-time `BettingOpportunity` rows and runs `BacktestEngine`.

It is **not** a Tippmix historical backtest. The probability and strategy engines are source-agnostic; the prices in v1 are football-data.co.uk quotes (`PINNACLE`, `BET365`, `MARKET_MAX`, or `MARKET_AVERAGE`).

```text
historical_match + historical_ah_offer
        ↓
HistoricalWalkForwardDatasetBuilder  (prior dates only)
        ↓
FootballProbabilityModel  (v1 Poisson default, injected v2, or injected v3)
        ↓
ScoreProbabilityDistribution
        ↓
CandidateEngine  (HOME and AWAY, all EV signs)
        ↓
HistoricalBettingOpportunity + HistoricalEventResult
        ↓
BacktestEngine  (StrategyConfig)
        ↓
BacktestResult
```

No parameter search. No Elo, xG, or ML. One configured model per comparison. Probability Model v2 (regularized Dixon-Coles) and v3 (jointly fitted Dixon-Coles) are selected at orchestration time; CandidateEngine and BacktestEngine do not branch on model version.

## True walk-forward

For each target match M:

```text
training history = persisted matches in the same competition
                   with matchDate strictly before M.matchDate
```

Excluded:

- M’s own final score
- any same-date result (unknown kickoff order)
- any later date
- a single full-dataset Poisson fit reused for earlier matches
- bookmaker odds as model features

The builder groups matches by date. All evaluation targets on date D are predicted from history with `matchDate < D`. Only after every D prediction/candidate is finished are D’s results appended to training.

```text
2023-01-01  A vs B     trained on dates < 2023-01-01
2023-01-01  C vs D     trained on the same prior dates; does not see A vs B
2023-01-02  E vs F     may see both 2023-01-01 results
```

## Training vs evaluation range

Request fields (explicit reproducibility):

```text
trainingFromSeason <= evaluationFromSeason <= evaluationToSeason
```

Example: train from 2014, evaluate 2018–2023.

- Matches with season start year in `[trainingFromSeason, evaluationFromSeason)` are **warmup only**. They train later predictions. They do not generate candidates or bets.
- Matches with season start year in `[evaluationFromSeason, evaluationToSeason]` are evaluation targets.
- Reproducibility depends on which seasons are actually persisted. The request bounds are the contract; missing DB seasons silently reduce warmup.

v1 uses all eligible prior matches in that loaded window, with the Poisson exponential decay. There is no rolling fixed N-season window.

## Selected historical quote source

One `HistoricalQuoteSource` per run. No opportunistic fallback to another bookmaker or to market average.

If the configured source quote is absent, the match is skipped for candidate generation (`matchesSkippedMissingQuote`). The Poisson prediction may still count toward average actual-score log loss.

Never mix Tippmix live odds into these rows. Never fuzzy-join teams.

## HOME and AWAY candidates

A valid quote has `homeHandicapLine`, `homeOdds`, `awayOdds`.

| Side | Line | Odds |
|---|---|---|
| HOME | `homeHandicapLine` | `homeOdds` |
| AWAY | `negate(homeHandicapLine)` | `awayOdds` |

Both sides are evaluated independently by `CandidateEngine`. The builder does not assume favourite vs underdog, does not hedge, and does not drop negative EV. `StrategyEngine.minimumEdge` is the gate.

Identity is deterministic (not random, not Tippmix):

```text
eventId      = source:competition:matchDate:home:away:sourceRowNumber
marketId     = eventId:quoteSource:AH:homeLine
opportunityId = marketId:HOME | marketId:AWAY
provider     = FOOTBALL_DATA_UK
```

## Model prediction is independent of odds

`PoissonFootballProbabilityModel` sees identity, date, and prior scores only. Odds enter only at `CandidateEngine`. Missing history (`NO_LEAGUE_HISTORY`, `INSUFFICIENT_HISTORY`) yields no distribution, no fake 50/50, and no candidate.

Log-loss uses `ScoreProbabilityEvaluator`. Off-grid actual scores are counted separately, not treated as probability zero.

Tracked separately:

```text
predictionsAvailable
predictionsWithSelectedAhQuote
candidatesGenerated
```

## All candidate EV goes to StrategyEngine

The prepared stream includes positive, zero, and negative EV. Different `minimumEdge` values can be compared on the **same** candidate dataset. Do not pre-filter “value bets” in the builder.

Candidate generation does not read `StrategyConfig`. `HistoricalStrategyComparisonEngine` builds (or receives) the dataset once, then runs `BacktestEngine` per config.

## Synthetic ordering timestamps

football-data.co.uk AH rows are `PRE_MATCH_SNAPSHOT` with `observedAt = null`. Canonical `kickoffUtc` is typically null. v1 does **not** persist a fake observation time or kickoff.

`BacktestEngine` still requires `decisionAt < settlementAt` and non-decreasing `decisionAt`. The builder therefore assigns:

```text
decisionAt   = matchDate at 00:00 UTC
settlementAt = matchDate + 1 day at 00:00 UTC
```

These are **synthetic chronological ordering timestamps**. They are not kickoff times and not real odds observation times. Same-date matches share `decisionAt`; prediction isolation for that date is the builder’s date-batch, not implied intra-day order. BacktestEngine settles day D at D+1 00:00 before day D+1 decisions.

## Model quality vs betting quality

- Model: available predictions and average actual-score log loss, including matches with no AH quote.
- Betting: candidates, strategy accepts, ROI, drawdown.

Do not treat a profitable backtest as proof the Poisson model is calibrated, or a losing backtest as proof it is useless.

## No optimization on the evaluation window

Do not search half-life, `minimumTeamMatches`, Kelly fraction, `minimumEdge`, stake caps, or Vault using the same period later reported as the final evaluation. Training / validation / test discipline is a later task. v1 is a baseline observation.

## Manual runner

Opt-in. No download. No normal-startup execution.

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-historical-backtest"
```

Required:

```text
SAFEEDGE_HISTORICAL_BACKTEST_COMPETITION=PREMIER_LEAGUE
SAFEEDGE_HISTORICAL_BACKTEST_TRAINING_FROM_SEASON=2014
SAFEEDGE_HISTORICAL_BACKTEST_FROM_SEASON=2018
SAFEEDGE_HISTORICAL_BACKTEST_TO_SEASON=2023
SAFEEDGE_HISTORICAL_BACKTEST_QUOTE_SOURCE=PINNACLE
```

Optional:

```text
SAFEEDGE_HISTORICAL_BACKTEST_STARTING_BANKROLL=100000
SAFEEDGE_HISTORICAL_BACKTEST_MAX_ACCEPTED_BETS=
```

Starting bankroll is simulation capital only. `maxAcceptedBets` keeps existing chronological first-N semantics.

Default comparison uses `StrategyPresetFactory` data for DEFENSIVE, BALANCED, GROWTH, and FLAT_STAKE. The engines remain config-driven. The runner does not declare a winner.

## Baseline diagnostics

Opt-in autopsy of one already-prepared walk-forward dataset and the four strategy backtests. It does not retune Poisson, StrategyConfig, or quote source. Same `SAFEEDGE_HISTORICAL_BACKTEST_*` properties as the evaluation runner.

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-baseline-diagnostics"
```

Writes `docs/results/baseline-001-diagnostics.md`. Unit-stake tables are diagnostic only.

## Baseline 002 edge quality

Opt-in ranking / settlement-calibration autopsy of the same prepared walk-forward candidates. Same `SAFEEDGE_HISTORICAL_BACKTEST_*` properties. Does not retune Poisson or StrategyConfig.

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-baseline-002"
```

Writes `docs/results/baseline-002-edge-quality.md`.

## Baseline 003 Bundesliga replication

Zero-tuning replication of Baseline 001/002 on `BUNDESLIGA` with the same Poisson defaults, quote source, and evaluation window. Writes Bundesliga diagnostics and a cross-league comparison against the published Premier League Baseline 001/002 numbers. Does not retune the model or select a league to bet.

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-baseline-003"
```

Requires `SAFEEDGE_HISTORICAL_BACKTEST_COMPETITION=BUNDESLIGA` and the same `TRAINING_FROM_SEASON=2014`, `FROM_SEASON=2019`, `TO_SEASON=2023`, `QUOTE_SOURCE=MARKET_AVERAGE` properties. Writes `docs/results/baseline-003-bundesliga.md` and `docs/results/baseline-003-cross-league-validation.md`.

## Baseline 004 Serie A replication

Zero-tuning replication of Baseline 001/002 on `SERIE_A` with the same Poisson defaults, quote source, and evaluation window. Writes Serie A diagnostics and a three-league comparison against the published Premier League and Bundesliga numbers. Does not retune the model, overwrite Baseline 001–003 reports, or select a league to bet.

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-baseline-004"
```

Requires `SAFEEDGE_HISTORICAL_BACKTEST_COMPETITION=SERIE_A` and the same `TRAINING_FROM_SEASON=2014`, `FROM_SEASON=2019`, `TO_SEASON=2023`, `QUOTE_SOURCE=MARKET_AVERAGE` properties. Writes `docs/results/baseline-004-serie-a.md` and `docs/results/baseline-004-three-league-validation.md`.

## Probability Model v2 development evaluation

Compares frozen v1 Poisson against `RegularizedDixonColesFootballProbabilityModel` on Premier League, Bundesliga, and Serie A only. Same window and `MARKET_AVERAGE` quotes. Does not overwrite Baseline 001–004. Does not run La Liga or Ligue 1.

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-probability-model-v2"
```

Writes `docs/results/probability-model-v2-development.md`. Shrinkage default and ρ fitting were declared before the run; they are not ROI-tuned. Success is ranking and calibration, not ROI.

## Probability Model v3 development evaluation

Compares frozen v1 Poisson and v2 regularized Dixon-Coles against `JointDixonColesFootballProbabilityModel` on Premier League, Bundesliga, and Serie A only. Same window and `MARKET_AVERAGE` quotes. One frozen v3 config across all three leagues. Does not overwrite Baseline 001–004 or the v2 development report. Does not run La Liga or Ligue 1.

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-probability-model-v3"
```

Writes `docs/results/probability-model-v3-development.md`. Regularization, identifiability, and ρ parameterization were declared before the run; they are not ROI-tuned. Success is ranking and calibration versus the better of v1/v2, not ROI.

## Limitations

- Date-only source: no real pre-match observation time; synthetic UTC date order only.
- One competition per run; the probability model is league-specific.
- Exact source team spellings; no TeamAlias / ClubElo / Tippmix join.
- Default builder still uses independent Poisson v1. Dixon-Coles v2 and jointly fitted Dixon-Coles v3 are selected at orchestration.
- In-memory only; no evaluation persistence, REST, or UI.
- Complexity is roughly O(N²) model scans over loaded matches. Acceptable for a manual run; no prediction-state cache (a cache would risk leakage).

## Out of scope

Parameter search, Monte Carlo, ML, Elo, xG, Tippmix historical odds, automatic “best strategy” selection, evaluation tables, API, Angular. La Liga / Ligue 1 remain reserved validation for probability-model work.
