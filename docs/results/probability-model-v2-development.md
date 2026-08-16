# Probability Model v2 Development Evaluation

## Model definition

Implemented model: `RegularizedDixonColesFootballProbabilityModel`.
Frozen baseline: `PoissonFootballProbabilityModel` (v1, unchanged).

### Shrinkage

```text
shrunkRate = (weightedTeamGoals + prior * leagueRate)
           / (weightedTeamExposure + prior)
shrunkStrength = shrunkRate / leagueRate
```

- `attackDefenceShrinkageStrength` default = **5** weighted league-average pseudo-matches (`Σ timeWeight`, not raw match count).
- Chosen before evaluation as the same order of magnitude as `minimumTeamMatches`. Not fitted to ROI.
- `prior = 0` reproduces the unregularized v1 ratio (Dixon-Coles still differs when enabled).

### Dixon-Coles

```text
τ(0,0) = 1 − λμρ
τ(0,1) = 1 + λρ
τ(1,0) = 1 + μρ
τ(1,1) = 1 − ρ
τ(x,y) = 1 otherwise
```

- ρ is fitted walk-forward from **weighted score log-likelihood** on matches with `matchDate < targetDate`.
- Market odds, candidate edge, and betting ROI never enter ρ fitting.
- Search is deterministic golden-section maximization on a τ-valid interval; ρ = 0 is always a candidate.
- Shared Poisson defaults remain decayHalfLifeDays=180, maxGoalsPerTeam=10, minimumTeamMatches=5.

## Anti-leakage

- Walk-forward: same competition, `matchDate < targetDate`, no same-day, no future.
- Score-only ρ fitting. No bookmaker odds as model features.
- Development leagues only: Premier League, Bundesliga, Serie A.
- La Liga and Ligue 1 were not run and were not inspected.
- CandidateEngine, StrategyEngine, BacktestEngine, and SettlementEngine were not changed.
- HISTORICAL QUOTE SOURCE = MARKET_AVERAGE (football-data.co.uk, not Tippmix).
- Window: trainingFromSeason=2014, evaluation 2019→2023, starting bankroll 100000.

## Premier League

- Predictions available: v1=1470 v2=1470
- Candidates: v1=2940 v2=2940

### Score prediction

| Metric | V1 | V2 |
|---|---|---|
| score log loss | 3.061582 | 3.0157 |
| predicted home goals | 1.562478 | 1.563324 |
| actual home goals | 1.57551 | 1.57551 |
| predicted away goals | 1.262254 | 1.262178 |
| actual away goals | 1.313605 | 1.313605 |
| 1X2 HOME predicted | 0.443058 | 0.441786 |
| 1X2 HOME actual | 0.446939 | 0.446939 |
| 1X2 DRAW predicted | 0.225691 | 0.2408 |
| 1X2 DRAW actual | 0.22381 | 0.22381 |
| 1X2 AWAY predicted | 0.331251 | 0.317415 |
| 1X2 AWAY actual | 0.329252 | 0.329252 |

Margin categories:

| Category | V1 pred | V1 actual | V2 pred | V2 actual |
|---|---|---|---|---|
| HOME_WIN_BY_2_PLUS | 0.248631 | 0.241497 | 0.236378 | 0.241497 |
| HOME_WIN_BY_1 | 0.194427 | 0.205442 | 0.205408 | 0.205442 |
| DRAW | 0.225691 | 0.22381 | 0.2408 | 0.22381 |
| AWAY_WIN_BY_1 | 0.167237 | 0.156463 | 0.170839 | 0.156463 |
| AWAY_WIN_BY_2_PLUS | 0.164015 | 0.172789 | 0.146576 | 0.172789 |

### Edge ranking

| Metric | V1 | V2 |
|---|---|---|
| Spearman | 0.0172 | 0.01207 |
| Pearson | 0.012664 | 0.016194 |
| mean predicted edge | -0.029834 | -0.030036 |
| realized unit ROI | -0.029741 | -0.029741 |
| decile ROI inversions (n≥30) | 5 | 5 |

