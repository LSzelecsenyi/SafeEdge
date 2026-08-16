# Baseline 001 Diagnostics

## Experiment configuration

- Competition: PREMIER_LEAGUE
- Training from season: 2014
- Evaluation range: 2019 → 2023
- HISTORICAL QUOTE SOURCE = MARKET_AVERAGE
- These prices are football-data.co.uk historical quotes, not Tippmix odds.
- Model: current independent time-decayed Poisson defaults (not retuned).
- Strategies: unchanged DEFENSIVE / BALANCED / GROWTH / FLAT_STAKE.
- Diagnostics used the already-prepared walk-forward candidates and one backtest per strategy.
- Unit-stake ROI is a diagnostic (stake = 1 on every candidate in the group). It is not a production strategy.

## Candidate overview

- Matches loaded: 3420
- Matches evaluated: 1520
- Predictions available: 1470
- Candidates generated (dataset): 2940
- Candidates analyzed: 2940
- Positive / zero / negative EV: 1306 / 0 / 1634
- Average candidate edge (dataset): -0.029834
- Average unit-stake realized return (all candidates): -0.029741
- Calibration gap (realized − predicted): 0.000092
- Negative-edge unit-stake ROI: -0.023299 (n=1634)
- Positive-edge unit-stake ROI: -0.037802 (n=1306)

Positive-EV concentration (share of edge > 0 candidates):

- HOME/AWAY:
  - HOME: 677 (51.8%)
  - AWAY: 629 (48.2%)
- AH line:
  - -3.0000: 1 (0.1%)
  - -2.7500: 2 (0.2%)
  - -2.5000: 9 (0.7%)
  - -2.2500: 7 (0.5%)
  - -2.0000: 20 (1.5%)
  - -1.7500: 16 (1.2%)
  - -1.5000: 43 (3.3%)
  - -1.2500: 40 (3.1%)
  - -1.0000: 54 (4.1%)
  - -0.7500: 78 (6.0%)
  - -0.5000: 103 (7.9%)
  - -0.2500: 156 (11.9%)
  - 0.0000: 144 (11.0%)
  - 0.2500: 152 (11.6%)
  - 0.5000: 107 (8.2%)
  - 0.7500: 112 (8.6%)
  - 1.0000: 87 (6.7%)
  - 1.2500: 55 (4.2%)
  - 1.5000: 51 (3.9%)
  - 1.7500: 30 (2.3%)
  - 2.0000: 16 (1.2%)
  - 2.2500: 9 (0.7%)
  - 2.5000: 10 (0.8%)
  - 2.7500: 2 (0.2%)
  - 3.0000: 2 (0.2%)
- Odds bucket:
  - 1.00 < odds < 1.20: 0 (0.0%)
  - 1.20 <= odds < 1.35: 1 (0.1%)
  - 1.35 <= odds < 1.50: 0 (0.0%)
  - 1.50 <= odds < 1.75: 0 (0.0%)
  - 1.75 <= odds < 2.00: 929 (71.1%)
  - 2.00 <= odds < 2.50: 376 (28.8%)
  - odds >= 2.50: 0 (0.0%)
- Season:
  - 2019/20: 332 (25.4%)
  - 2020/21: 318 (24.3%)
  - 2022/23: 322 (24.7%)
  - 2023/24: 334 (25.6%)

## Edge calibration

| Bucket | Count | Avg edge | Avg odds | WIN | HALF_WIN | PUSH | HALF_LOSS | LOSS | Avg realized | Unit-stake ROI | Gap |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| edge <= 0 | 1634 | -0.194579 | 1.934119 | 647 | 132 | 103 | 104 | 648 | -0.023299 | -0.023299 | 0.17128 |
| 0 < edge < 0.02 | 107 | 0.010497 | 1.939159 | 41 | 7 | 4 | 8 | 47 | -0.092944 | -0.092944 | -0.103441 |
| 0.02 <= edge < 0.05 | 155 | 0.036024 | 1.923226 | 54 | 12 | 10 | 14 | 65 | -0.109581 | -0.109581 | -0.145604 |
| 0.05 <= edge < 0.10 | 193 | 0.075159 | 1.937927 | 75 | 12 | 14 | 15 | 77 | -0.04829 | -0.04829 | -0.123449 |
| edge >= 0.10 | 851 | 0.245616 | 1.940188 | 342 | 52 | 61 | 74 | 322 | -0.015417 | -0.015417 | -0.261033 |

