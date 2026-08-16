# Historical feature builder v1

SafeEdge turns imported `historical_match` facts into modelling rows for a future Probability Model. This is **not** a predictive model. It does not produce `ScoreProbabilityDistribution`, candidates, or backtests.

```text
historical_match facts
        ↓
HistoricalFeatureDatasetService (load, ordered)
        ↓
HistoricalFeatureBuilder (pure, point-in-time)
        ↓
HistoricalModelRow (PreMatchFeatures + MatchScore target)
```

## Pre-match vs target

`PreMatchFeatures` contains only information that was available **before** the match. The final score is `MatchScore target` on `HistoricalModelRow`. Home/away goals never live inside the feature object.

Processing for each date batch:

1. compute features for every match on that date from current history
2. emit modelling rows
3. **then** apply that date’s results to history

The current match cannot appear in its own features.

## Same-date batching

Many football-data rows are date-only (`kickoffUtc` is null). v1 does **not** order same-day matches by invented or unverified kickoff times.

Matches are grouped by `(competition, matchDate)`:

- all rows on that date see the same prior-date state
- none of them see each other’s results
- after every row is emitted, all results from that date update history

A match on the next date can see both. This is conservative look-ahead protection.

## Rolling windows

- **Last 5 / last 10**: the team’s most recent prior matches in this competition, any venue.
- **Home-only last 5**: the home team’s prior **home** fixtures only.
- **Away-only last 5**: the away team’s prior **away** fixtures only.

The current match is excluded. Future matches are excluded.

Partial windows average over the matches that exist. Three prior games → last5 is the average of those three, not padded to five zeros.

## Missing history

Zero prior matches → team averages are **null**, counts are `0`. Null means unknown. Zero would mean “historically scored zero”. v1 does not impute.

The same applies to league averages when the season has no prior matches yet.

## Season boundaries

Team rolling history **carries across seasons** in the same competition. Opening day of 2024/25 may use 2023/24 club form; that information was known.

League-context averages (`leagueHomeGoalsPerMatch`, `leagueAwayGoalsPerMatch`, `leagueTotalGoalsPerMatch`, `leagueMatchesObserved`) are **season-scoped**. They reset to `0` / `null` at the first match of a new season.

## Competition isolation

State is keyed by `canonical competition + exact source team name`. Premier League history never enters Bundesliga league averages or Bundesliga club windows.

## Team identity (v1 limitation)

Identity is the exact persisted source spelling. `"Man United"` does not match `"Manchester United"`. There is no Team / alias / fuzzy merge. Canonicalization is a later task (needed before ClubElo joins).

## Odds are not features

Bet365 / Pinnacle / market AH quotes are **not** in `PreMatchFeatures`. v1 is a football-performance feature set. Historical odds stay available later for candidate EV and backtests, not for teaching the first model to copy the book.

No z-scoring or dataset-wide normalization (that would leak future rows).

## Arithmetic

Averages use `BigDecimal` and `MathContext.DECIMAL128`. Counts are integers. Features are not persisted; they are derived in memory.

## Dataset range

`HistoricalFeatureDatasetService.buildDataset(competition, fromSeason, toSeason)` loads that competition’s matches whose `season_start_year` is in the inclusive range, then runs the builder. History cannot include matches that were not loaded. If 2014 opening-day form should include 2013, include 2013 in the range.

## Timeline example

```text
2024-01-01  A vs B  4-0
  A's last5 GF = null          (no prior matches)
  target = 4-0                 (label only)

2024-01-01  C vs D  1-0        (same date, date-only)
  league averages still null   (does not see A-B)

2024-01-08  A vs C
  A's last5 GF = 4             (from 1 Jan only)
  league home GPM = 2.5        (5+1)/2 from 1 Jan
```

## Dataset summary

`HistoricalFeatureDataset` reports row counts including `rowsWithFullLast5History`, `rowsWithFullLast10History`, and `rowsWithMissingTeamHistory`. There is no automatic “usable” threshold.

## Manual inspection

Does not download. Requires already-imported matches.

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-historical-features"
```

```text
SAFEEDGE_HISTORICAL_FEATURES_COMPETITION=PREMIER_LEAGUE
SAFEEDGE_HISTORICAL_FEATURES_FROM_SEASON=2023
SAFEEDGE_HISTORICAL_FEATURES_TO_SEASON=2023
```

Logs a summary and a small sample of rows.

## Out of scope

Poisson, Dixon-Coles, Elo, xG, ML, ClubElo, TeamAlias, CandidateEngine/BacktestRequest wiring, feature tables, UI.
