# Rich historical football data research

**Status:** research only. No importer, no Probability Model v4, no dataset download.

**Date:** 2026-08-16.

**Question:** what historical football data can SafeEdge realistically obtain that is **not already represented by final scores and simple team identity**, and that can be used in **strict point-in-time walk-forward** evaluation?

This note follows Probability Model v3 (`MODEL_V3_NO_MEANINGFUL_IMPROVEMENT`). v1→v2→v3 improved score log loss (~3.050 → 3.010 → 2.988) and did **not** improve Spearman(predicted AH edge, realized unit return), which stayed ≈ 0 on Premier League, Bundesliga, and Serie A. Further manipulation of full-time scores alone is currently low-value.

La Liga and Ligue 1 remain reserved validation leagues. Coverage metadata for those leagues is noted below. Their match results were **not** inspected for model development.

This is not legal advice. Licensing notes are observations from public pages.

## SafeEdge baseline (what we already have)

Current historical pipeline:

```text
football-data.co.uk CSV
        ↓
historical_match + historical_ah_offer
        ↓
point-in-time features / probability models
        ↓
CandidateEngine → AH expected return
        ↓
StrategyEngine / BacktestEngine / diagnostics
```

Already imported:

- final score (`FTHG`/`FTAG`)
- date (often date-only; `kickoffUtc` is null)
- exact source team spellings (`Man United`)
- one pre-match AH snapshot per quote source (`BET365`, `PINNACLE`, `MARKET_MAX`, `MARKET_AVERAGE`)

Already **in the CSVs but not imported** (see [historical-data-import.md](../historical-data-import.md)):

- match statistics: `HS`/`AS`/`HST`/`AST` (shots / shots on target), plus corners, fouls, cards
- closing-odds columns (`B365CH`, `AHCh`, `B365CAHH`, `AvgCAHH`, …) since **2019/20**
- no xG, no Elo, no lineups, no injuries

Identity today: canonical competition + season + match date + exact home/away source names. **No TeamAlias.** Features are already point-in-time: prior dates only, same-day isolation ([historical-features.md](../historical-features.md)).

Development window used by Baseline 002–004 / v2 / v3:

- warmup: 2014/15–2018/19
- evaluation: 2019/20–2023/24
- quote source: `MARKET_AVERAGE` (not Tippmix)

Approximate match counts if a source covers that window continuously:

| League | Matches / season | 10 seasons (2014/15–2023/24) |
|---|---|---|
| Premier League | 380 | ~3,800 |
| Bundesliga | 306 | ~3,060 |
| Serie A | 380 | ~3,800 |
| **Total** | | **~10,660** |

A source that starts in 2014/15 or 2015/16 is still usable: 2019–2023 evaluation would have several prior seasons of the new signal.

## Point-in-time rule (non-negotiable)

Post-match xG / shots / Elo updates for match *D* must **not** enter features for match *D*.

They may enter features for matches with `matchDate > D` (and not same-day date-only siblings), exactly as final scores do today.

Current-match xG as a “feature” for that match is look-ahead. It is forbidden.

---

## Comparison table

Access legend: **CSV** = documented downloadable files; **CSV API** = documented HTTP CSV; **Git JSON** = versioned public repository; **unofficial JSON-in-HTML** = not an official API; **HTML scrape** = page scraping; **paid API** = commercial.

Join: vs football-data.co.uk exact source names, **without** a TeamAlias table.

| Source | xG | Shots | Elo | AH market | History | PL | BL | SA | Access | Join | SafeEdge value |
|---|---|---|---|---|---|---|---|---|---|---|---|
| football-data.co.uk unused columns | no | yes | no | opening+closing from 19/20 | 2000/01 stats; 19/20 closing | yes | yes | yes | CSV (already fetched) | EASY | HIGH (cheap, owned) |
| Understat | yes | shot-level | no | no | 2014/15→ | yes | yes | yes | unofficial JSON-in-HTML | MODERATE | HIGH |
| ClubElo | no | no | yes | no | ~1939→ | yes | yes | yes | documented CSV API | HARD | MEDIUM |
| FBref | yes from ~2017/18 | yes | no | no | xG ~2017/18→ | yes | yes | yes | HTML scrape; hostile ToS | MODERATE | LOW |
| StatsBomb Open Data | yes (event) | yes | no | no | **sparse** Big 5 | 03/04, 15/16 only | 15/16, 23/24 | 86/87, 15/16 | Git JSON + User Agreement | MODERATE | LOW for this experiment |
| Kaggle / GitHub Understat mirrors | yes | often | no | no | frozen scrape | yes if complete | yes if complete | yes if complete | third-party dump | MODERATE | LOW |
| Oddsportal / Oddsbase / sgodds | no | no | no | some opening/closing | uncertain / commercial | uncertain | uncertain | uncertain | scrape or paid | HARD | LOW (free) |
| StatsBomb / Opta paid APIs | yes | yes | no | no | continuous if licensed | yes | yes | yes | paid API | EASY–MODERATE | listed, not chosen |