## Positive-edge threshold diagnostics

Diagnostic thresholds only. Not StrategyConfig.

| Subset | Count | Avg edge | Unit-stake ROI |
| --- | ---: | ---: | ---: |
| edge > 0 | 1306 | 0.176287 | -0.037802 |
| edge >= 0.02 | 1199 | 0.191083 | -0.032882 |
| edge >= 0.03 | 1150 | 0.19812 | -0.020235 |
| edge >= 0.05 | 1044 | 0.214104 | -0.021494 |
| edge >= 0.10 | 851 | 0.245616 | -0.015417 |

## Odds buckets

| Bucket | Count | +EV | Avg edge | Unit-stake ROI |
| --- | ---: | ---: | ---: | ---: |
| 1.00 < odds < 1.20 | 0 | 0 | n/a | n/a |
| 1.20 <= odds < 1.35 | 1 | 1 | 0.11017 | 0.32 |
| 1.35 <= odds < 1.50 | 0 | 0 | n/a | n/a |
| 1.50 <= odds < 1.75 | 0 | 0 | n/a | n/a |
| 1.75 <= odds < 2.00 | 2113 | 929 | -0.032091 | -0.033888 |
| 2.00 <= odds < 2.50 | 825 | 376 | -0.023697 | -0.01837 |
| odds >= 2.50 | 1 | 0 | -0.462708 | -1 |

## AH lines

Exact selected-side handicap. Quarter-lines are not merged.

| Line | Count | +EV | Avg edge | Avg odds | Unit-stake ROI |
| --- | ---: | ---: | ---: | ---: | ---: |
| -3 | 3 | 1 | -0.095437 | 2.02 | -0.016667 |
| -2.75 | 5 | 2 | -0.039006 | 1.918 | -0.236 |
| -2.5 | 21 | 9 | -0.073932 | 2.013333 | -0.267143 |
| -2.25 | 17 | 7 | -0.007056 | 1.939412 | -0.25 |
| -2 | 40 | 20 | -0.033871 | 1.94375 | -0.11275 |
| -1.75 | 48 | 16 | -0.084902 | 1.932708 | -0.391354 |
| -1.5 | 109 | 43 | -0.056607 | 1.951101 | 0.004954 |
| -1.25 | 107 | 40 | -0.044383 | 1.958224 | -0.04486 |
| -1 | 157 | 54 | -0.078985 | 1.94 | 0.071975 |
| -0.75 | 211 | 78 | -0.073095 | 1.934265 | -0.041872 |
| -0.5 | 238 | 103 | -0.018071 | 1.954958 | -0.002815 |
| -0.25 | 354 | 156 | -0.023546 | 1.953446 | -0.101695 |
| 0 | 320 | 144 | -0.02485 | 1.935469 | -0.027844 |
| 0.25 | 354 | 152 | -0.036021 | 1.917599 | 0.03702 |
| 0.5 | 238 | 107 | -0.049257 | 1.918739 | -0.068613 |
| 0.75 | 211 | 112 | 0.012615 | 1.934171 | -0.009716 |
| 1 | 157 | 87 | 0.029143 | 1.931847 | -0.119108 |
| 1.25 | 107 | 55 | -0.016412 | 1.912897 | -0.008318 |
| 1.5 | 109 | 51 | -0.008416 | 1.920917 | -0.063578 |
| 1.75 | 48 | 30 | 0.024564 | 1.934583 | 0.333125 |
| 2 | 40 | 16 | -0.022046 | 1.923 | 0.072 |
| 2.25 | 17 | 9 | -0.061615 | 1.925882 | 0.202647 |
| 2.5 | 21 | 10 | -0.006064 | 1.895238 | 0.152381 |
| 2.75 | 5 | 2 | -0.017711 | 1.948 | 0.158 |
| 3 | 3 | 2 | 0.027129 | 1.853333 | -0.063333 |

Line-family summary:

