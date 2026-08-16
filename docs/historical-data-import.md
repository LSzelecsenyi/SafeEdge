# Historical football data import v1

SafeEdge's v1 historical source is **football-data.co.uk**: static, no-key CSV files with Big 5 league results and Asian Handicap pre-match odds. This is modelling/backtest training data. It is **not** Tippmix live pricing and must never be labelled as Tippmix odds.

```text
football-data.co.uk CSV
        ↓
FootballData client / parser / DTO / mapper
        ↓
SafeEdge historical domain
        ↓
historical_match + historical_ah_offer
```

Column names such as `AHh` and `B365AHH` stay inside `historical/footballdata`. CandidateEngine, SettlementEngine, StrategyEngine, and BacktestEngine never see them.

## Why not Tippmix tables

`betting_event` / `odds_snapshot` assume a live bookmaker event with a UTC kickoff and append-only observation history. football-data rows are often date-only, use source team spellings, and are static files that should upsert. Reusing Tippmix tables would invent kickoffs and conflate sources. v1 uses a separate historical boundary.

## Big 5 scope

| Canonical competition | football-data code |
|---|---|
| PREMIER_LEAGUE | E0 |
| BUNDESLIGA | D1 |
| LA_LIGA | SP1 |
| SERIE_A | I1 |
| LIGUE_1 | F1 |

Season `2023/24` is `FootballSeason(2023, 2024)`. The adapter turns that into the source path segment `2324`. URL fragments are not scattered through the app.

CSV path: `{baseUrl}/mmz4281/{seasonCode}/{leagueCode}.csv`  
Default base URL: `https://www.football-data.co.uk`

## AH line convention

football-data.co.uk handicap size is from the **home team** perspective (`AHh`, `B365AH`, `PAH`).

SafeEdge `BettingMarket.line` is also the **home-side** handicap. There is no sign inversion:

```text
source AHh / B365AH = -1.25
  → canonical HOME = -1.25
  → canonical AWAY = +1.25
```

Home winning by one goal then settles as HOME `HALF_LOSS`, AWAY `HALF_WIN` via the existing `SettlementEngine`. Quarter lines stay exact (`-0.25`, not `-0`).

## Quote sources and columns

Pairings live only in `FootballDataAhQuoteMapping`:

| Quote source | Line | Home odds | Away odds |
|---|---|---|---|
| BET365 | B365AH | B365AHH | B365AHA |
| PINNACLE | PAH | PAHH | PAHA |
| MARKET_MAX | AHh | MaxAHH | MaxAHA |
| MARKET_AVERAGE | AHh | AvgAHH | AvgAHA |

`notes.txt` lists `PAHH`/`PAHA` but not `PAH`. Pinnacle quotes are imported only when `PAH` is present. There is no fallback that pairs Pinnacle odds with `AHh`.

Closing-odds columns (`B365CAHH`, etc.) are not imported. Observation type is `PRE_MATCH_SNAPSHOT`. `observedAt` is null; v1 does not invent `kickoff - 1h` or call these opening/closing.

## Missing and incomplete data

- A match with a valid score and no AH columns is still imported.
- Incomplete triples (line or one side of odds missing) skip that quote only (`quotesSkippedIncomplete`).
- Non-numeric or `<= 1` odds skip the quote (`quotesSkippedInvalidOdds`).
- Lines that are not a multiple of 0.25 skip the quote (`quotesSkippedInvalidLine`). No rounding, no 1X2 fallback, no interpolation.

Required CSV headers: `Div`, `Date`, `HomeTeam`, `AwayTeam`, `FTHG`, `FTAG`. `Time` is optional. Unknown extra columns are ignored. `FTR` is not used; final score is canonical.

## Time

`matchDate` is stored. Source `Time` is kept as `sourceKickoffTime` when it parses. Canonical `kickoffUtc` stays **null** because football-data times have no proven timezone. v1 does not invent noon or midnight.

## Identity and idempotency

Match key: source + canonical competition + season years + match date + exact source home/away names.

Quote key: historical match + quote source + `PRE_MATCH_SNAPSHOT` (one quote per bookmaker/aggregate per match). Line is not part of identity so a CSV correction updates the row.

Re-importing the same file does not duplicate. Changed scores or odds update in place and log WARN. This is intentionally different from live Tippmix `odds_snapshot` append-only history.

Raw provenance: `source_file`, `source_row_number`, quote column names, and raw line/odds strings.

## Teams

Source names are stored exactly (`Man United`). There is no Team / alias crosswalk yet.

## Manual import

Default is off. No scheduler. One league + one season only:

```text
.\gradlew.bat bootRun -PspringProfiles=local,manual-historical-import
```

```text
SAFEEDGE_HISTORICAL_LEAGUE=PREMIER_LEAGUE
SAFEEDGE_HISTORICAL_SEASON_START=2023
```

`safeedge.providers.football-data.base-url` is configurable. No credentials.

## Coverage before bulk import

Do **not** assume uniform AH coverage. Before importing 10–15 seasons × Big 5, compute per league/season/quote source:

- matches
- matchesWithBet365AH
- matchesWithPinnacleAH
- matchesWithMarketAggregateAH
- coveragePercentage

v1 does not implement that report.

## Out of scope

Probability models, Dixon-Coles, ClubElo, StatsBomb, Understat, feature engineering, 1X2/totals persistence, BacktestRequest builder, UI.
