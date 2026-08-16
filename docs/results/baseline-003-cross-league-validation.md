# Baseline 003 – Cross-League Structural Validation

## Experiment configuration

- Leagues: PREMIER_LEAGUE (published Baseline 001/002) and BUNDESLIGA (this replication).
- Training from season: 2014
- Evaluation range: 2019 → 2023
- HISTORICAL QUOTE SOURCE = MARKET_AVERAGE
- These prices are football-data.co.uk historical quotes, not Tippmix odds.
- Model: independent time-decayed Poisson defaults (not retuned).
- decayHalfLifeDays = 180
- maxGoalsPerTeam = 10
- minimumTeamMatches = 5
- No league-specific tuning. No production filter. Zero-tuning replication.
- Premier League numbers are the published Baseline 001/002 results, not a rerun.

## Prediction quality

| Metric | PREMIER_LEAGUE | BUNDESLIGA |
| --- | ---: | ---: |
| Predictions available | 1470 | 1481 |
| Average actual-score log loss | 3.061582 | 3.143268 |
| Predicted home goals | 1.562478 | 1.748046 |
| Actual home goals | 1.57551 | 1.746793 |
| Predicted away goals | 1.262254 | 1.409947 |
| Actual away goals | 1.313605 | 1.405807 |
| Predicted home win | 0.443058 | 0.456563 |
| Actual home win | 0.446939 | 0.434841 |
| Predicted draw | 0.225691 | 0.2099 |
| Actual draw | 0.22381 | 0.251857 |
| Predicted away win | 0.331251 | 0.333537 |
| Actual away win | 0.329252 | 0.313302 |

Margin categories use the same Baseline 001 buckets.

| Margin | PL predicted | PL actual | BL predicted | BL actual |
| --- | ---: | ---: | ---: | ---: |
| HOME_WIN_BY_2_PLUS | 0.248631 | 0.241497 | 0.267579 | 0.261985 |
| HOME_WIN_BY_1 | 0.194427 | 0.205442 | 0.188984 | 0.172856 |
| DRAW | 0.225691 | 0.22381 | 0.2099 | 0.251857 |
| AWAY_WIN_BY_1 | 0.167237 | 0.156463 | 0.160563 | 0.147198 |
| AWAY_WIN_BY_2_PLUS | 0.164015 | 0.172789 | 0.172974 | 0.166104 |

## Edge quality

| Metric | PREMIER_LEAGUE | BUNDESLIGA |
| --- | ---: | ---: |
| Candidate count | 2940 | 2962 |
| +EV count | 1306 | 1327 |
| −EV count | 1634 | 1635 |
| Average predicted edge | -0.029834 | -0.031134 |
| Average realized unit return | -0.029741 | -0.032058 |
| Calibration gap | 0.000092 | -0.000923 |
| Spearman(edge, realized return) | 0.0172 | -0.010761 |
| Pearson(edge, realized return) | 0.012664 | -0.033968 |

Single-bet realized return is noisy. Correlation is diagnostic, not proof.

### Edge deciles

| Decile | PL n | PL avg edge | PL ROI | BL n | BL avg edge | BL ROI |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 294 | -0.441356 | -0.023656 | 297 | -0.412241 | 0.018182 |
| 2 | 294 | -0.273428 | -0.08784 | 296 | -0.26525 | -0.005068 |
| 3 | 294 | -0.18579 | -0.011378 | 296 | -0.180915 | -0.046993 |
| 4 | 294 | -0.116097 | -0.011871 | 296 | -0.117305 | 0.033159 |
| 5 | 294 | -0.05699 | -0.005714 | 296 | -0.062534 | -0.041993 |
| 6 | 294 | -0.002114 | -0.040425 | 297 | -0.001417 | 0.004832 |
| 7 | 294 | 0.05648 | -0.075544 | 296 | 0.055127 | -0.103649 |
| 8 | 294 | 0.12715 | -0.05318 | 296 | 0.117701 | -0.036182 |
| 9 | 294 | 0.213902 | 0.059439 | 296 | 0.203095 | -0.059882 |
| 10 | 294 | 0.379908 | -0.047245 | 296 | 0.353581 | -0.083277 |

## High-edge calibration