| Family | Count | +EV | Avg edge | Unit-stake ROI |
| --- | ---: | ---: | ---: | ---: |
| NEGATIVE_HANDICAP | 1310 | 529 | -0.04501 | -0.055611 |
| ZERO | 320 | 144 | -0.02485 | -0.027844 |
| POSITIVE_HANDICAP | 1310 | 633 | -0.015874 | -0.004336 |

## HOME vs AWAY

| Side | Count | +EV | Avg edge | Avg odds | WIN | HALF_WIN | PUSH | HALF_LOSS | LOSS | Unit-stake ROI |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| HOME | 1470 | 677 | -0.025542 | 1.939633 | 578 | 103 | 96 | 112 | 581 | -0.033347 |
| AWAY | 1470 | 629 | -0.034125 | 1.931837 | 581 | 112 | 96 | 103 | 578 | -0.026136 |

## Seasons

| Season | Predictions | Candidates | +EV | Avg edge | Unit-stake ROI |
| --- | ---: | ---: | ---: | ---: | ---: |
| 2019/20 | 370 | 740 | 332 | -0.029838 | -0.030243 |
| 2020/21 | 370 | 740 | 318 | -0.027609 | -0.028027 |
| 2022/23 | 360 | 720 | 322 | -0.029422 | -0.030736 |
| 2023/24 | 370 | 740 | 334 | -0.032454 | -0.029986 |

## Goal / margin calibration

No odds involved. Predicted values are sums of the captured score distribution.

- Predictions: 1470
- Predicted vs actual home goals: 1.562478 vs 1.57551
- Predicted vs actual away goals: 1.262254 vs 1.313605
- Predicted vs actual total goals: 2.824733 vs 2.889116
- Predicted vs actual home-win: 0.443058 vs 0.446939
- Predicted vs actual draw: 0.225691 vs 0.22381
- Predicted vs actual away-win: 0.331251 vs 0.329252

| Margin category | Predicted | Actual frequency | Actual count |
| --- | ---: | ---: | ---: |
| HOME_WIN_BY_2_PLUS | 0.248631 | 0.241497 | 355 |
| HOME_WIN_BY_1 | 0.194427 | 0.205442 | 302 |
| DRAW | 0.225691 | 0.22381 | 329 |
| AWAY_WIN_BY_1 | 0.167237 | 0.156463 | 230 |
| AWAY_WIN_BY_2_PLUS | 0.164015 | 0.172789 | 254 |

| Exact home margin | Predicted | Actual frequency | Actual count |
| --- | ---: | ---: | ---: |
| <= -3 | 0.07079 | 0.067347 | 99 |
| -2 | 0.093225 | 0.105442 | 155 |
| -1 | 0.167237 | 0.156463 | 230 |
| 0 | 0.225691 | 0.22381 | 329 |
| +1 | 0.194427 | 0.205442 | 302 |
| +2 | 0.125416 | 0.130612 | 192 |
| >= +3 | 0.123214 | 0.110884 | 163 |

## Edge quantiles

Nearest-rank: index = round_half_up(p × (n − 1)).

| Group | min | p10 | p25 | median | p75 | p90 | p95 | p99 | max |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| All candidates | -0.842468 | -0.334184 | -0.181998 | -0.028913 | 0.125635 | 0.270379 | 0.358401 | 0.508579 | 0.700431 |
| Positive-EV only | 0.000053 | 0.026557 | 0.067489 | 0.148115 | 0.256655 | 0.374411 | 0.443286 | 0.556138 | 0.700431 |

## Original 1.15–1.35 hypothesis

Diagnostic subset only. Not a production filter.

| Subset | Count | Avg edge | Unit-stake ROI | WIN | HALF_WIN | PUSH | HALF_LOSS | LOSS |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| odds 1.15-1.35 | 1 | 0.11017 | 0.32 | 1 | 0 | 0 | 0 | 0 |
| odds 1.15-1.35 AND edge > 0 | 1 | 0.11017 | 0.32 | 1 | 0 | 0 | 0 | 0 |
| odds 1.15-1.35 AND edge >= 0.03 | 1 | 0.11017 | 0.32 | 1 | 0 | 0 | 0 | 0 |

## Strategy accepted-bet composition

Denominator is StrategyEngine accepted bets only. Unit-stake ROI is stake=1 on those same identities.

### DEFENSIVE