| Decile | V1 n | V1 avg edge | V1 ROI | V2 n | V2 avg edge | V2 ROI |
|---|---|---|---|---|---|---|
| decile 1 (lowest edge to highest) | 294 | -0.441356 | -0.023656 | 294 | -0.403584 | -0.073639 |
| decile 2 (lowest edge to highest) | 294 | -0.273428 | -0.08784 | 294 | -0.25717 | -0.014864 |
| decile 3 (lowest edge to highest) | 294 | -0.18579 | -0.011378 | 294 | -0.178933 | -0.026054 |
| decile 4 (lowest edge to highest) | 294 | -0.116097 | -0.011871 | 294 | -0.112528 | -0.010969 |
| decile 5 (lowest edge to highest) | 294 | -0.05699 | -0.005714 | 294 | -0.057071 | -0.032296 |
| decile 6 (lowest edge to highest) | 294 | -0.002114 | -0.040425 | 294 | -0.002074 | -0.01318 |
| decile 7 (lowest edge to highest) | 294 | 0.05648 | -0.075544 | 294 | 0.053062 | -0.041224 |
| decile 8 (lowest edge to highest) | 294 | 0.12715 | -0.05318 | 294 | 0.11831 | -0.042279 |
| decile 9 (lowest edge to highest) | 294 | 0.213902 | 0.059439 | 294 | 0.196527 | -0.067789 |
| decile 10 (lowest edge to highest) | 294 | 0.379908 | -0.047245 | 294 | 0.343104 | 0.024881 |

### High edge

#### ≥ 10%

| Metric | V1 | V2 |
|---|---|---|
| n | 851 | 817 |
| avg edge | 0.245616 | 0.229396 |
| unit ROI | -0.015417 | -0.01634 |
| predicted P(WIN) | 0.553973 | 0.547966 |
| actual WIN | 0.40188 | 0.400245 |
| predicted P(LOSS) | 0.269844 | 0.282077 |
| actual LOSS | 0.378378 | 0.381885 |

#### ≥ 20%

| Metric | V1 | V2 |
|---|---|---|
| n | 474 | 423 |
| avg edge | 0.324177 | 0.306954 |
| unit ROI | -0.021962 | -0.005863 |
| predicted P(WIN) | 0.601874 | 0.593454 |
| actual WIN | 0.394515 | 0.408983 |
| predicted P(LOSS) | 0.235229 | 0.250947 |
| actual LOSS | 0.383966 | 0.387707 |

#### ≥ 30%

| Metric | V1 | V2 |
|---|---|---|
| n | 238 | 187 |
| avg edge | 0.402628 | 0.384387 |
| unit ROI | 0.015378 | 0.071471 |
| predicted P(WIN) | 0.646961 | 0.642688 |
| actual WIN | 0.415966 | 0.44385 |
| predicted P(LOSS) | 0.205045 | 0.213674 |
| actual LOSS | 0.37395 | 0.331551 |

### Low-score calibration (0-0 / 1-0 / 0-1 / 1-1)

| Score | V1 pred | V1 actual | V2 pred | V2 actual |
|---|---|---|---|---|
| 0-0 | 0.072022 | 0.055782 | 0.066598 | 0.055782 |
| 1-0 | 0.095714 | 0.087755 | 0.091842 | 0.087755 |
| 0-1 | 0.081581 | 0.066667 | 0.075988 | 0.066667 |
| 1-1 | 0.100603 | 0.106122 | 0.111464 | 0.106122 |

### Fitted ρ (score likelihood only)

- n=1470
- min=-0.115893
- median=-0.034283
- max=0.109008

### Strength shrinkage (raw v1-style ratio vs shrunk v2)

| Strength | raw MAD from 1 | shrunk MAD from 1 | raw p50 | shrunk p50 | raw p95 | shrunk p95 | raw max | shrunk max |
|---|---|---|---|---|---|---|---|---|
| homeAttack | 0.283696 | 0.186236 | 0.949708 | 0.972077 | 1.779555 | 1.547867 | 2.474062 | 1.834443 |
| homeDefence | 0.228801 | 0.147156 | 0.961812 | 0.973468 | 1.563461 | 1.348239 | 2.52325 | 1.830969 |
| awayAttack | 0.272557 | 0.17839 | 0.957343 | 0.97158 | 1.59379 | 1.405734 | 2.103484 | 1.816887 |
| awayDefence | 0.205527 | 0.131192 | 0.983593 | 0.989508 | 1.434811 | 1.269484 | 2.740639 | 1.701058 |

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI |
|---|---|---|---|---|
| DEFENSIVE | 117 | -0.030255 | 233 | -0.058175 |
| BALANCED | 122 | -0.021072 | 153 | -0.06704 |
| GROWTH | 123 | -0.021641 | 122 | -0.071655 |
| FLAT_STAKE | 588 | -0.038249 | 789 | -0.025473 |