| League | Threshold | n | Avg edge | ROI | P(WIN) pred | WIN actual | P(LOSS) pred | LOSS actual |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| PREMIER_LEAGUE | 0.1 | 851 | 0.245616 | -0.015417 | 0.553973 | 0.40188 | 0.269844 | 0.378378 |
| PREMIER_LEAGUE | 0.2 | 474 | 0.324177 | -0.021962 | 0.601874 | 0.394515 | 0.235229 | 0.383966 |
| PREMIER_LEAGUE | 0.3 | 238 | 0.402628 | 0.015378 | 0.646961 | 0.415966 | 0.205045 | 0.37395 |
| BUNDESLIGA | 0.1 | 806 | 0.238341 | -0.074026 | 0.554189 | 0.394541 | 0.27812 | 0.423077 |
| BUNDESLIGA | 0.2 | 449 | 0.310711 | -0.080523 | 0.594763 | 0.389755 | 0.247929 | 0.420935 |
| BUNDESLIGA | 0.3 | 199 | 0.392041 | -0.106533 | 0.636867 | 0.371859 | 0.214208 | 0.432161 |

## AH family

| Family | PL n | PL avg edge | PL ROI | BL n | BL avg edge | BL ROI |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| NEGATIVE_HANDICAP | 1310 | -0.04501 | -0.055611 | 1314 | -0.009862 | -0.064098 |
| ZERO | 320 | -0.02485 | -0.027844 | 334 | -0.025547 | -0.021287 |
| POSITIVE_HANDICAP | 1310 | -0.015874 | -0.004336 | 1314 | -0.053827 | -0.002755 |

## HOME / AWAY

| Side | PL n | PL ROI | BL n | BL ROI |
| --- | ---: | ---: | ---: | ---: |
| HOME | 1470 | -0.033347 | 1481 | -0.033025 |
| AWAY | 1470 | -0.026136 | 1481 | -0.03109 |

## Season stability

- Premier League missing evaluation seasons: 2021/22
- Bundesliga missing evaluation seasons: none

Bundesliga includes 2021/22; Premier League does not. Do not pool first and infer stability.

| League | Season | n | +EV n | Avg edge | ROI | >=3% ROI | >=10% ROI |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| PREMIER_LEAGUE | 2019/20 | 608 | 276 | -0.029664 | -0.030748 | -0.034607 | -0.018152 |
| PREMIER_LEAGUE | 2020/21 | 872 | 374 | -0.028067 | -0.02801 | 0.008677 | 0.022544 |
| PREMIER_LEAGUE | 2022/23 | 720 | 322 | -0.029422 | -0.030736 | -0.026713 | -0.021202 |
| PREMIER_LEAGUE | 2023/24 | 740 | 334 | -0.032454 | -0.029986 | -0.033923 | -0.045286 |
| BUNDESLIGA | 2019/20 | 592 | 270 | -0.029564 | -0.033041 | 0.008246 | 0.048892 |
| BUNDESLIGA | 2020/21 | 592 | 262 | -0.030881 | -0.031318 | -0.114413 | -0.153836 |
| BUNDESLIGA | 2021/22 | 574 | 268 | -0.028936 | -0.02973 | -0.125498 | -0.077313 |
| BUNDESLIGA | 2022/23 | 612 | 271 | -0.032014 | -0.031275 | -0.041702 | -0.092931 |
| BUNDESLIGA | 2023/24 | 592 | 256 | -0.034181 | -0.034882 | -0.080996 | -0.1085 |

## Market overround

MARKET-IMPLIED REFERENCE. Not ground-truth probability and not the SafeEdge model.

| League | Mean overround | Median overround |
| --- | ---: | ---: |
| PREMIER_LEAGUE | 0.035373 | n/a |
| BUNDESLIGA | 0.037353 | 0.036965 |

Premier League median overround was not published in Baseline 002; mean and season means are shown.

| League | Season | Events | Avg overround |
| --- | --- | ---: | ---: |
| PREMIER_LEAGUE | 2019/20 | 304 | 0.033883 |
| PREMIER_LEAGUE | 2020/21 | 436 | 0.033866 |
| PREMIER_LEAGUE | 2022/23 | 360 | 0.035578 |
| PREMIER_LEAGUE | 2023/24 | 370 | 0.038175 |
| BUNDESLIGA | 2019/20 | 296 | 0.035916 |
| BUNDESLIGA | 2020/21 | 296 | 0.037328 |
| BUNDESLIGA | 2021/22 | 287 | 0.034891 |
| BUNDESLIGA | 2022/23 | 306 | 0.037649 |
| BUNDESLIGA | 2023/24 | 296 | 0.040899 |