- Accepted bets: 117
- Average edge of accepted identities: 0.193621
- Unit-stake ROI of accepted identities: -0.053248
- HOME count: 64
- AWAY count: 53

Accepted by edge bucket:

| Bucket | Count | Avg edge | Unit-stake ROI |
| --- | ---: | ---: | ---: |
| edge <= 0 | 0 | n/a | n/a |
| 0 < edge < 0.02 | 0 | n/a | n/a |
| 0.02 <= edge < 0.05 | 17 | 0.040924 | 0.017647 |
| 0.05 <= edge < 0.10 | 23 | 0.070645 | -0.254348 |
| edge >= 0.10 | 77 | 0.264067 | -0.008831 |

Accepted by odds bucket:

| Bucket | Count | Unit-stake ROI |
| --- | ---: | ---: |
| 1.75 <= odds < 2.00 | 90 | -0.073833 |
| 2.00 <= odds < 2.50 | 27 | 0.01537 |

Accepted by AH line:

| Line | Count | Unit-stake ROI |
| --- | ---: | ---: |
| -2.25 | 1 | 0.94 |
| -2 | 1 | 0 |
| -1.75 | 1 | -1 |
| -1.5 | 2 | -0.035 |
| -1.25 | 3 | -0.5 |
| -1 | 3 | -0.333333 |
| -0.75 | 8 | -0.2725 |
| -0.5 | 3 | 0.306667 |
| -0.25 | 19 | -0.035263 |
| 0 | 12 | 0.1325 |
| 0.25 | 20 | 0.1705 |
| 0.5 | 9 | -0.145556 |
| 0.75 | 11 | -0.607273 |
| 1 | 1 | -1 |
| 1.25 | 5 | -0.715 |
| 1.5 | 7 | 0.674286 |
| 1.75 | 4 | -0.0125 |
| 2 | 3 | 0.326667 |
| 2.25 | 1 | 0.435 |
| 2.5 | 1 | -1 |
| 3 | 2 | 0.405 |

### BALANCED

- Accepted bets: 122
- Average edge of accepted identities: 0.191808
- Unit-stake ROI of accepted identities: -0.076885
- HOME count: 67
- AWAY count: 55

Accepted by edge bucket:

| Bucket | Count | Avg edge | Unit-stake ROI |
| --- | ---: | ---: | ---: |
| edge <= 0 | 0 | n/a | n/a |
| 0 < edge < 0.02 | 0 | n/a | n/a |
| 0.02 <= edge < 0.05 | 18 | 0.041097 | -0.038889 |
| 0.05 <= edge < 0.10 | 24 | 0.070324 | -0.285417 |
| edge >= 0.10 | 80 | 0.262163 | -0.022875 |

Accepted by odds bucket:

| Bucket | Count | Unit-stake ROI |
| --- | ---: | ---: |
| 1.75 <= odds < 2.00 | 93 | -0.083817 |
| 2.00 <= odds < 2.50 | 29 | -0.054655 |

Accepted by AH line:

| Line | Count | Unit-stake ROI |
| --- | ---: | ---: |
| -2.25 | 1 | 0.94 |
| -2 | 2 | -0.5 |
| -1.75 | 1 | -1 |
| -1.5 | 3 | -0.356667 |
| -1.25 | 3 | -0.5 |
| -1 | 3 | -0.333333 |
| -0.75 | 8 | -0.2725 |
| -0.5 | 4 | 0.4425 |
| -0.25 | 19 | -0.035263 |
| 0 | 12 | 0.1325 |
| 0.25 | 20 | 0.1705 |
| 0.5 | 11 | -0.300909 |
| 0.75 | 11 | -0.607273 |
| 1 | 1 | -1 |
| 1.25 | 5 | -0.715 |
| 1.5 | 7 | 0.674286 |
| 1.75 | 4 | -0.0125 |
| 2 | 3 | 0.326667 |
| 2.25 | 1 | 0.435 |
| 2.5 | 1 | -1 |
| 3 | 2 | 0.405 |

### GROWTH

- Accepted bets: 123
- Average edge of accepted identities: 0.18774
- Unit-stake ROI of accepted identities: -0.057561
- HOME count: 67
- AWAY count: 56

Accepted by edge bucket:

| Bucket | Count | Avg edge | Unit-stake ROI |
| --- | ---: | ---: | ---: |
| edge <= 0 | 0 | n/a | n/a |
| 0 < edge < 0.02 | 0 | n/a | n/a |
| 0.02 <= edge < 0.05 | 21 | 0.037921 | -0.087619 |
| 0.05 <= edge < 0.10 | 23 | 0.070645 | -0.254348 |
| edge >= 0.10 | 79 | 0.261656 | 0.007722 |

Accepted by odds bucket:

| Bucket | Count | Unit-stake ROI |
| --- | ---: | ---: |
| 1.20 <= odds < 1.35 | 1 | 0.32 |
| 1.75 <= odds < 2.00 | 93 | -0.062527 |
| 2.00 <= odds < 2.50 | 29 | -0.054655 |

Accepted by AH line:

| Line | Count | Unit-stake ROI |
| --- | ---: | ---: |
| -2.25 | 1 | 0.94 |
| -2 | 1 | 0 |
| -1.75 | 1 | -1 |
| -1.5 | 2 | -0.035 |
| -1.25 | 3 | -0.5 |
| -1 | 3 | -0.333333 |
| -0.75 | 9 | -0.353333 |
| -0.5 | 6 | -0.018333 |
| -0.25 | 19 | -0.035263 |
| 0 | 12 | 0.1325 |
| 0.25 | 20 | 0.1705 |
| 0.5 | 10 | -0.045 |
| 0.75 | 11 | -0.607273 |
| 1 | 1 | -1 |
| 1.25 | 5 | -0.715 |
| 1.5 | 7 | 0.674286 |
| 1.75 | 4 | -0.0125 |
| 2 | 3 | 0.326667 |
| 2.25 | 1 | 0.435 |
| 2.5 | 2 | -0.34 |
| 3 | 2 | 0.405 |

### FLAT_STAKE

- Accepted bets: 588
- Average edge of accepted identities: 0.192119
- Unit-stake ROI of accepted identities: -0.018571
- HOME count: 287
- AWAY count: 301

Accepted by edge bucket:

| Bucket | Count | Avg edge | Unit-stake ROI |
| --- | ---: | ---: | ---: |
| edge <= 0 | 0 | n/a | n/a |
| 0 < edge < 0.02 | 0 | n/a | n/a |
| 0.02 <= edge < 0.05 | 63 | 0.040571 | -0.077381 |
| 0.05 <= edge < 0.10 | 115 | 0.075444 | -0.021739 |
| edge >= 0.10 | 410 | 0.248131 | -0.008646 |

Accepted by odds bucket:

| Bucket | Count | Unit-stake ROI |
| --- | ---: | ---: |
| 1.75 <= odds < 2.00 | 404 | -0.054864 |
| 2.00 <= odds < 2.50 | 184 | 0.061114 |

Accepted by AH line:

| Line | Count | Unit-stake ROI |
| --- | ---: | ---: |
| -2.5 | 3 | -0.37 |
| -2.25 | 3 | -0.353333 |
| -2 | 6 | 0.006667 |
| -1.75 | 6 | -1 |
| -1.5 | 18 | -0.011111 |
| -1.25 | 19 | -0.247895 |
| -1 | 20 | 0.23 |
| -0.75 | 32 | 0.239531 |
| -0.5 | 40 | 0.0825 |
| -0.25 | 66 | -0.195606 |
| 0 | 60 | 0.1315 |
| 0.25 | 82 | 0.032622 |
| 0.5 | 51 | -0.025882 |
| 0.75 | 55 | 0.011273 |
| 1 | 46 | -0.134565 |
| 1.25 | 23 | -0.307609 |
| 1.5 | 26 | 0.055 |
| 1.75 | 13 | 0.098462 |
| 2 | 7 | 0.095714 |
| 2.25 | 4 | -0.27875 |
| 2.5 | 5 | 0.158 |
| 2.75 | 1 | -1 |
| 3 | 2 | 0.405 |

## Drawdown stop details

### DEFENSIVE

- Paused: true
- Opportunity index (0-based, first skipped-by-pause): 397
- Pause betting date: 2020-01-02
- Pause decisionAt: 2020-01-02T00:00:00Z
- Accepted bets before pause: 117
- Active bankroll at pause: 89135.52084
- Active drawdown at pause: 0.203984
- Total equity at pause: 94268.52692