## Bundesliga

- Predictions available: v1=1481 v2=1481
- Candidates: v1=2962 v2=2962

### Score prediction

| Metric | V1 | V2 |
|---|---|---|
| score log loss | 3.143268 | 3.10184 |
| predicted home goals | 1.748046 | 1.749208 |
| actual home goals | 1.746793 | 1.746793 |
| predicted away goals | 1.409947 | 1.404538 |
| actual away goals | 1.405807 | 1.405807 |
| 1X2 HOME predicted | 0.456563 | 0.443263 |
| 1X2 HOME actual | 0.434841 | 0.434841 |
| 1X2 DRAW predicted | 0.2099 | 0.24911 |
| 1X2 DRAW actual | 0.251857 | 0.251857 |
| 1X2 AWAY predicted | 0.333537 | 0.307627 |
| 1X2 AWAY actual | 0.313302 | 0.313302 |

Margin categories:

| Category | V1 pred | V1 actual | V2 pred | V2 actual |
|---|---|---|---|---|
| HOME_WIN_BY_2_PLUS | 0.267579 | 0.261985 | 0.257163 | 0.261985 |
| HOME_WIN_BY_1 | 0.188984 | 0.172856 | 0.1861 | 0.172856 |
| DRAW | 0.2099 | 0.251857 | 0.24911 | 0.251857 |
| AWAY_WIN_BY_1 | 0.160563 | 0.147198 | 0.151757 | 0.147198 |
| AWAY_WIN_BY_2_PLUS | 0.172974 | 0.166104 | 0.15587 | 0.166104 |

### Edge ranking

| Metric | V1 | V2 |
|---|---|---|
| Spearman | -0.010761 | 0.0005 |
| Pearson | -0.033968 | -0.007716 |
| mean predicted edge | -0.031134 | -0.031214 |
| realized unit ROI | -0.032058 | -0.032058 |
| decile ROI inversions (n≥30) | 6 | 4 |

| Decile | V1 n | V1 avg edge | V1 ROI | V2 n | V2 avg edge | V2 ROI |
|---|---|---|---|---|---|---|
| decile 1 (lowest edge to highest) | 297 | -0.412241 | 0.018182 | 297 | -0.367267 | -0.054747 |
| decile 2 (lowest edge to highest) | 296 | -0.26525 | -0.005068 | 296 | -0.23421 | 0.063412 |
| decile 3 (lowest edge to highest) | 296 | -0.180915 | -0.046993 | 296 | -0.163996 | -0.066216 |
| decile 4 (lowest edge to highest) | 296 | -0.117305 | 0.033159 | 296 | -0.106958 | -0.04603 |
| decile 5 (lowest edge to highest) | 296 | -0.062534 | -0.041993 | 296 | -0.057539 | -0.050084 |
| decile 6 (lowest edge to highest) | 297 | -0.001417 | 0.004832 | 297 | -0.00531 | 0.025202 |
| decile 7 (lowest edge to highest) | 296 | 0.055127 | -0.103649 | 296 | 0.044028 | -0.029307 |
| decile 8 (lowest edge to highest) | 296 | 0.117701 | -0.036182 | 296 | 0.101819 | -0.019003 |
| decile 9 (lowest edge to highest) | 296 | 0.203095 | -0.059882 | 296 | 0.171687 | -0.151757 |
| decile 10 (lowest edge to highest) | 296 | 0.353581 | -0.083277 | 296 | 0.306655 | 0.007838 |

### High edge

#### ≥ 10%

| Metric | V1 | V2 |
|---|---|---|
| n | 806 | 751 |
| avg edge | 0.238341 | 0.21299 |
| unit ROI | -0.074026 | -0.061731 |
| predicted P(WIN) | 0.554189 | 0.536038 |
| actual WIN | 0.394541 | 0.396804 |
| predicted P(LOSS) | 0.27812 | 0.291855 |
| actual LOSS | 0.423077 | 0.422104 |

#### ≥ 20%

| Metric | V1 | V2 |
|---|---|---|
| n | 449 | 345 |
| avg edge | 0.310711 | 0.292986 |
| unit ROI | -0.080523 | -0.028029 |
| predicted P(WIN) | 0.594763 | 0.577145 |
| actual WIN | 0.389755 | 0.417391 |
| predicted P(LOSS) | 0.247929 | 0.258106 |
| actual LOSS | 0.420935 | 0.423188 |

