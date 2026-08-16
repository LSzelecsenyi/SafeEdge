# Baseline 004 – Three-League Structural Validation

## Experiment configuration

- Leagues: PREMIER_LEAGUE (published Baseline 001/002), BUNDESLIGA (published Baseline 003), SERIE_A (this replication).
- Training from season: 2014
- Evaluation range: 2019 → 2023
- HISTORICAL QUOTE SOURCE = MARKET_AVERAGE
- These prices are football-data.co.uk historical quotes, not Tippmix odds.
- Model: independent time-decayed Poisson defaults (not retuned).
- decayHalfLifeDays = 180
- maxGoalsPerTeam = 10
- minimumTeamMatches = 5
- No league-specific tuning. No production filter. Zero-tuning replication.
- Premier League and Bundesliga numbers are published baselines, not reruns.

## Dataset

| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |
| --- | ---: | ---: | ---: |
| Predictions | 1470 | 1481 | 1831 |
| Candidates | 2940 | 2962 | 3658 |
| Matches evaluated | 1520 | 1530 | 1900 |
| Matches skipped missing quote | 0 | 0 | 2 |
| Evaluation seasons present | 2019/20, 2020/21, 2022/23, 2023/24 | 2019/20, 2020/21, 2021/22, 2022/23, 2023/24 | 2019/20, 2020/21, 2021/22, 2022/23, 2023/24 |
| Missing evaluation seasons | 2021/22 | none | none |

## Aggregate calibration

| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |
| --- | ---: | ---: | ---: |
| Average predicted edge | -0.029834 | -0.031134 | -0.0316 |
| Realized unit ROI | -0.029741 | -0.032058 | -0.030197 |
| Calibration gap | 0.000092 | -0.000923 | 0.001403 |

## Ranking

| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |
| --- | ---: | ---: | ---: |
| Spearman | 0.0172 | -0.010761 | 0.009322 |
| Pearson | 0.012664 | -0.033968 | -0.000405 |

Single-bet realized return is noisy. Correlation is diagnostic, not proof.

## High edge >=10%

| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |
| --- | ---: | ---: | ---: |
| n | 851 | 806 | 894 |
| Avg edge | 0.245616 | 0.238341 | 0.220989 |
| ROI | -0.015417 | -0.074026 | -0.024597 |
| Predicted P(WIN) | 0.553973 | 0.554189 | 0.536873 |
| Actual P(WIN) | 0.40188 | 0.394541 | 0.395973 |
| Predicted P(LOSS) | 0.269844 | 0.27812 | 0.278866 |
| Actual P(LOSS) | 0.378378 | 0.423077 | 0.389262 |

## High edge >=20%

| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |
| --- | ---: | ---: | ---: |
| n | 474 | 449 | 423 |
| Avg edge | 0.324177 | 0.310711 | 0.302358 |
| ROI | -0.021962 | -0.080523 | -0.04117 |
| Predicted P(WIN) | 0.601874 | 0.594763 | 0.585301 |
| Actual P(WIN) | 0.394515 | 0.389755 | 0.404255 |
| Predicted P(LOSS) | 0.235229 | 0.247929 | 0.243748 |
| Actual P(LOSS) | 0.383966 | 0.420935 | 0.404255 |

## Goal calibration

| Metric | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |
| --- | ---: | ---: | ---: |
| Home goals predicted | 1.562478 | 1.748046 | 1.518242 |
| Home goals actual | 1.57551 | 1.746793 | 1.525396 |
| Away goals predicted | 1.262254 | 1.409947 | 1.303529 |
| Away goals actual | 1.313605 | 1.405807 | 1.298744 |
| 1X2 HOME predicted | 0.443058 | 0.456563 | 0.428465 |
| 1X2 HOME actual | 0.446939 | 0.434841 | 0.413435 |
| 1X2 DRAW predicted | 0.225691 | 0.2099 | 0.229322 |
| 1X2 DRAW actual | 0.22381 | 0.251857 | 0.260513 |
| 1X2 AWAY predicted | 0.331251 | 0.333537 | 0.342213 |
| 1X2 AWAY actual | 0.329252 | 0.313302 | 0.326051 |

## AH families

