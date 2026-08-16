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

## Manual import (one league, one season)

Default is off. No scheduler.

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-historical-import"
```

```text
SAFEEDGE_HISTORICAL_LEAGUE=PREMIER_LEAGUE
SAFEEDGE_HISTORICAL_SEASON_START=2023
```

`safeedge.providers.football-data.base-url` is configurable. No credentials.

## Bulk import

`FootballDataBulkHistoricalImportManager.importRange(competitions, startSeason, endSeason)` calls the existing single-season importer once per pair. It does not fetch, parse, or persist itself.

Season range is inclusive. `startSeason=2010`, `endSeason=2024` means `2010/11` … `2024/25`. Core logic does not default a global window such as 2010–2024.

Execution is **sequential** (competition enum order, then seasons ascending). No parallel HTTP. One failed file is recorded (`SOURCE_NOT_FOUND`, `FETCH_FAILED`, `PARSE_FAILED`, or `IMPORT_FAILED`) and the remaining pairs continue. The bulk run is not one transaction; each season import keeps its own persistence behavior. Re-running the same range does not duplicate rows.

Opt-in only. Normal `.\gradlew.bat bootRun` does not download historical files.

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-historical-bulk-import"
```

```text
SAFEEDGE_HISTORICAL_BULK_START_SEASON=2023
SAFEEDGE_HISTORICAL_BULK_END_SEASON=2023
SAFEEDGE_HISTORICAL_BULK_LEAGUES=PREMIER_LEAGUE
```

If `SAFEEDGE_HISTORICAL_BULK_LEAGUES` is omitted, all Big 5 competitions are imported. Start/end season env vars are required; missing them skips the run.

Suggested small smoke test (user-executed, not automatic): Premier League `2023/24` only, then optionally Big 5 for `2023/24` by omitting the leagues variable and keeping start=end=2023.

## AH coverage audit

`HistoricalAhCoverageService` is read-only over persisted `historical_match` and valid `historical_ah_offer` rows. It does not fetch CSVs or inspect raw columns. Quotes skipped at import (incomplete, invalid odds, invalid line) count as missing coverage.

For each persisted competition + season, and for each quote source (`BET365`, `PINNACLE`, `MARKET_MAX`, `MARKET_AVERAGE`):

```text
coverageRate = matchesWithQuote / totalMatches
```

- `totalMatches` is the historical match count for that league-season (stable denominator across sources).
- `matchesWithQuote` is distinct matches with at least one valid two-sided quote for that source.
- `totalMatches == 0` → `coverageRate = 0` (no division error). League-seasons with no persisted matches are omitted from the report.
- `ANY` coverage is distinct matches with at least one supported quote source — never the sum of per-source counts.
- `bestQuoteSource` is the source with the highest coverage rate. Ties use `HistoricalQuoteSource` enum order (`BET365`, then `PINNACLE`, `MARKET_MAX`, `MARKET_AVERAGE`). If every source has zero quotes, `bestQuoteSource` is null.

There is **no** automatic “usable season” threshold. Coverage is data for a later modelling task to choose a window. Do not assume uniform AH availability across leagues, seasons, or bookmakers.

Coverage-only (no download):

```text
.\gradlew.bat bootRun "-PspringProfiles=local,manual-historical-coverage"
```

Example log shape (**example only**, not observed Premier League data):

```text
PREMIER_LEAGUE 2023/24
  matches: 10
  BET365:         8 / 10 = 80.00%
  PINNACLE:       6 / 10 = 60.00%
  MARKET_MAX:     4 / 10 = 40.00%
  MARKET_AVERAGE: 0 / 10 = 0.00%
  ANY:            8 / 10 = 80.00%
```

If those 6 Pinnacle and 4 Market Max matches sit inside the 8 Bet365 matches, ANY is `8/10`, not `18/10`.

## Out of scope

Probability models, Dixon-Coles, ClubElo, StatsBomb, Understat, 1X2/totals persistence, BacktestRequest builder, UI, TeamAlias, coverage REST API, historical scheduler.

Feature rows are built separately; see [historical-features.md](historical-features.md).