Last settled bets before pause:

| Date | Side | Line | Odds | Edge | Settlement | Profit |
| --- | --- | ---: | ---: | ---: | --- | ---: |
| 2019-12-28 | AWAY | 0.5 | 1.88 | 0.195942 | LOSS | -944.275883 |
| 2019-12-28 | AWAY | -0.75 | 1.85 | 0.145445 | LOSS | -944.275883 |
| 2019-12-29 | HOME | 0.25 | 1.81 | 0.064584 | LOSS | -914.908903 |
| 2019-12-29 | AWAY | 1.5 | 2.04 | 0.047742 | WIN | 545.998565 |
| 2019-12-29 | AWAY | 2 | 1.98 | 0.655662 | PUSH | 0 |
| 2020-01-01 | AWAY | 0.75 | 1.82 | 0.072362 | WIN | 747.200236 |
| 2020-01-01 | HOME | 0.75 | 2 | 0.094318 | LOSS | -911.2198 |
| 2020-01-01 | AWAY | -0.25 | 1.87 | 0.288277 | LOSS | -911.2198 |
| 2020-01-01 | HOME | -1.75 | 1.95 | 0.14233 | LOSS | -911.2198 |
| 2020-01-01 | AWAY | 0 | 2.02 | 0.350998 | PUSH | 0 |

### BALANCED

- Paused: true
- Opportunity index (0-based, first skipped-by-pause): 397
- Pause betting date: 2020-01-02
- Pause decisionAt: 2020-01-02T00:00:00Z
- Accepted bets before pause: 122
- Active bankroll at pause: 89871.177371
- Active drawdown at pause: 0.259487
- Total equity at pause: 93641.19446

Last settled bets before pause:

| Date | Side | Line | Odds | Edge | Settlement | Profit |
| --- | --- | ---: | ---: | ---: | --- | ---: |
| 2019-12-28 | AWAY | -0.5 | 1.85 | 0.259938 | WIN | 417.263403 |
| 2019-12-29 | HOME | 0.25 | 1.81 | 0.064584 | LOSS | -1410.252124 |
| 2019-12-29 | AWAY | 1.5 | 2.04 | 0.047742 | WIN | 785.501804 |
| 2019-12-29 | AWAY | 2 | 1.98 | 0.655662 | PUSH | 0 |
| 2020-01-01 | AWAY | 0.75 | 1.82 | 0.072362 | WIN | 1148.722313 |
| 2020-01-01 | HOME | 0.75 | 2 | 0.094318 | LOSS | -1400.880869 |
| 2020-01-01 | AWAY | -0.25 | 1.87 | 0.288277 | LOSS | -1400.880869 |
| 2020-01-01 | HOME | -1.75 | 1.95 | 0.14233 | LOSS | -1400.880869 |
| 2020-01-01 | AWAY | 0 | 2.02 | 0.350998 | PUSH | 0 |
| 2020-01-01 | AWAY | 0.5 | 1.92 | 0.106388 | LOSS | -466.96029 |

### GROWTH

- Paused: true
- Opportunity index (0-based, first skipped-by-pause): 397
- Pause betting date: 2020-01-02
- Pause decisionAt: 2020-01-02T00:00:00Z
- Accepted bets before pause: 123
- Active bankroll at pause: 91238.416851
- Active drawdown at pause: 0.315115
- Total equity at pause: 91238.416851

Last settled bets before pause:

| Date | Side | Line | Odds | Edge | Settlement | Profit |
| --- | --- | ---: | ---: | ---: | --- | ---: |
| 2019-12-28 | AWAY | -0.75 | 1.85 | 0.145445 | LOSS | -2066.333298 |
| 2019-12-29 | HOME | 0.25 | 1.81 | 0.064584 | LOSS | -1937.807367 |
| 2019-12-29 | AWAY | 1.5 | 2.04 | 0.047742 | WIN | 1156.443049 |
| 2019-12-29 | AWAY | 2 | 1.98 | 0.655662 | PUSH | 0 |
| 2020-01-01 | AWAY | -0.5 | 2 | 0.028311 | LOSS | -680.234594 |
| 2020-01-01 | AWAY | 0.75 | 1.82 | 0.072362 | WIN | 1576.187666 |
| 2020-01-01 | HOME | 0.75 | 2 | 0.094318 | LOSS | -1922.18008 |
| 2020-01-01 | AWAY | -0.25 | 1.87 | 0.288277 | LOSS | -1922.18008 |
| 2020-01-01 | HOME | -1.75 | 1.95 | 0.14233 | LOSS | -1922.18008 |
| 2020-01-01 | AWAY | 0 | 2.02 | 0.350998 | PUSH | 0 |