---

## football-data.co.uk unused columns

1. **Source:** football-data.co.uk (already SafeEdge’s historical source).
2. **URL:** https://www.football-data.co.uk — key: https://www.football-data.co.uk/notes.txt
3. **Access:** documented CSV download. SafeEdge already fetches these files.
4. **Cost:** free.
5. **Auth:** none.
6. **History:** match stats for major leagues from **2000/01**; closing odds from **2019/20** (earlier seasons: one pre-closing set). Pinnacle 1X2 closing back to 2012/13.
7. **Leagues:** PL / BL / SA (and reserved La Liga / Ligue 1) — same files as today.
8. **Fields:** `HS`/`AS`/`HST`/`AST` (shots / SOT); corners, fouls, cards; 1X2 and AH **opening** (current import) and **closing** (`C` suffix: `B365CAHH`, `AvgCAHH`, `AHCh`, …). No xG. No Elo. No lineups. No injuries. No timestamps on odds.
9. **Granularity:** match-level (two team totals).
10. **IDs:** no stable match/team IDs; date + team names.
11. **Team naming:** **none** — already joined.
12. **Point-in-time:** shots/SOT are **post-match facts**. Usable as prior-match form only, same as scores. Closing AH is a **market observation of the same match**, not a football-performance feature; using closing price to *decide* a bet at an earlier time would be leakage. Using prior matches’ closing lines as features is a different, later experiment.
13. **Reproducibility:** high. Files are static and already versioned by season path.
14. **Licensing:** public free CSVs; site exists to support betting-system analysis. Not legal advice.
15. **Reliability:** low operational risk (already in production import). Column presence varies by season; incomplete triples must skip, as today. Pinnacle odds quality warning from **2025-07-23** (public API stale); MARKET_AVERAGE construction changed. Historical 2014–2023 files already on disk are the relevant snapshot.
16. **Usefulness:** **HIGH** as a *cheap control*, not as a substitute for xG. Shot volume is new vs goals. It is not shot quality.

SafeEdge currently ignores extra CSV columns. Importing `HS`/`HST` would not require a new provider.

---

## Understat

1. **Source:** Understat.
2. **URL:** https://understat.com/
3. **Access:** **no official public API** (no documented `/v1`, no API key). Pages embed JSON (`datesData`, `teamsData`, `playersData`, match `shotsData`). Third-party wrappers (`understatAPI`, `soccerdata.Understat`) scrape that HTML/JSON. Those wrappers are **not** an official API.
4. **Cost:** website is free to browse. No published bulk-download product.
5. **Auth:** none for public pages.
6. **History:** **2014/15 → current** (verified by site copy and multiple independent wrappers). Aligns with SafeEdge’s 2014–2023 window.
7. **Leagues:** EPL, Bundesliga, Serie A, La Liga, Ligue 1, RFPL. Reserved-league **coverage exists**; do not evaluate v4 on them.
8. **Fields:** match-level home/away xG (and often npxG, expected points, PPDA, deep completions); shot-level xG with coordinates, situation, body part, result; player xG/xA. No bookmaker odds. No Elo. Lineups/rosters on match pages. Injuries: not a first-class historical injury feed.
9. **Granularity:** match, team-match, player-match, shot.
10. **IDs:** numeric `match` / `team` / `player` IDs; datetime on schedule (timezone not independently verified — treat as source clock, not SafeEdge `Clock`).
11. **Team naming:** full names (`Manchester United`) vs football-data (`Man United`). Codes exist (`MUN`). Join is **MODERATE** with an explicit alias table; **HARD** if silent string equality is required.
12. **Point-in-time:** **yes**, if only matches with `matchDate < targetDate` contribute rolling xG. Same-day isolation should follow SafeEdge’s date-only rule. Do not use that match’s own xG.
13. **Reproducibility:** medium. Need a **frozen local snapshot** of fetched JSON. Live site can change encoding, layout, or availability. Kaggle mirrors are worse (stale, unclear license).
14. **Licensing:** no StatsBomb-style open-data agreement found. Terms are not a documented research license. Unofficial extraction sits in a grey area. Not legal advice.
15. **Reliability:** unofficial endpoint / HTML JSON can break; rate limits possible; no vendor SLA. Less bot-hostile than FBref in community reports — **uncertain**, do not treat as guaranteed.
16. **Usefulness:** **HIGH** — only realistic **free** source with continuous PL/BL/SA **xG from 2014/15**, match IDs, and shot-level optional later.