#### ≥ 30%

| Metric | V1 | V2 |
|---|---|---|
| n | 199 | 122 |
| avg edge | 0.392041 | 0.376764 |
| unit ROI | -0.106533 | -0.009877 |
| predicted P(WIN) | 0.636867 | 0.628125 |
| actual WIN | 0.371859 | 0.434426 |
| predicted P(LOSS) | 0.214208 | 0.219431 |
| actual LOSS | 0.432161 | 0.401639 |

### Low-score calibration (0-0 / 1-0 / 0-1 / 1-1)

| Score | V1 pred | V1 actual | V2 pred | V2 actual |
|---|---|---|---|---|
| 0-0 | 0.051215 | 0.049291 | 0.060703 | 0.049291 |
| 1-0 | 0.078074 | 0.055368 | 0.061783 | 0.055368 |
| 0-1 | 0.065419 | 0.054018 | 0.048503 | 0.054018 |
| 1-1 | 0.093862 | 0.128967 | 0.113868 | 0.128967 |

### Fitted ρ (score likelihood only)

- n=1481
- min=-0.242335
- median=-0.138661
- max=-0.05835

### Strength shrinkage (raw v1-style ratio vs shrunk v2)

| Strength | raw MAD from 1 | shrunk MAD from 1 | raw p50 | shrunk p50 | raw p95 | shrunk p95 | raw max | shrunk max |
|---|---|---|---|---|---|---|---|---|
| homeAttack | 0.24778 | 0.168388 | 0.953235 | 0.968674 | 1.717533 | 1.505621 | 2.265051 | 1.888314 |
| homeDefence | 0.201584 | 0.132288 | 0.982725 | 0.987665 | 1.394796 | 1.270258 | 2.918054 | 1.627276 |
| awayAttack | 0.256316 | 0.17298 | 0.977289 | 0.984789 | 1.622083 | 1.438244 | 2.238339 | 1.858827 |
| awayDefence | 0.184667 | 0.123471 | 0.979299 | 0.985079 | 1.373364 | 1.23631 | 2.24962 | 1.694664 |

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI |
|---|---|---|---|---|
| DEFENSIVE | 194 | -0.034371 | 144 | -0.11823 |
| BALANCED | 202 | -0.022986 | 106 | -0.127157 |
| GROWTH | 109 | -0.047705 | 113 | -0.123356 |
| FLAT_STAKE | 375 | -0.028569 | 372 | -0.058958 |


## Serie A

- Predictions available: v1=1831 v2=1831
- Candidates: v1=3658 v2=3658

### Score prediction

| Metric | V1 | V2 |
|---|---|---|
| score log loss | 2.945234 | 2.912799 |
| predicted home goals | 1.518242 | 1.52457 |
| actual home goals | 1.525396 | 1.525396 |
| predicted away goals | 1.303529 | 1.30248 |
| actual away goals | 1.298744 | 1.298744 |
| 1X2 HOME predicted | 0.428465 | 0.419101 |
| 1X2 HOME actual | 0.413435 | 0.413435 |
| 1X2 DRAW predicted | 0.229322 | 0.255692 |
| 1X2 DRAW actual | 0.260513 | 0.260513 |
| 1X2 AWAY predicted | 0.342213 | 0.325207 |
| 1X2 AWAY actual | 0.326051 | 0.326051 |

Margin categories:

| Category | V1 pred | V1 actual | V2 pred | V2 actual |
|---|---|---|---|---|
| HOME_WIN_BY_2_PLUS | 0.233322 | 0.211906 | 0.222928 | 0.211906 |
| HOME_WIN_BY_1 | 0.195143 | 0.201529 | 0.196173 | 0.201529 |
| DRAW | 0.229322 | 0.260513 | 0.255692 | 0.260513 |
| AWAY_WIN_BY_1 | 0.171701 | 0.181868 | 0.169071 | 0.181868 |
| AWAY_WIN_BY_2_PLUS | 0.170511 | 0.144184 | 0.156136 | 0.144184 |

### Edge ranking

| Metric | V1 | V2 |
|---|---|---|
| Spearman | 0.009322 | -0.009856 |
| Pearson | -0.000405 | -0.013301 |
| mean predicted edge | -0.0316 | -0.031506 |
| realized unit ROI | -0.030197 | -0.030197 |
| decile ROI inversions (n≥30) | 4 | 4 |