| Family | PL ROI | BL ROI | SA ROI |
| --- | ---: | ---: | ---: |
| NEGATIVE_HANDICAP | -0.055611 | -0.064098 | -0.005114 |
| ZERO | -0.027844 | -0.021287 | -0.02348 |
| POSITIVE_HANDICAP | -0.004336 | -0.002755 | -0.056966 |

Repeated family ordering is not a production filter.

## Strategy results

Unchanged StrategyPresetFactory configs. Not a league-selection exercise.

| Strategy | PL bets | PL ROI | PL paused | BL bets | BL ROI | BL paused | SA bets | SA ROI | SA paused |
| --- | ---: | ---: | --- | ---: | ---: | --- | ---: | ---: | --- |
| DEFENSIVE | 117 | -0.030255 | true | 194 | -0.034371 | true | 198 | -0.027486 | true |
| BALANCED | 122 | -0.021072 | true | 202 | -0.022986 | true | 179 | -0.025404 | true |
| GROWTH | 123 | -0.021641 | true | 109 | -0.047705 | true | 219 | -0.023299 | true |
| FLAT_STAKE | 588 | -0.038249 | true | 375 | -0.028569 | true | 332 | -0.057821 | true |

## Statistical uncertainty

Deterministic bootstrap seed=20260816, replicates=2000. CI excluding 0 is not proof of future profitability.

| Group | PL mean | PL 95% | BL mean | BL 95% | SA mean | SA 95% |
| --- | ---: | --- | ---: | --- | ---: | --- |
| all candidates | -0.029741 | [-0.062548, 0.002031] | -0.032058 | [-0.065289, -0.00015] | -0.030197 | [-0.058882, -0.001949] |
| positive-edge | -0.037802 | [-0.082366, 0.011765] | -0.059846 | [-0.107826, -0.010799] | -0.022277 | [-0.064978, 0.020701] |
| edge >= 0.10 | -0.015417 | [-0.073519, 0.043637] | -0.074026 | [-0.1316, -0.011476] | -0.024597 | [-0.081985, 0.03764] |
| NEGATIVE_HANDICAP | -0.055611 | [-0.10271, -0.007439] | -0.064098 | [-0.110495, -0.013505] | -0.005114 | [-0.046486, 0.03824] |
| POSITIVE_HANDICAP | -0.004336 | [-0.053, 0.042573] | -0.002755 | [-0.053189, 0.043569] | -0.056966 | [-0.099662, -0.016249] |

## Structural-error test

Diagnostic cutoffs (not production filters): |1X2 gap| ≤ 0.03; |goal gap| ≤ 0.15; |edge−return gap| ≤ 0.01; |Spearman| < 0.10; high-edge ≥10% and ≥20% have predicted WIN above actual and predicted LOSS below actual; edge buckets with n≥30 are not monotone in ROI; ≥3 populated seasons have negative unit ROI and ≥2 populated ≥10%-edge seasons have negative ≥10% ROI.

| Pattern | PREMIER_LEAGUE | BUNDESLIGA | SERIE_A |
| --- | --- | --- | --- |
| A) aggregate goals / 1X2 reasonably calibrated | yes | no | no |
| B) aggregate predicted edge near realized return | yes | yes | yes |
| C) edge ranking near zero / weak | yes | yes | yes |
| D) high predicted edge: P(WIN) too high, P(LOSS) too low | yes | yes | yes |
| E) higher edge does not monotonically improve ROI | yes | yes | yes |
| F) failure appears across multiple seasons | yes | yes | yes |

Three leagues are strong evidence, not mathematical proof.

## Classification

**FAILURE STRONGLY REPLICATES AGAIN**

Serie A reproduces the same AH edge-ranking limitation seen in Premier League and Bundesliga.

The independent Poisson baseline has now shown the same AH edge-ranking limitation across three major leagues. Further same-model league replications are low-value; the next justified experiment is a better probability model.

### NEXT HYPOTHESIS

Possible later experiments (not implemented, not chosen here): Dixon-Coles dependence correction; attack/defence strength shrinkage; richer strength model; Elo; xG.

## Explicit non-conclusions

- no parameter optimization performed
- no production filter selected
- Serie A is not selected as a betting venue because of this comparison
- best-looking cell is not a validated strategy
- three leagues do not prove a theorem
- CI excluding 0 is not proof of future profitability
- MARKET_AVERAGE is not Tippmix
- 1/odds is not true AH probability
- football-data.co.uk historical quotes only