This is the best xG candidate for the *current* experiment **if** SafeEdge accepts unofficial HTML JSON plus a frozen archive. It is not “an API”.

---

## StatsBomb Open Data

1. **Source:** StatsBomb Open Data (Hudl).
2. **URL:** https://github.com/statsbomb/open-data — index `data/competitions.json` (fetched 2026-08-16). Package: https://github.com/statsbomb/statsbombpy. Paid API is separate.
3. **Access:** Git repository of JSON (competitions / matches / events / lineups / optional 360). **Official open dump**, not scraping. Paid StatsBomb API is authenticated and commercial (`statsbombpy` says API access is for paying customers only).
4. **Cost:** open subset free; full leagues paid.
5. **Auth:** none for GitHub JSON.
6–7. **Open coverage (verified from `competitions.json`, 2026-08-16):**

| Competition | Open seasons (male top flight) |
|---|---|
| Premier League | **2003/04, 2015/16 only** |
| 1. Bundesliga | **2015/16, 2023/24 only** |
| Serie A | **1986/87, 2015/16 only** |
| La Liga | many seasons 2004/05–2020/21 (plus 1973/74) — **coverage only; do not use for v4 development** |
| Ligue 1 | 2015/16, 2021/22, 2022/23 — **coverage only** |

Also: Champions League slices, World Cups, Euros, women’s competitions, etc. **Not** a continuous 2014–2023 Big 5 domestic panel.

8. **Fields:** event-level (shots with StatsBomb xG, locations, freeze-frames on some 360 matches), lineups, kickoff timestamps. No bookmaker AH history.
9. **Granularity:** event-level (gold standard).
10. **IDs:** competition_id, season_id, match_id, team_id, player_id; kickoff present.
11. **Team naming:** stable IDs; names still need a map to football-data spellings. **MODERATE**.
12. **Point-in-time:** events are post-match. Prior matches only — fine **where matches exist**. Coverage holes make walk-forward on 2019–2023 impossible for PL/BL/SA.
13. **Reproducibility:** high for the Git snapshot (pin a commit).
14. **Licensing:** StatsBomb Open Data User Agreement (`LICENSE.pdf`). Cite StatsBomb and use logo if publishing analysis. Register on statsbomb.com/resource-centre (requested, not a technical gate for Git clone).
15. **Reliability:** repo is maintained; **selection bias** is the real risk (showpiece seasons, not a panel).
16. **Usefulness:** **LOW** for *this* experiment. Excellent data, wrong shape. Do not assume “StatsBomb quality” implies usable history.

Paid StatsBomb/Hudl API could fill the holes. That is a later commercial decision, not the free next step.

---

## FBref (Sports Reference)

1. **Source:** FBref.
2. **URL:** https://fbref.com/ — data-use: https://www.sports-reference.com/data_use.html — bots: https://www.sports-reference.com/bot-traffic.html
3. **Access:** HTML tables. **No official API** (Sports Reference: third-party licences forbid offering data as a download API). Community scrapers exist; they are scrapers.
4. **Cost:** free to browse. Custom extract: they quote a **$5,000 minimum** for fulfilled data requests (data_use.html).
5. **Auth:** none for public pages.
6. **History:** Big 5 **xG on FBref is visible from 2017/18** (verified on 2017-18 league tables). That **misses 2014/15–2016/17** warmup. Match logs with xG exist for later seasons. Provider note: pages currently attribute advanced xG to **Opta / Stats Perform**; historically FBref used StatsBomb. **xG definition may not be homogeneous across years.** Uncertain exact switch date.
7. **Leagues:** PL / BL / SA yes; reserved leagues yes (coverage only).
8. **Fields:** xG/xGA, npxG, xA, shots, possession, progressive actions, player match logs. Not a native Elo. Not AH odds.
9. **Granularity:** season, squad, player, match logs.
10. **IDs:** FBref squad/player slugs; kickoff on match pages.
11. **Team naming:** longer official names; **MODERATE** with aliases.
12. **Point-in-time:** match logs can be prior-only if joined carefully. Season-to-date tables on a page are **not** automatically as-of-D.
13. **Reproducibility:** poor without a frozen scrape; Cloudflare bot filtering; 10 requests/minute jail.
14. **Licensing:** ToS restrict automated access that harms the site; forbid building a competing data store; **forbid using site content to train ML models that predict/classify/score** (data_use.html excerpt, 2026-08-16). A probability model that consumes scraped FBref xG to predict matches is in a sensitive zone. Not legal advice.
15. **Reliability:** high block risk; old tutorials stale; xG vendor change.
16. **Usefulness:** **LOW** for SafeEdge v4. Coverage starts late; access is hostile; ToS are the worst fit of the xG sources.