| Decile | V1 n | V1 avg edge | V1 ROI | V2 n | V2 avg edge | V2 ROI |
|---|---|---|---|---|---|---|
| decile 1 (lowest edge to highest) | 366 | -0.381095 | -0.03291 | 366 | -0.344069 | -0.004549 |
| decile 2 (lowest edge to highest) | 366 | -0.234652 | 0.009016 | 366 | -0.216054 | 0.030587 |
| decile 3 (lowest edge to highest) | 366 | -0.159661 | -0.099112 | 366 | -0.150174 | -0.078279 |
| decile 4 (lowest edge to highest) | 366 | -0.104524 | -0.016831 | 366 | -0.095895 | -0.016598 |
| decile 5 (lowest edge to highest) | 365 | -0.054636 | -0.065425 | 365 | -0.051474 | -0.076712 |
| decile 6 (lowest edge to highest) | 366 | -0.008764 | -0.000437 | 366 | -0.011023 | 0.003798 |
| decile 7 (lowest edge to highest) | 366 | 0.041277 | -0.017596 | 366 | 0.033039 | -0.01168 |
| decile 8 (lowest edge to highest) | 366 | 0.097597 | 0.002842 | 366 | 0.087164 | 0.002678 |
| decile 9 (lowest edge to highest) | 366 | 0.172165 | -0.044604 | 366 | 0.153675 | -0.078142 |
| decile 10 (lowest edge to highest) | 365 | 0.317187 | -0.037027 | 365 | 0.28055 | -0.073315 |

### High edge

#### ≥ 10%

| Metric | V1 | V2 |
|---|---|---|
| n | 894 | 838 |
| avg edge | 0.220989 | 0.203275 |
| unit ROI | -0.024597 | -0.054809 |
| predicted P(WIN) | 0.536873 | 0.528 |
| actual WIN | 0.395973 | 0.381862 |
| predicted P(LOSS) | 0.278866 | 0.294358 |
| actual LOSS | 0.389262 | 0.403341 |

#### ≥ 20%

| Metric | V1 | V2 |
|---|---|---|
| n | 423 | 350 |
| avg edge | 0.302358 | 0.284072 |
| unit ROI | -0.04117 | -0.065386 |
| predicted P(WIN) | 0.585301 | 0.57814 |
| actual WIN | 0.404255 | 0.394286 |
| predicted P(LOSS) | 0.243748 | 0.259998 |
| actual LOSS | 0.404255 | 0.408571 |

#### ≥ 30%

| Metric | V1 | V2 |
|---|---|---|
| n | 160 | 119 |
| avg edge | 0.398012 | 0.36185 |
| unit ROI | 0.075719 | -0.028025 |
| predicted P(WIN) | 0.634797 | 0.63356 |
| actual WIN | 0.45 | 0.445378 |
| predicted P(LOSS) | 0.204203 | 0.231114 |
| actual LOSS | 0.3375 | 0.428571 |

### Low-score calibration (0-0 / 1-0 / 0-1 / 1-1)

| Score | V1 pred | V1 actual | V2 pred | V2 actual |
|---|---|---|---|---|
| 0-0 | 0.070775 | 0.060076 | 0.072994 | 0.060076 |
| 1-0 | 0.093837 | 0.085745 | 0.083465 | 0.085745 |
| 0-1 | 0.081732 | 0.068269 | 0.070692 | 0.068269 |
| 1-1 | 0.102693 | 0.125068 | 0.118625 | 0.125068 |

### Fitted ρ (score likelihood only)

- n=1831
- min=-0.228242
- median=-0.066854
- max=0.030957

### Strength shrinkage (raw v1-style ratio vs shrunk v2)