## Strategy results

Unchanged StrategyPresetFactory configs. Not a league-selection exercise.

| Strategy | PL bets | PL ROI | PL paused | BL bets | BL ROI | BL paused |
| --- | ---: | ---: | --- | ---: | ---: | --- |
| DEFENSIVE | 117 | -0.030255 | true | 194 | -0.034371 | true |
| BALANCED | 122 | -0.021072 | true | 202 | -0.022986 | true |
| GROWTH | 123 | -0.021641 | true | 109 | -0.047705 | true |
| FLAT_STAKE | 588 | -0.038249 | true | 375 | -0.028569 | true |

## Statistical uncertainty

Deterministic bootstrap seed=20260816, replicates=2000. CI excluding 0 is not proof of future profitability.

| Group | PL mean | PL 95% | BL mean | BL 95% |
| --- | ---: | --- | ---: | --- |
| all candidates | -0.029741 | [-0.062548, 0.002031] | -0.032058 | [-0.065289, -0.00015] |
| positive-edge | -0.037802 | [-0.082366, 0.011765] | -0.059846 | [-0.107826, -0.010799] |
| edge >= 0.10 | -0.015417 | [-0.073519, 0.043637] | -0.074026 | [-0.1316, -0.011476] |
| NEGATIVE_HANDICAP | -0.055611 | [-0.10271, -0.007439] | -0.064098 | [-0.110495, -0.013505] |
| POSITIVE_HANDICAP | -0.004336 | [-0.053, 0.042573] | -0.002755 | [-0.053189, 0.043569] |

## High-edge forensics (qualitative)

Inspection only. Outcomes must not define a filter.