---

## ClubElo

1. **Source:** ClubElo.
2. **URL:** http://clubelo.com/ — CSV HTTP: `http://api.clubelo.com/` (documented by ClubElo/community and `soccerdata`: “CSV API at http://api.clubelo.com”).
3. **Access:** **documented CSV over HTTP**, no key. Examples used in the wild: `http://api.clubelo.com/YYYY-MM-DD` (all clubs that day), `http://api.clubelo.com/ManCity` (one club history). This is a **CSV download API**, not HTML scraping.
4. **Cost:** free.
5. **Auth:** none.
6. **History:** ratings from **1939**; values before 1960 described as provisional (`soccerdata` ClubElo docs). Far deeper than needed.
7. **Leagues:** European clubs including PL / BL / SA. Source **does not tag league names**; country + level only. Reserved leagues exist as clubs, not as a SafeEdge split.
8. **Fields:** Rank, Club, Country, Level, Elo, **From**, **To**. Fixtures/probabilities on the website. No xG, shots, or AH.
9. **Granularity:** club-date (constant Elo on `[From, To]`).
10. **IDs:** club name string; no football-data ID.
11. **Team naming:** short unique spellings (`Man City`, `Bayern`). **HARD** vs `Man United` / `Man City` / `Nott'm Forest`. Needs an explicit alias table. No silent fuzzy match.
12. **Point-in-time:** **yes, if** the rating used for match date *D* is the row with `From ≤ D ≤ To` **and** that interval does not include *D*’s own result. ClubElo `From`/`To` are documented as periods of constant Elo, typically changing **after** matches. Implementation must take the interval **before** *D*’s result (e.g. Elo whose `To` is the day before *D*, or `From ≤ D-1`). Do **not** recompute Elo from a future sequence.
13. **Reproducibility:** high if CSV responses are archived. Live ratings can be revised; freeze the files.
14. **Licensing:** public API; no open-data PDF found. Not legal advice.
15. **Reliability:** small independent site; API could change. Method is a published Elo, not Opta.
16. **Usefulness:** **MEDIUM**. Genuinely different from league-only Poisson strengths (European coefficient, midweek CL). It is still a **results-based** rating, so it may be largely inside v3’s joint attack/defence. Worth a **secondary** signal, not the xG test.

---

## Market data (beyond current MARKET_AVERAGE)

SafeEdge already stores **one** pre-match AH snapshot. football-data.co.uk says that since **2019/20** CSVs contain **two** sets: collected after opening, and closing (`C` columns). AH closing examples in community schemas: `AHCh`, `B365CAHH`, `B365CAHA`, `AvgCAHH`, `AvgCAHA`.

That is the **best free** richer AH history: **same files, same join, no new vendor**.

Limits:

- not tick-level; no timestamps
- opening collection time is “Friday afternoon / Tuesday afternoon” per notes.txt, not kickoff−N hours
- not usable as a feature **of the same match** if the model is meant to beat the price used for EV (that price is already the input to CandidateEngine)
- closing-vs-opening **of the same match** is a market-efficiency diagnostic, not a pre-match football feature
- Pinnacle quality warning from July 2025 affects **new** files more than the frozen 2014–2023 window

Other free archives (Oddsbase, sgodds, Oddsportal scrapes): not a documented bulk research dump with clear provenance for PL/BL/SA 2014–2023. Treat as **uncertain**. Paid odds APIs exist; do not buy one until the xG experiment is done.

**Best market-history source (free, realistic):** football-data.co.uk closing columns already in the CSVs. Value: **MEDIUM** as a *diagnostic* (did our edge sit on opening vs closing?), **LOW** as v4 football features.

---

## Other sources (brief)