| Strength | raw MAD from 1 | shrunk MAD from 1 | raw p50 | shrunk p50 | raw p95 | shrunk p95 | raw max | shrunk max |
|---|---|---|---|---|---|---|---|---|
| homeAttack | 0.24185 | 0.16811 | 0.971595 | 0.980456 | 1.551768 | 1.393482 | 1.833691 | 1.596459 |
| homeDefence | 0.220192 | 0.150344 | 0.970237 | 0.979924 | 1.487449 | 1.29716 | 2.892424 | 1.617437 |
| awayAttack | 0.260235 | 0.180494 | 0.969204 | 0.978321 | 1.578951 | 1.413518 | 1.966944 | 1.717158 |
| awayDefence | 0.203934 | 0.140253 | 0.975369 | 0.982338 | 1.432515 | 1.289324 | 2.200267 | 1.656676 |

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI |
|---|---|---|---|---|
| DEFENSIVE | 198 | -0.027486 | 177 | 0.014948 |
| BALANCED | 179 | -0.025404 | 186 | 0.025324 |
| GROWTH | 219 | -0.023299 | 192 | 0.028127 |
| FLAT_STAKE | 332 | -0.057821 | 267 | -0.034416 |


## Cross-league summary

| League | Spearman V1 | Spearman V2 | Pearson V1 | Pearson V2 | log loss V1 | log loss V2 |
|---|---|---|---|---|---|---|
| Premier League | 0.0172 | 0.01207 | 0.012664 | 0.016194 | 3.061582 | 3.0157 |
| Bundesliga | -0.010761 | 0.0005 | -0.033968 | -0.007716 | 3.143268 | 3.10184 |
| Serie A | 0.009322 | -0.009856 | -0.000405 | -0.013301 | 2.945234 | 2.912799 |

## Confidence compression

Question: does shrinkage reduce pathological extreme confidence?

### Premier League

| predicted edge | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | -0.028913 | 0.125635 | 0.270379 | 0.358401 | 0.508579 | 0.700431 |
| V2 | -0.028838 | 0.118075 | 0.246731 | 0.3224 | 0.458946 | 0.734367 |

| P(WIN) | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 0.400423 | 0.502426 | 0.600908 | 0.650941 | 0.741572 | 0.906915 |
| V2 | 0.400907 | 0.498426 | 0.592419 | 0.640629 | 0.719847 | 0.859146 |

| P(LOSS) | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 0.400423 | 0.502426 | 0.600908 | 0.650941 | 0.741572 | 0.906915 |
| V2 | 0.400907 | 0.498426 | 0.592419 | 0.640629 | 0.719847 | 0.859146 |

| lambdaHome | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 1.417737 | 1.922384 | 2.554517 | 2.92082 | 3.780918 | 5.474818 |
| V2 | 1.48558 | 1.810231 | 2.20333 | 2.429165 | 2.936439 | 3.650399 |

| lambdaAway | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 1.156706 | 1.593458 | 2.052178 | 2.272766 | 2.855362 | 4.279903 |
| V2 | 1.203952 | 1.490923 | 1.758804 | 1.906836 | 2.278286 | 3.197781 |

### Bundesliga

| predicted edge | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | -0.03386 | 0.115927 | 0.253731 | 0.327611 | 0.47621 | 0.7779 |
| V2 | -0.031733 | 0.101196 | 0.218242 | 0.289198 | 0.400345 | 0.63183 |

| P(WIN) | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 0.407459 | 0.502596 | 0.587359 | 0.633324 | 0.714346 | 0.898543 |
| V2 | 0.405909 | 0.493096 | 0.568215 | 0.615161 | 0.695663 | 0.774123 |

| P(LOSS) | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 0.407459 | 0.502596 | 0.587359 | 0.633324 | 0.714346 | 0.898543 |
| V2 | 0.405909 | 0.493096 | 0.568215 | 0.615161 | 0.695663 | 0.774123 |

| lambdaHome | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 1.622678 | 2.088405 | 2.72782 | 3.162503 | 3.916054 | 6.563773 |
| V2 | 1.680948 | 1.982487 | 2.398588 | 2.693265 | 3.167137 | 4.537835 |

| lambdaAway | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 1.305873 | 1.737545 | 2.198987 | 2.48648 | 3.258261 | 6.277476 |
| V2 | 1.344656 | 1.637989 | 1.931381 | 2.119092 | 2.499671 | 3.245559 |

### Serie A

| predicted edge | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | -0.030928 | 0.095832 | 0.21883 | 0.285889 | 0.444132 | 0.839108 |
| V2 | -0.031373 | 0.086902 | 0.195927 | 0.26292 | 0.373994 | 0.564642 |

| P(WIN) | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 0.400167 | 0.489171 | 0.566096 | 0.618682 | 0.707633 | 0.906142 |
| V2 | 0.395104 | 0.481075 | 0.558595 | 0.604664 | 0.686404 | 0.813074 |