Premier League Baseline 002 top-30 included several 50–90% predicted WIN candidates that settled LOSS or HALF_LOSS (for example Norwich vs Chelsea HOME +0.75, Southampton vs Nott'm Forest HOME −0.5).

Bundesliga top predicted-edge rows:

| Date | Event | Side | Line | Odds | P(WIN) | Edge | Settlement | Unit return |
| --- | --- | --- | ---: | ---: | ---: | ---: | --- | ---: |
| 2019-10-20 | FC Koln vs Paderborn | AWAY | 0.75 | 2.03 | 0.850474 | 0.7779 | LOSS | -1 |
| 2023-09-17 | Darmstadt vs M'gladbach | AWAY | -0.25 | 2 | 0.86386 | 0.767029 | HALF_LOSS | -0.5 |
| 2019-09-28 | Paderborn vs Bayern Munich | AWAY | -2.5 | 1.95 | 0.898543 | 0.752158 | LOSS | -1 |
| 2021-12-11 | Bochum vs Dortmund | HOME | 1 | 2.1 | 0.718742 | 0.687047 | WIN | 1.1 |
| 2022-03-05 | Bochum vs Greuther Furth | HOME | -0.75 | 2.01 | 0.666232 | 0.611737 | HALF_WIN | 0.505 |
| 2019-09-15 | Paderborn vs Schalke 04 | AWAY | -0.25 | 1.98 | 0.770329 | 0.596358 | WIN | 0.98 |
| 2020-06-27 | Werder Bremen vs FC Koln | AWAY | 1 | 1.87 | 0.77287 | 0.589021 | LOSS | -1 |
| 2022-02-20 | Bayern Munich vs Greuther Furth | HOME | -3 | 1.85 | 0.803357 | 0.584505 | PUSH | 0 |
| 2023-11-25 | Freiburg vs Darmstadt | HOME | -1 | 1.94 | 0.741336 | 0.58261 | LOSS | -1 |
| 2021-12-15 | Dortmund vs Greuther Furth | HOME | -2.5 | 1.88 | 0.837561 | 0.574614 | WIN | 0.88 |
| 2021-02-13 | Union Berlin vs Schalke 04 | HOME | -0.75 | 2.02 | 0.628968 | 0.561977 | LOSS | -1 |
| 2019-11-08 | FC Koln vs Hoffenheim | AWAY | 0 | 1.96 | 0.711146 | 0.555305 | WIN | 0.96 |
| 2021-04-10 | Bayern Munich vs Union Berlin | HOME | -1.25 | 2.04 | 0.723649 | 0.546982 | LOSS | -1 |
| 2019-12-18 | Freiburg vs Bayern Munich | HOME | 2 | 1.93 | 0.719028 | 0.541115 | PUSH | 0 |
| 2023-02-10 | Schalke 04 vs Wolfsburg | AWAY | -0.5 | 1.97 | 0.780181 | 0.536957 | LOSS | -1 |
| 2023-09-22 | Stuttgart vs Darmstadt | HOME | -1.5 | 2.04 | 0.751887 | 0.533849 | WIN | 1.04 |
| 2020-06-06 | RB Leipzig vs Paderborn | AWAY | 2.25 | 2.08 | 0.575991 | 0.529808 | WIN | 1.08 |
| 2020-01-26 | Werder Bremen vs Hoffenheim | AWAY | 0 | 2.06 | 0.654608 | 0.525306 | WIN | 1.06 |
| 2019-08-31 | Freiburg vs FC Koln | HOME | -0.25 | 2.12 | 0.677335 | 0.524665 | LOSS | -1 |
| 2020-10-24 | Dortmund vs Schalke 04 | HOME | -2 | 1.98 | 0.690614 | 0.522995 | WIN | 0.98 |
| 2021-12-04 | Augsburg vs Bochum | HOME | -0.25 | 2.02 | 0.702528 | 0.520874 | LOSS | -1 |
| 2020-06-27 | Union Berlin vs Fortuna Dusseldorf | HOME | 0.5 | 1.9 | 0.800368 | 0.520698 | WIN | 0.9 |
| 2019-09-14 | FC Koln vs M'gladbach | AWAY | 0 | 1.98 | 0.666537 | 0.514385 | WIN | 0.98 |
| 2023-09-02 | Leverkusen vs Darmstadt | AWAY | 2 | 1.93 | 0.670813 | 0.508138 | LOSS | -1 |
| 2021-10-30 | Union Berlin vs Bayern Munich | HOME | 1.5 | 1.99 | 0.753349 | 0.499165 | LOSS | -1 |
| 2019-10-26 | Paderborn vs Fortuna Dusseldorf | AWAY | 0.25 | 1.81 | 0.71534 | 0.491221 | LOSS | -1 |
| 2022-08-20 | Wolfsburg vs Schalke 04 | HOME | -0.5 | 1.84 | 0.806154 | 0.483324 | LOSS | -1 |
| 2019-11-23 | Fortuna Dusseldorf vs Bayern Munich | HOME | 2 | 2.07 | 0.629186 | 0.481433 | LOSS | -1 |
| 2020-10-25 | Werder Bremen vs Hoffenheim | AWAY | -0.25 | 1.96 | 0.711138 | 0.479091 | HALF_LOSS | -0.5 |
| 2020-02-16 | FC Koln vs Bayern Munich | HOME | 2 | 1.93 | 0.678883 | 0.478733 | LOSS | -1 |

- Bundesliga top-30 rows with P(WIN) ≥ 0.50 that settled LOSS: 15

## Structural-error test

Diagnostic cutoffs (not production filters): |1X2 gap| ≤ 0.03; |goal gap| ≤ 0.15; |edge−return gap| ≤ 0.01; |Spearman| < 0.10; high-edge ≥10% and ≥20% have predicted WIN above actual and predicted LOSS below actual; edge buckets with n≥30 are not monotone in ROI.

| Pattern | PREMIER_LEAGUE | BUNDESLIGA |
| --- | --- | --- |
| A) aggregate goals / 1X2 reasonably calibrated | yes | no |
| B) aggregate predicted edge near realized return | yes | yes |
| C) edge ranking near zero / weak | yes | yes |
| D) high predicted edge: P(WIN) too high, P(LOSS) too low | yes | yes |
| E) higher edge does not monotonically improve ROI | yes | yes |

Two leagues are not a proof. This is a replication check.

## Classification

**FAILURE STRONGLY REPLICATES**

Both leagues show weak edge ranking, high-edge settlement overconfidence, and the same qualitative pattern.

### NEXT HYPOTHESIS

Independent-Poisson score tails / dependence assumptions may be too confident for AH value ranking.

Possible later experiments (not implemented): Dixon-Coles; shrinkage / regularization of team strength; richer team-strength model; Elo/xG.

## Explicit non-conclusions

- no parameter optimization performed
- no production filter selected
- Bundesliga is not selected as a betting venue because of this comparison
- best-looking cell is not a validated strategy
- two leagues do not prove a theorem
- CI excluding 0 is not proof of future profitability
- MARKET_AVERAGE is not Tippmix
- 1/odds is not true AH probability
- football-data.co.uk historical quotes only