| Source | Note |
|---|---|
| **soccerdata** | Python wrappers around ClubElo, Understat, FBref, football-data. Convenience, not a primary source. Understat/FBref paths remain unofficial scrape. |
| **Kaggle / GitHub Understat dumps** | e.g. community shot CSVs 2014–2021. Provenance = scrape. Stale. Weak reproducibility. Do not prefer over a frozen first-party Understat snapshot. |
| **WhoScored / Sofascore** | scrape of Opta-like stats. Worse ToS/reliability than Understat. |
| **FiveThirtyEight SPI** | effectively discontinued. |
| **TheStatsAPI** | football-data.co.uk paid partner. Not needed for the xG question. |
| **StatsBomb / Opta / Gracenote paid** | would solve coverage and IDs. Out of scope until free sources are exhausted. |

---

## Joinability and a future identity layer

Do **not** implement TeamAlias now. Do **not** silent-fuzzy in production.

When a second source is added, a canonical team layer will probably need:

```text
canonical competition
season
match date (and kickoff only if both sources have a verified timestamp)
explicit alias rows: (provider, external_name_or_id) → SafeEdgeTeamId
```

Matching hierarchy: competition → season → date → home/away aliases.

One-off unmatched rows should **skip** that source for that match (`INSUFFICIENT_IDENTITY`), not guess.

| Source | Join |
|---|---|
| football-data extra columns | EASY |
| Understat | MODERATE (date + alias table; IDs help once mapped) |
| ClubElo | HARD (short names, no league tag) |
| FBref | MODERATE |
| StatsBomb Open Data | MODERATE IDs, but coverage fails first |

---

## Estimated usable dataset (xG experiment)

If Understat match-level xG is snapshotted for 2014/15–2023/24:

- ~10,660 development-league matches with home/away xG
- evaluation 2019/20–2023/24 would have **prior xG from 2014/15 onward**
- first weeks of 2014/15: thin rolling windows (same as score features)
- shot-level is optional later (~10–30 shots/match → hundreds of thousands of shots); **not** required for v4 v1

football-data shots: same ~10,660 matches, already on disk, no join loss.

ClubElo: one pre-match rating per club per date; join loss depends on alias completeness (plan for some skipped matches).

StatsBomb Open Data: **do not size a 2014–2023 walk-forward on it.** PL open data is two seasons, not ten.

The existing PL 2021/22 hole in SafeEdge’s published v3 season table is an **import/window** issue, not an Understat coverage issue. Do not “fix” it by peeking at reserved leagues.

---

## Market-efficiency context (skeptical)

**Predictive information ≠ market-beating information.**

- xG is repeatedly a **better predictor of future scoring/results than raw goals** (e.g. Meadows & Blundell 2023, *PLOS One*; industry Poisson-with-xG vs goals comparisons). That is the same *kind* of win v2/v3 already got: better score likelihood.
- SafeEdge v1–v3 already showed: better log loss **did not** raise Spearman(edge, AH return).
- Asian Handicap markets are often found **closer to efficient** than 1X2 (e.g. Buhagiar, Cortis, Newall, MPRA 2023: AH implied probabilities unbiased vs favourite–longshot in 1X2).
- There *is* academic work that betting prices **under-react to xG vs actual results** (outcome bias: Flepp, Meier, Franck, *Economic Inquiry* 2023, Big 5 2013/14–2017/18, Matchbook/Oddsportal). That is a **hypothesis**, not a SafeEdge result, and it was not an AH walk-forward with CandidateEngine.
- Constantinou-style AH studies with ratings/Bayesian nets do not give SafeEdge a free lunch.

So: xG is the right **next information** to test. It is **not** a reason to expect profitable AH ranking. The v4 question is ranking/calibration vs v3, not ROI.

---

## PRIMARY recommendation

**Understat match-level xG (home_xg / away_xg, optionally npxG), frozen local snapshot, prior matches only.**

Why this, not the fancier StatsBomb dump:

1. **New information:** shot quality, not just goals or team IDs.
2. **History:** 2014/15 → covers warmup + eval.
3. **PL / BL / SA:** all six top leagues including development three.
4. **Point-in-time:** reconstructable from completed prior matches.
5. **Reproducibility:** only if SafeEdge **archives JSON** at import time.
6. **Free:** yes, with unofficial access.
7. **Complexity:** one adapter + explicit team aliases + date join. Shot-level not required for the first test.