| P(LOSS) | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 0.400167 | 0.489171 | 0.566096 | 0.618682 | 0.707633 | 0.906142 |
| V2 | 0.395104 | 0.481075 | 0.558595 | 0.604664 | 0.686404 | 0.813074 |

| lambdaHome | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 1.421655 | 1.870183 | 2.354394 | 2.646235 | 3.233427 | 4.497123 |
| V2 | 1.473723 | 1.772343 | 2.101626 | 2.299798 | 2.665474 | 3.529745 |

| lambdaAway | p50 | p75 | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| V1 | 1.198152 | 1.63027 | 2.077032 | 2.333007 | 3.035643 | 4.069658 |
| V2 | 1.243626 | 1.537655 | 1.833151 | 1.99619 | 2.324126 | 3.037876 |

## Edge ranking

- Premier League Spearman delta=-0.00513 Pearson delta=0.003531 decile inversions 5→5
- Bundesliga Spearman delta=0.011262 Pearson delta=0.026252 decile inversions 6→4
- Serie A Spearman delta=-0.019178 Pearson delta=-0.012896 decile inversions 4→4

## Settlement calibration

| League | ≥10% V1 P(WIN)/actual | ≥10% V2 P(WIN)/actual | ≥10% V1 P(LOSS)/actual | ≥10% V2 P(LOSS)/actual |
|---|---|---|---|---|
| Premier League | 0.553973/0.40188 | 0.547966/0.400245 | 0.269844/0.378378 | 0.282077/0.381885 |
| Bundesliga | 0.554189/0.394541 | 0.536038/0.396804 | 0.27812/0.423077 | 0.291855/0.422104 |
| Serie A | 0.536873/0.395973 | 0.528/0.381862 | 0.278866/0.389262 | 0.294358/0.403341 |

## Score likelihood

- Premier League log loss delta=-0.045881 (positive means v2 is worse)
- Bundesliga log loss delta=-0.041428 (positive means v2 is worse)
- Serie A log loss delta=-0.032436 (positive means v2 is worse)

## Strategy secondary metrics

Unchanged presets: DEFENSIVE, BALANCED, GROWTH, FLAT_STAKE. ROI is **not** a v2 success gate.

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI |
|---|---|---|---|---|
| DEFENSIVE | 117 | -0.030255 | 233 | -0.058175 |
| BALANCED | 122 | -0.021072 | 153 | -0.06704 |
| GROWTH | 123 | -0.021641 | 122 | -0.071655 |
| FLAT_STAKE | 588 | -0.038249 | 789 | -0.025473 |

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI |
|---|---|---|---|---|
| DEFENSIVE | 194 | -0.034371 | 144 | -0.11823 |
| BALANCED | 202 | -0.022986 | 106 | -0.127157 |
| GROWTH | 109 | -0.047705 | 113 | -0.123356 |
| FLAT_STAKE | 375 | -0.028569 | 372 | -0.058958 |

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI |
|---|---|---|---|---|
| DEFENSIVE | 198 | -0.027486 | 177 | 0.014948 |
| BALANCED | 179 | -0.025404 | 186 | 0.025324 |
| GROWTH | 219 | -0.023299 | 192 | 0.028127 |
| FLAT_STAKE | 332 | -0.057821 | 267 | -0.034416 |

## Hypotheses

- Independent Poisson plus noisy venue ratios can produce extreme AH settlement tails.
- Shrinkage toward league-average strength should compress those tails.
- Dixon-Coles should mainly move 0-0 / 1-0 / 0-1 / 1-1 mass, not 1X2 means.
- Edge ranking can remain weak even if score means are well calibrated.

## Explicit non-conclusions

- Classification: **MODEL_V2_NO_MEANINGFUL_IMPROVEMENT**
- PREMIER_LEAGUE Spearman delta -0.00512975558242160857787157294454702 is below the small-improvement cutoff
- BUNDESLIGA Spearman delta 0.0112618291785568189050831176474121717 is below the small-improvement cutoff
- SERIE_A Spearman delta -0.019178315700703838633934377189370029 is below the small-improvement cutoff
- Changes were mixed or smaller than the predeclared material thresholds. ROI was not used.
- Decile ROI inversions improved in 1 league(s); not sufficient alone.
- This is not a claim that SafeEdge is profitable.
- Parameters were not changed after seeing ROI.
- La Liga and Ligue 1 remain untouched validation leagues.