### FLAT_STAKE

- Paused: true
- Opportunity index (0-based, first skipped-by-pause): 1600
- Pause betting date: 2022-10-02
- Pause decisionAt: 2022-10-02T00:00:00Z
- Accepted bets before pause: 588
- Active bankroll at pause: 83511.656934
- Active drawdown at pause: 0.202117
- Total equity at pause: 83511.656934

Last settled bets before pause:

| Date | Side | Line | Odds | Edge | Settlement | Profit |
| --- | --- | ---: | ---: | ---: | --- | ---: |
| 2022-09-17 | HOME | 1.5 | 1.92 | 0.393869 | LOSS | -426.241418 |
| 2022-09-17 | HOME | -1.25 | 2.05 | 0.572368 | LOSS | -426.241418 |
| 2022-09-17 | HOME | -1.25 | 2.04 | 0.321666 | WIN | 443.291074 |
| 2022-09-18 | AWAY | -0.25 | 2.13 | 0.073856 | LOSS | -424.195459 |
| 2022-10-01 | AWAY | 0.5 | 1.82 | 0.152449 | LOSS | -422.074482 |
| 2022-10-01 | HOME | 0.5 | 1.97 | 0.14964 | LOSS | -422.074482 |
| 2022-10-01 | HOME | 0.25 | 1.84 | 0.092344 | LOSS | -422.074482 |
| 2022-10-01 | AWAY | 1.5 | 1.86 | 0.09581 | WIN | 362.984054 |
| 2022-10-01 | AWAY | 0.25 | 2 | 0.183822 | WIN | 422.074482 |
| 2022-10-01 | AWAY | 0.5 | 1.91 | 0.310471 | LOSS | -422.074482 |

## Observations

Facts only. No recommended threshold, preset, or proven edge.

- Analyzed candidate count equals dataset candidate count: 2940 vs 2940
- All-candidate average predicted edge: -0.029834; average realized unit return: -0.029741; gap: 0.000092
- Negative-edge n=1634 unit ROI=-0.023299; positive-edge n=1306 unit ROI=-0.037802
- Edge bucket edge <= 0: n=1634 avgEdge=-0.194579 realized=-0.023299
- Edge bucket 0 < edge < 0.02: n=107 avgEdge=0.010497 realized=-0.092944
- Edge bucket 0.02 <= edge < 0.05: n=155 avgEdge=0.036024 realized=-0.109581
- Edge bucket 0.05 <= edge < 0.10: n=193 avgEdge=0.075159 realized=-0.04829
- Edge bucket edge >= 0.10: n=851 avgEdge=0.245616 realized=-0.015417
- HOME: n=1470 +EV=677 unit ROI=-0.033347
- AWAY: n=1470 +EV=629 unit ROI=-0.026136
- Season 2019/20: predictions=370 candidates=740 unit ROI=-0.030243
- Season 2020/21: predictions=370 candidates=740 unit ROI=-0.028027
- Season 2022/23: predictions=360 candidates=720 unit ROI=-0.030736
- Season 2023/24: predictions=370 candidates=740 unit ROI=-0.029986
- Strategy DEFENSIVE: accepted=117 paused=true pauseDate=2020-01-02
- Strategy BALANCED: accepted=122 paused=true pauseDate=2020-01-02
- Strategy GROWTH: accepted=123 paused=true pauseDate=2020-01-02
- Strategy FLAT_STAKE: accepted=588 paused=true pauseDate=2022-10-02

## Explicit non-conclusions

- no parameter optimization performed
- best-looking bucket is not validated strategy
- historical pattern may be noise
- MARKET_AVERAGE is not Tippmix
- HISTORICAL QUOTE SOURCE prices are football-data.co.uk quotes, never Tippmix odds