**Caveat:** this is unofficial JSON-in-HTML, not an API. If that access path is rejected, do **not** switch to FBref. Fall back to football-data `HS`/`HST` (see precursor below) or revisit paid StatsBomb later.

## SECONDARY recommendation

**ClubElo point-in-time Elo** (interval ending before match date), explicit aliases, frozen CSVs.

Independent of Understat’s xG model. May be collinear with v3 strengths. Still the best free **rating** source with a real CSV API and pre-match `From`/`To`.

**Already-owned control (not a third vendor):** persist football-data `HS`/`AS`/`HST`/`AST`. Same join. Answers whether **shot volume** moves AH ranking before investing in unofficial xG.

**Not recommended as primary:** StatsBomb Open Data (coverage), FBref (ToS + late xG + vendor change), Kaggle mirrors (stale), paid odds APIs (premature).

---

## Proposed next experiment (do not implement here)

Preferred shape:

```text
historical scores (existing)
        +
historical prior-match xG (Understat snapshot)
        ↓
point-in-time feature builder  (same date-batch rules as today)
        ↓
Probability Model v4  (new class; v1/v2/v3 frozen)
        ↓
same CandidateEngine
        ↓
same AH prices (MARKET_AVERAGE)
        ↓
same diagnostics / same development leagues
```

**Optional precursor (still no v4):** import shots/SOT from existing CSVs and add last-5/last-10 shot rates to features only. If even shot volume cannot move Spearman, that is useful; it does not kill the xG test (quality ≠ volume).

### v4 conceptual test (after a frozen Understat snapshot)

Features from **prior** matches only, e.g.:

- rolling xG for / xGA (last 5 / 10, overall + venue)
- xG difference
- goals − xG (over/under-performance) — still prior-only
- **not** current-match xG
- **not** AH line, odds, edge, or ROI in fitting

Keep CandidateEngine, StrategyEngine, `minimumEdge`, Kelly, settlement, bankroll unchanged.

Development: Premier League, Bundesliga, Serie A.  
Reserved: La Liga, Ligue 1 — coverage may be imported later; **no evaluation**.

If Understat cannot support this split (it can), do not invent a new split after seeing numbers. The split stays.

### What would justify proceeding

Predeclare before running, then freeze. Suggested gates vs **v3** (not vs v1):

- Spearman(edge, realized AH unit return) improves by **≥ +0.05** vs v3 in **at least 2 of 3** development leagues
- third league Spearman not worse by **≥ 0.02**
- ≥10% predicted-edge **WIN and LOSS** absolute calibration gaps shrink by **≥ 3pp** in at least 2 of 3
- score log loss not worse by **> 0.02** in any development league

ROI is secondary. Positive strategy ROI is **not** success.

Then, and only then, consider **one** reserved-league validation run.

### What would tell us to stop richer score/performance modelling

If v4 with prior xG (and, if run, shot volume) still shows Spearman ≈ 0 and the same high-edge WIN overconfidence, **stop adding football-performance features derived from shots/xG/Elo**.

The remaining information is then likely **market microstructure** (true timed prices), **availability** (lineups/injuries with timestamps), or **the AH market is already pricing this class of signal**. That would be a different research phase, not Probability Model v5-with-another-xG-vendor.

---

## Anti-overfitting

Unchanged:

- develop on PL / BL / SA
- do not inspect La Liga / Ligue 1 outcomes
- do not tune regularization, half-life, `minimumEdge`, or Kelly on ROI
- do not add production filters from diagnostics

Understat and ClubElo **can** support the same split. StatsBomb Open Data **cannot**; that is why it is not primary.

---

## Sources investigated

Primary pages and indexes consulted 2026-08-16:

- https://understat.com/
- https://github.com/statsbomb/open-data `data/competitions.json`
- https://github.com/statsbomb/statsbombpy
- https://fbref.com/ (2017-18 Big 5 tables for xG presence)
- https://www.sports-reference.com/data_use.html
- https://www.sports-reference.com/bot-traffic.html
- http://clubelo.com/ and `http://api.clubelo.com/` (via soccerdata ClubElo docs)
- https://www.football-data.co.uk/notes.txt
- https://www.football-data.co.uk/data.php
- https://soccerdata.readthedocs.io/ (ClubElo, Understat)
- academic: Meadows & Blundell 2023; Flepp et al. 2023 *Economic Inquiry*; Buhagiar et al. MPRA 2023; Constantinou JSA 2021

Uncertain items are labelled in the source sections (Understat ToS; ClubElo revision policy; FBref xG vendor switch date; third-party odds archives).
