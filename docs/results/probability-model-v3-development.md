# Probability Model v3 Development Evaluation

## Model definition

Implemented model: `JointDixonColesFootballProbabilityModel`.
Frozen baselines: `PoissonFootballProbabilityModel` (v1) and `RegularizedDixonColesFootballProbabilityModel` (v2).

Defence convention: **positive defence is stronger** (concedes fewer). Log-link:

```text
log λ_home = intercept + homeAdvantage + attack(home) − defence(away)
log λ_away = intercept + attack(away) − defence(home)
ρ = rhoScale * tanh(z)
```

Identifiability: `Σ attack = 0` and `Σ defence = 0` after every optimizer step.
Regularization: L2 on centered attack/defence; intercept and home advantage unpenalized.

| Field | Frozen default |
|---|---|
| decayHalfLifeDays | 180 |
| maxGoalsPerTeam | 10 |
| minimumTeamMatches | 5 |
| minimumLeagueMatches | 20 |
| attackRegularization | 5 |
| defenceRegularization | 5 |
| optimizerMaxIterations | 80 |
| gradientTolerance | 0.00001 |
| rhoScale | 0.4 |

These defaults were declared before evaluation. They are not ROI-fitted optima.

## Predeclared classification gates

Compare v3 to the **better of v1/v2**. Positive ROI is ignored.

- `MODEL_V3_CLEAR_IMPROVEMENT`: Spearman +≥0.05 vs better baseline in ≥2/3 leagues; third not worse by ≥0.02; ≥10% WIN **and** LOSS abs-gap shrink ≥0.03 in ≥2/3 leagues; log loss not worse by >0.02 in any league.
- `MODEL_V3_PARTIAL_IMPROVEMENT`: log loss gate holds; Spearman +≥0.02 in ≥2 leagues **or** WIN+LOSS gap shrink in ≥2; Spearman not worse by ≥0.02 in more than one league.
- `MODEL_V3_REGRESSION`: Spearman worse by ≥0.02 in ≥2 leagues, **or** log loss worse by >0.02 in ≥2, **or** ≥10% WIN gaps worsen by ≥0.03 in ≥2, without CLEAR/PARTIAL offset.
- `MODEL_V3_NO_MEANINGFUL_IMPROVEMENT`: otherwise.

## Anti-leakage

- Walk-forward: same competition, `matchDate < targetDate`, no same-day, no future.
- Score-only joint MLE. No bookmaker odds, AH line, edge, or ROI in fitting.
- Development leagues only: Premier League, Bundesliga, Serie A.
- La Liga and Ligue 1 were not run and were not inspected.
- CandidateEngine, StrategyEngine, BacktestEngine, and SettlementEngine were not changed.
- HISTORICAL QUOTE SOURCE = MARKET_AVERAGE (football-data.co.uk, not Tippmix).
- Window: trainingFromSeason=2014, evaluation 2019→2023, starting bankroll 100000.
- Warm-start uses only earlier cutoffs as initial values; each date still refits.

## Premier League

### Counts

| Count | V1 | V2 | V3 |
|---|---|---|---|
| matches loaded | 3420 | 3420 | 3420 |
| matches evaluated | 1520 | 1520 | 1520 |
| predictions available | 1470 | 1470 | 1495 |
| skipped insufficient history | 50 | 50 | 25 |
| skipped fitting failed | 0 | 0 | 0 |
| candidates | 2940 | 2940 | 2990 |
| positive EV | 1306 | 1314 | 1278 |
| zero EV | 0 | 0 | 0 |
| negative EV | 1634 | 1626 | 1712 |

### Score, ranking, and mean edge

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| score log loss | 3.061582 | 3.0157 | 2.991684 |
| Spearman | 0.0172 | 0.01207 | 0.013411 |
| Pearson | 0.012664 | 0.016194 | 0.015018 |
| mean predicted edge | -0.029834 | -0.030036 | -0.029964 |
| realized all-candidate unit ROI | -0.029741 | -0.029741 | -0.029639 |
| decile ROI inversions (n≥30) | 5 | 5 | 4 |

| λ_home | p50 | p90 | p99 | max |
|---|---|---|---|---|
| V1 | 1.417737 | 2.554517 | 3.780918 | 5.474818 |
| V2 | 1.48558 | 2.20333 | 2.936439 | 3.650399 |
| V3 | 1.487489 | 2.223664 | 3.074342 | 3.600919 |

| λ_away | p50 | p90 | p99 | max |
|---|---|---|---|---|
| V1 | 1.156706 | 2.052178 | 2.855362 | 4.279903 |
| V2 | 1.203952 | 1.758804 | 2.278286 | 3.197781 |
| V3 | 1.204065 | 1.788657 | 2.30044 | 2.950029 |

| P(WIN) | p50 | p90 | p99 | max |
|---|---|---|---|---|
| V1 | 0.400423 | 0.600908 | 0.741572 | 0.906915 |
| V2 | 0.400907 | 0.592419 | 0.719847 | 0.859146 |
| V3 | 0.402908 | 0.560788 | 0.674514 | 0.845148 |

### Edge deciles

| Decile | V1 n | V1 avg edge | V1 ROI | V1 gap | V2 n | V2 avg edge | V2 ROI | V2 gap | V3 n | V3 avg edge | V3 ROI | V3 gap |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| decile 1 (lowest edge to highest) | 294 | -0.441356 | -0.023656 | 0.417699 | 294 | -0.403584 | -0.073639 | 0.329944 | 299 | -0.331779 | -0.088094 | 0.243685 |
| decile 2 (lowest edge to highest) | 294 | -0.273428 | -0.08784 | 0.185588 | 294 | -0.25717 | -0.014864 | 0.242306 | 299 | -0.208125 | -0.051288 | 0.156837 |
| decile 3 (lowest edge to highest) | 294 | -0.18579 | -0.011378 | 0.174413 | 294 | -0.178933 | -0.026054 | 0.152878 | 299 | -0.144574 | 0.043545 | 0.188119 |
| decile 4 (lowest edge to highest) | 294 | -0.116097 | -0.011871 | 0.104226 | 294 | -0.112528 | -0.010969 | 0.101559 | 299 | -0.09342 | 0.008227 | 0.101647 |
| decile 5 (lowest edge to highest) | 294 | -0.05699 | -0.005714 | 0.051276 | 294 | -0.057071 | -0.032296 | 0.024775 | 299 | -0.04996 | 0.001689 | 0.051649 |
| decile 6 (lowest edge to highest) | 294 | -0.002114 | -0.040425 | -0.038311 | 294 | -0.002074 | -0.01318 | -0.011107 | 299 | -0.009656 | -0.066839 | -0.057184 |
| decile 7 (lowest edge to highest) | 294 | 0.05648 | -0.075544 | -0.132024 | 294 | 0.053062 | -0.041224 | -0.094287 | 299 | 0.034438 | -0.035585 | -0.070023 |
| decile 8 (lowest edge to highest) | 294 | 0.12715 | -0.05318 | -0.180331 | 294 | 0.11831 | -0.042279 | -0.160589 | 299 | 0.0849 | -0.0951 | -0.18 |
| decile 9 (lowest edge to highest) | 294 | 0.213902 | 0.059439 | -0.154463 | 294 | 0.196527 | -0.067789 | -0.264316 | 299 | 0.147486 | -0.03107 | -0.178556 |
| decile 10 (lowest edge to highest) | 294 | 0.379908 | -0.047245 | -0.427153 | 294 | 0.343104 | 0.024881 | -0.318223 | 299 | 0.271048 | 0.018127 | -0.25292 |

### High-edge five-way settlement

#### ≥ 3%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 1150 | 1144 | 1070 |
| avg edge | 0.19812 | 0.181944 | 0.147799 |
| unit ROI | -0.020235 | -0.027889 | -0.025879 |
| WIN predicted | 0.526855 | 0.521337 | 0.501208 |
| WIN actual | 0.39913 | 0.398601 | 0.398131 |
| HALF_WIN predicted | 0.060155 | 0.058258 | 0.059645 |
| HALF_WIN actual | 0.063478 | 0.063811 | 0.065421 |
| PUSH predicted | 0.060448 | 0.062111 | 0.061486 |
| PUSH actual | 0.070435 | 0.072552 | 0.069159 |
| HALF_LOSS predicted | 0.059874 | 0.055508 | 0.059144 |
| HALF_LOSS actual | 0.086957 | 0.075175 | 0.079439 |
| LOSS predicted | 0.292668 | 0.302786 | 0.318517 |
| LOSS actual | 0.38 | 0.38986 | 0.38785 |

#### ≥ 5%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 1044 | 1031 | 945 |
| avg edge | 0.214104 | 0.197475 | 0.162028 |
| unit ROI | -0.021494 | -0.030165 | -0.026042 |
| WIN predicted | 0.535346 | 0.529617 | 0.509135 |
| WIN actual | 0.399425 | 0.395732 | 0.396825 |
| HALF_WIN predicted | 0.059533 | 0.058751 | 0.058806 |
| HALF_WIN actual | 0.061303 | 0.064985 | 0.065608 |
| PUSH predicted | 0.060161 | 0.061651 | 0.061504 |
| PUSH actual | 0.071839 | 0.075655 | 0.070899 |
| HALF_LOSS predicted | 0.060228 | 0.053001 | 0.058824 |
| HALF_LOSS actual | 0.085249 | 0.070805 | 0.079365 |
| LOSS predicted | 0.284732 | 0.296981 | 0.31173 |
| LOSS actual | 0.382184 | 0.392823 | 0.387302 |

#### ≥ 10%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 851 | 817 | 664 |
| avg edge | 0.245616 | 0.229396 | 0.19903 |
| unit ROI | -0.015417 | -0.01634 | -0.007327 |
| WIN predicted | 0.553973 | 0.547966 | 0.530367 |
| WIN actual | 0.40188 | 0.400245 | 0.406627 |
| HALF_WIN predicted | 0.056707 | 0.057127 | 0.05837 |
| HALF_WIN actual | 0.061105 | 0.067319 | 0.069277 |
| PUSH predicted | 0.059854 | 0.059419 | 0.057553 |
| PUSH actual | 0.07168 | 0.072215 | 0.061747 |
| HALF_LOSS predicted | 0.059622 | 0.053411 | 0.058384 |
| HALF_LOSS actual | 0.086957 | 0.078335 | 0.084337 |
| LOSS predicted | 0.269844 | 0.282077 | 0.295326 |
| LOSS actual | 0.378378 | 0.381885 | 0.378012 |

#### ≥ 20%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 474 | 423 | 280 |
| avg edge | 0.324177 | 0.306954 | 0.276005 |
| unit ROI | -0.021962 | -0.005863 | 0.012446 |
| WIN predicted | 0.601874 | 0.593454 | 0.57888 |
| WIN actual | 0.394515 | 0.408983 | 0.421429 |
| HALF_WIN predicted | 0.052201 | 0.052906 | 0.047417 |
| HALF_WIN actual | 0.07173 | 0.066194 | 0.057143 |
| PUSH predicted | 0.049155 | 0.051214 | 0.055862 |
| PUSH actual | 0.061181 | 0.066194 | 0.064286 |
| HALF_LOSS predicted | 0.061541 | 0.051479 | 0.054245 |
| HALF_LOSS actual | 0.088608 | 0.070922 | 0.085714 |
| LOSS predicted | 0.235229 | 0.250947 | 0.263596 |
| LOSS actual | 0.383966 | 0.387707 | 0.371429 |

#### ≥ 30%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 238 | 187 | 74 |
| avg edge | 0.402628 | 0.384387 | 0.365689 |
| unit ROI | 0.015378 | 0.071471 | 0.163784 |
| WIN predicted | 0.646961 | 0.642688 | 0.629427 |
| WIN actual | 0.415966 | 0.44385 | 0.486486 |
| HALF_WIN predicted | 0.050816 | 0.036387 | 0.039222 |
| HALF_WIN actual | 0.067227 | 0.048128 | 0.067568 |
| PUSH predicted | 0.045019 | 0.05378 | 0.057416 |
| PUSH actual | 0.071429 | 0.090909 | 0.067568 |
| HALF_LOSS predicted | 0.052159 | 0.053471 | 0.054494 |
| HALF_LOSS actual | 0.071429 | 0.085561 | 0.081081 |
| LOSS predicted | 0.205045 | 0.213674 | 0.219442 |
| LOSS actual | 0.37395 | 0.331551 | 0.297297 |

### HOME / AWAY (v3)

| Group | n | avg edge | unit ROI |
|---|---|---|---|
| HOME | 1495 | -0.022278 | -0.033435 |
| AWAY | 1495 | -0.03765 | -0.025843 |

### AH family (v3)

| Group | n | avg edge | unit ROI |
|---|---|---|---|
| NEGATIVE_HANDICAP | 1331 | -0.130377 | -0.061221 |
| ZERO | 328 | -0.025235 | -0.027866 |
| POSITIVE_HANDICAP | 1331 | 0.069284 | 0.001506 |

### Season (v3)

| Season | n | avg edge | unit ROI | ≥10% n | ≥10% ROI |
|---|---|---|---|---|---|
| 2019/20 | 618 | -0.029264 | -0.030502 | 133 | 0.05703 |
| 2020/21 | 882 | -0.028566 | -0.027744 | 189 | 0.040159 |
| 2022/23 | 740 | -0.0301 | -0.030628 | 169 | -0.029645 |
| 2023/24 | 750 | -0.032051 | -0.03018 | 173 | -0.086879 |

### Fitted parameters and optimizer (v3)

- snapshots=1495
- fitting failures=0
- converged=0
- mean iterations=80
- median iterations=80
- max iterations=80
- parameters finite=true
- median home advantage positive=true
- ρ min/median/max=-0.134142/-0.035725/0.127397

| attack | min | p10 | p50 | p90 | p99 | max |
|---|---|---|---|---|---|---|
| V3 | -0.496944 | -0.24075 | 0.004704 | 0.3364 | 0.562394 | 0.634628 |

| defence | min | p10 | p50 | p90 | p99 | max |
|---|---|---|---|---|---|---|
| V3 | -0.479281 | -0.186843 | 0.026229 | 0.232062 | 0.447199 | 0.554623 |

| homeAdvantage | min | p10 | p50 | p90 | p99 | max |
|---|---|---|---|---|---|---|
| V3 | 0.055817 | 0.109518 | 0.22104 | 0.280389 | 0.379162 | 0.40585 |

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI | V3 accepted | V3 ROI |
|---|---|---|---|---|---|---|
| DEFENSIVE | 117 | -0.030255 | 233 | -0.058175 | 223 | -0.003441 |
| BALANCED | 122 | -0.021072 | 153 | -0.06704 | 180 | 0.010686 |
| GROWTH | 123 | -0.021641 | 122 | -0.071655 | 202 | -0.003634 |
| FLAT_STAKE | 588 | -0.038249 | 789 | -0.025473 | 576 | -0.017607 |


## Bundesliga

### Counts

| Count | V1 | V2 | V3 |
|---|---|---|---|
| matches loaded | 3060 | 3060 | 3060 |
| matches evaluated | 1530 | 1530 | 1530 |
| predictions available | 1481 | 1481 | 1505 |
| skipped insufficient history | 49 | 49 | 25 |
| skipped fitting failed | 0 | 0 | 0 |
| candidates | 2962 | 2962 | 3010 |
| positive EV | 1327 | 1311 | 1252 |
| zero EV | 0 | 0 | 0 |
| negative EV | 1635 | 1651 | 1758 |

### Score, ranking, and mean edge

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| score log loss | 3.143268 | 3.10184 | 3.076402 |
| Spearman | -0.010761 | 0.0005 | -0.015666 |
| Pearson | -0.033968 | -0.007716 | -0.0174 |
| mean predicted edge | -0.031134 | -0.031214 | -0.031253 |
| realized all-candidate unit ROI | -0.032058 | -0.032058 | -0.032113 |
| decile ROI inversions (n≥30) | 6 | 4 | 5 |

| λ_home | p50 | p90 | p99 | max |
|---|---|---|---|---|
| V1 | 1.622678 | 2.72782 | 3.916054 | 6.563773 |
| V2 | 1.680948 | 2.398588 | 3.167137 | 4.537835 |
| V3 | 1.651091 | 2.44254 | 3.291187 | 4.755758 |

| λ_away | p50 | p90 | p99 | max |
|---|---|---|---|---|
| V1 | 1.305873 | 2.198987 | 3.258261 | 6.277476 |
| V2 | 1.344656 | 1.931381 | 2.499671 | 3.245559 |
| V3 | 1.328319 | 1.94744 | 2.700629 | 3.435147 |

| P(WIN) | p50 | p90 | p99 | max |
|---|---|---|---|---|
| V1 | 0.407459 | 0.587359 | 0.714346 | 0.898543 |
| V2 | 0.405909 | 0.568215 | 0.695663 | 0.774123 |
| V3 | 0.401849 | 0.541058 | 0.646247 | 0.747061 |

### Edge deciles

| Decile | V1 n | V1 avg edge | V1 ROI | V1 gap | V2 n | V2 avg edge | V2 ROI | V2 gap | V3 n | V3 avg edge | V3 ROI | V3 gap |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| decile 1 (lowest edge to highest) | 297 | -0.412241 | 0.018182 | 0.430422 | 297 | -0.367267 | -0.054747 | 0.31252 | 301 | -0.282254 | -0.027542 | 0.254713 |
| decile 2 (lowest edge to highest) | 296 | -0.26525 | -0.005068 | 0.260183 | 296 | -0.23421 | 0.063412 | 0.297622 | 301 | -0.178903 | 0.035183 | 0.214086 |
| decile 3 (lowest edge to highest) | 296 | -0.180915 | -0.046993 | 0.133922 | 296 | -0.163996 | -0.066216 | 0.09778 | 301 | -0.124975 | 0.007841 | 0.132815 |
| decile 4 (lowest edge to highest) | 296 | -0.117305 | 0.033159 | 0.150464 | 296 | -0.106958 | -0.04603 | 0.060928 | 301 | -0.085681 | 0.046429 | 0.132109 |
| decile 5 (lowest edge to highest) | 296 | -0.062534 | -0.041993 | 0.02054 | 296 | -0.057539 | -0.050084 | 0.007455 | 301 | -0.050734 | -0.029003 | 0.02173 |
| decile 6 (lowest edge to highest) | 297 | -0.001417 | 0.004832 | 0.006248 | 297 | -0.00531 | 0.025202 | 0.030512 | 301 | -0.012008 | -0.075847 | -0.063839 |
| decile 7 (lowest edge to highest) | 296 | 0.055127 | -0.103649 | -0.158775 | 296 | 0.044028 | -0.029307 | -0.073335 | 301 | 0.022397 | -0.103953 | -0.126351 |
| decile 8 (lowest edge to highest) | 296 | 0.117701 | -0.036182 | -0.153883 | 296 | 0.101819 | -0.019003 | -0.120822 | 301 | 0.062228 | -0.038173 | -0.100401 |
| decile 9 (lowest edge to highest) | 296 | 0.203095 | -0.059882 | -0.262977 | 296 | 0.171687 | -0.151757 | -0.323444 | 301 | 0.116643 | -0.080249 | -0.196892 |
| decile 10 (lowest edge to highest) | 296 | 0.353581 | -0.083277 | -0.436859 | 296 | 0.306655 | 0.007838 | -0.298817 | 301 | 0.220752 | -0.055814 | -0.276566 |

### High-edge five-way settlement

#### ≥ 3%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 1170 | 1122 | 992 |
| avg edge | 0.184215 | 0.163319 | 0.124396 |
| unit ROI | -0.069543 | -0.048449 | -0.061215 |
| WIN predicted | 0.523067 | 0.508612 | 0.482107 |
| WIN actual | 0.395726 | 0.397504 | 0.399194 |
| HALF_WIN predicted | 0.058321 | 0.063244 | 0.069818 |
| HALF_WIN actual | 0.051282 | 0.062389 | 0.052419 |
| PUSH predicted | 0.056088 | 0.057969 | 0.060605 |
| PUSH actual | 0.050427 | 0.054367 | 0.053427 |
| HALF_LOSS predicted | 0.060216 | 0.058212 | 0.056119 |
| HALF_LOSS actual | 0.078632 | 0.074866 | 0.070565 |
| LOSS predicted | 0.302307 | 0.311963 | 0.331351 |
| LOSS actual | 0.423932 | 0.410873 | 0.424395 |

#### ≥ 5%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 1056 | 992 | 831 |
| avg edge | 0.199769 | 0.179486 | 0.140819 |
| unit ROI | -0.059844 | -0.052767 | -0.052581 |
| WIN predicted | 0.531966 | 0.517793 | 0.491308 |
| WIN actual | 0.399621 | 0.398185 | 0.403129 |
| HALF_WIN predicted | 0.057864 | 0.061723 | 0.069328 |
| HALF_WIN actual | 0.052083 | 0.056452 | 0.052948 |
| PUSH predicted | 0.055835 | 0.059532 | 0.058935 |
| PUSH actual | 0.051136 | 0.056452 | 0.052948 |
| HALF_LOSS predicted | 0.058679 | 0.056592 | 0.056164 |
| HALF_LOSS actual | 0.079545 | 0.077621 | 0.070999 |
| LOSS predicted | 0.295657 | 0.30436 | 0.324265 |
| LOSS actual | 0.417614 | 0.41129 | 0.419976 |

#### ≥ 10%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 806 | 751 | 520 |
| avg edge | 0.238341 | 0.21299 | 0.180884 |
| unit ROI | -0.074026 | -0.061731 | -0.069048 |
| WIN predicted | 0.554189 | 0.536038 | 0.516528 |
| WIN actual | 0.394541 | 0.396804 | 0.398077 |
| HALF_WIN predicted | 0.055858 | 0.063042 | 0.059756 |
| HALF_WIN actual | 0.049628 | 0.055925 | 0.048077 |
| PUSH predicted | 0.052675 | 0.056075 | 0.061555 |
| PUSH actual | 0.044665 | 0.050599 | 0.051923 |
| HALF_LOSS predicted | 0.059158 | 0.052989 | 0.058328 |
| HALF_LOSS actual | 0.088089 | 0.074567 | 0.071154 |
| LOSS predicted | 0.27812 | 0.291855 | 0.303833 |
| LOSS actual | 0.423077 | 0.422104 | 0.430769 |

#### ≥ 20%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 449 | 345 | 165 |
| avg edge | 0.310711 | 0.292986 | 0.257598 |
| unit ROI | -0.080523 | -0.028029 | 0.052727 |
| WIN predicted | 0.594763 | 0.577145 | 0.565121 |
| WIN actual | 0.389755 | 0.417391 | 0.448485 |
| HALF_WIN predicted | 0.051024 | 0.061334 | 0.053683 |
| HALF_WIN actual | 0.046771 | 0.06087 | 0.078788 |
| PUSH predicted | 0.050835 | 0.054607 | 0.057007 |
| PUSH actual | 0.044543 | 0.043478 | 0.036364 |
| HALF_LOSS predicted | 0.055448 | 0.048808 | 0.048852 |
| HALF_LOSS actual | 0.097996 | 0.055072 | 0.060606 |
| LOSS predicted | 0.247929 | 0.258106 | 0.275338 |
| LOSS actual | 0.420935 | 0.423188 | 0.375758 |

#### ≥ 30%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 199 | 122 | 29 |
| avg edge | 0.392041 | 0.376764 | 0.344244 |
| unit ROI | -0.106533 | -0.009877 | 0.395345 |
| WIN predicted | 0.636867 | 0.628125 | 0.612549 |
| WIN actual | 0.371859 | 0.434426 | 0.551724 |
| HALF_WIN predicted | 0.047323 | 0.059973 | 0.061773 |
| HALF_WIN actual | 0.050251 | 0.057377 | 0.172414 |
| PUSH predicted | 0.0549 | 0.040846 | 0.038155 |
| PUSH actual | 0.045226 | 0.016393 | 0.068966 |
| HALF_LOSS predicted | 0.046702 | 0.051626 | 0.047549 |
| HALF_LOSS actual | 0.100503 | 0.090164 | 0 |
| LOSS predicted | 0.214208 | 0.219431 | 0.239974 |
| LOSS actual | 0.432161 | 0.401639 | 0.206897 |

### HOME / AWAY (v3)

| Group | n | avg edge | unit ROI |
|---|---|---|---|
| HOME | 1505 | -0.014477 | -0.029392 |
| AWAY | 1505 | -0.04803 | -0.034834 |

### AH family (v3)

| Group | n | avg edge | unit ROI |
|---|---|---|---|
| NEGATIVE_HANDICAP | 1336 | -0.099476 | -0.064622 |
| ZERO | 338 | -0.024727 | -0.021154 |
| POSITIVE_HANDICAP | 1336 | 0.035318 | -0.002376 |

### Season (v3)

| Season | n | avg edge | unit ROI | ≥10% n | ≥10% ROI |
|---|---|---|---|---|---|
| 2019/20 | 602 | -0.029723 | -0.03299 | 106 | -0.062123 |
| 2020/21 | 602 | -0.031272 | -0.031445 | 90 | -0.127944 |
| 2021/22 | 592 | -0.028837 | -0.029814 | 116 | -0.056336 |
| 2022/23 | 612 | -0.032044 | -0.031275 | 93 | 0.06371 |
| 2023/24 | 602 | -0.034339 | -0.035017 | 115 | -0.149522 |

### Fitted parameters and optimizer (v3)

- snapshots=1505
- fitting failures=0
- converged=0
- mean iterations=80
- median iterations=80
- max iterations=80
- parameters finite=true
- median home advantage positive=true
- ρ min/median/max=-0.241111/-0.142626/-0.069455

| attack | min | p10 | p50 | p90 | p99 | max |
|---|---|---|---|---|---|---|
| V3 | -0.463595 | -0.211412 | 0.010105 | 0.341173 | 0.618302 | 0.716394 |

| defence | min | p10 | p50 | p90 | p99 | max |
|---|---|---|---|---|---|---|
| V3 | -0.570346 | -0.162109 | 0.01893 | 0.234442 | 0.369398 | 0.440835 |

| homeAdvantage | min | p10 | p50 | p90 | p99 | max |
|---|---|---|---|---|---|---|
| V3 | 0.05922 | 0.09762 | 0.241343 | 0.324655 | 0.348888 | 0.352075 |

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI | V3 accepted | V3 ROI |
|---|---|---|---|---|---|---|
| DEFENSIVE | 194 | -0.034371 | 144 | -0.11823 | 97 | -0.14293 |
| BALANCED | 202 | -0.022986 | 106 | -0.127157 | 101 | -0.13194 |
| GROWTH | 109 | -0.047705 | 113 | -0.123356 | 114 | -0.148074 |
| FLAT_STAKE | 375 | -0.028569 | 372 | -0.058958 | 177 | -0.132902 |


## Serie A

### Counts

| Count | V1 | V2 | V3 |
|---|---|---|---|
| matches loaded | 3800 | 3800 | 3800 |
| matches evaluated | 1900 | 1900 | 1900 |
| predictions available | 1831 | 1831 | 1865 |
| skipped insufficient history | 69 | 69 | 35 |
| skipped fitting failed | 0 | 0 | 0 |
| candidates | 3658 | 3658 | 3726 |
| positive EV | 1570 | 1550 | 1499 |
| zero EV | 0 | 0 | 0 |
| negative EV | 2088 | 2108 | 2227 |

### Score, ranking, and mean edge

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| score log loss | 2.945234 | 2.912799 | 2.896413 |
| Spearman | 0.009322 | -0.009856 | -0.011579 |
| Pearson | -0.000405 | -0.013301 | -0.016301 |
| mean predicted edge | -0.0316 | -0.031506 | -0.031453 |
| realized all-candidate unit ROI | -0.030197 | -0.030197 | -0.030276 |
| decile ROI inversions (n≥30) | 4 | 4 | 4 |

| λ_home | p50 | p90 | p99 | max |
|---|---|---|---|---|
| V1 | 1.421655 | 2.354394 | 3.233427 | 4.497123 |
| V2 | 1.473723 | 2.101626 | 2.665474 | 3.529745 |
| V3 | 1.45197 | 2.127134 | 2.694137 | 3.449929 |

| λ_away | p50 | p90 | p99 | max |
|---|---|---|---|---|
| V1 | 1.198152 | 2.077032 | 3.035643 | 4.069658 |
| V2 | 1.243626 | 1.833151 | 2.324126 | 3.037876 |
| V3 | 1.243682 | 1.8261 | 2.281616 | 2.902686 |

| P(WIN) | p50 | p90 | p99 | max |
|---|---|---|---|---|
| V1 | 0.400167 | 0.566096 | 0.707633 | 0.906142 |
| V2 | 0.395104 | 0.558595 | 0.686404 | 0.813074 |
| V3 | 0.396598 | 0.536501 | 0.647143 | 0.789182 |

### Edge deciles

| Decile | V1 n | V1 avg edge | V1 ROI | V1 gap | V2 n | V2 avg edge | V2 ROI | V2 gap | V3 n | V3 avg edge | V3 ROI | V3 gap |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| decile 1 (lowest edge to highest) | 366 | -0.381095 | -0.03291 | 0.348185 | 366 | -0.344069 | -0.004549 | 0.33952 | 373 | -0.272702 | -0.028324 | 0.244378 |
| decile 2 (lowest edge to highest) | 366 | -0.234652 | 0.009016 | 0.243668 | 366 | -0.216054 | 0.030587 | 0.246642 | 373 | -0.167242 | 0.050214 | 0.217456 |
| decile 3 (lowest edge to highest) | 366 | -0.159661 | -0.099112 | 0.060549 | 366 | -0.150174 | -0.078279 | 0.071895 | 372 | -0.114752 | -0.035793 | 0.078959 |
| decile 4 (lowest edge to highest) | 366 | -0.104524 | -0.016831 | 0.087693 | 366 | -0.095895 | -0.016598 | 0.079296 | 373 | -0.079922 | -0.053619 | 0.026303 |
| decile 5 (lowest edge to highest) | 365 | -0.054636 | -0.065425 | -0.010789 | 365 | -0.051474 | -0.076712 | -0.025238 | 372 | -0.048756 | -0.020632 | 0.028124 |
| decile 6 (lowest edge to highest) | 366 | -0.008764 | -0.000437 | 0.008327 | 366 | -0.011023 | 0.003798 | 0.01482 | 373 | -0.01428 | -0.035643 | -0.021363 |
| decile 7 (lowest edge to highest) | 366 | 0.041277 | -0.017596 | -0.058872 | 366 | 0.033039 | -0.01168 | -0.044719 | 373 | 0.016905 | 0.001099 | -0.015806 |
| decile 8 (lowest edge to highest) | 366 | 0.097597 | 0.002842 | -0.094755 | 366 | 0.087164 | 0.002678 | -0.084487 | 372 | 0.052913 | -0.072594 | -0.125507 |
| decile 9 (lowest edge to highest) | 366 | 0.172165 | -0.044604 | -0.216768 | 366 | 0.153675 | -0.078142 | -0.231817 | 373 | 0.104165 | -0.060845 | -0.165009 |
| decile 10 (lowest edge to highest) | 365 | 0.317187 | -0.037027 | -0.354215 | 365 | 0.28055 | -0.073315 | -0.353865 | 372 | 0.209742 | -0.046774 | -0.256516 |

### High-edge five-way settlement

#### ≥ 3%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 1353 | 1304 | 1146 |
| avg edge | 0.167942 | 0.152961 | 0.119979 |
| unit ROI | -0.020965 | -0.040449 | -0.061619 |
| WIN predicted | 0.507593 | 0.499016 | 0.480043 |
| WIN actual | 0.393939 | 0.388037 | 0.373473 |
| HALF_WIN predicted | 0.06527 | 0.06915 | 0.067035 |
| HALF_WIN actual | 0.070953 | 0.072086 | 0.069808 |
| PUSH predicted | 0.060183 | 0.059406 | 0.062795 |
| PUSH actual | 0.071693 | 0.067485 | 0.070681 |
| HALF_LOSS predicted | 0.062438 | 0.056386 | 0.059889 |
| HALF_LOSS actual | 0.084257 | 0.07362 | 0.08377 |
| LOSS predicted | 0.304515 | 0.316043 | 0.330237 |
| LOSS actual | 0.379157 | 0.398773 | 0.402269 |

#### ≥ 5%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 1215 | 1140 | 950 |
| avg edge | 0.182414 | 0.169191 | 0.13637 |
| unit ROI | -0.022765 | -0.036048 | -0.056453 |
| WIN predicted | 0.51608 | 0.507439 | 0.490794 |
| WIN actual | 0.394239 | 0.388596 | 0.378947 |
| HALF_WIN predicted | 0.063185 | 0.07015 | 0.065254 |
| HALF_WIN actual | 0.069136 | 0.07807 | 0.067368 |
| PUSH predicted | 0.061552 | 0.057771 | 0.060579 |
| PUSH actual | 0.073251 | 0.062281 | 0.067368 |
| HALF_LOSS predicted | 0.061616 | 0.055479 | 0.060448 |
| HALF_LOSS actual | 0.082305 | 0.072807 | 0.087368 |
| LOSS predicted | 0.297567 | 0.309162 | 0.322925 |
| LOSS actual | 0.38107 | 0.398246 | 0.398947 |

#### ≥ 10%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 894 | 838 | 570 |
| avg edge | 0.220989 | 0.203275 | 0.17803 |
| unit ROI | -0.024597 | -0.054809 | -0.057658 |
| WIN predicted | 0.536873 | 0.528 | 0.51723 |
| WIN actual | 0.395973 | 0.381862 | 0.392982 |
| HALF_WIN predicted | 0.062241 | 0.064997 | 0.060123 |
| HALF_WIN actual | 0.067114 | 0.065632 | 0.045614 |
| PUSH predicted | 0.06223 | 0.058564 | 0.061581 |
| PUSH actual | 0.072707 | 0.071599 | 0.07193 |
| HALF_LOSS predicted | 0.059789 | 0.054081 | 0.054186 |
| HALF_LOSS actual | 0.074944 | 0.077566 | 0.085965 |
| LOSS predicted | 0.278866 | 0.294358 | 0.30688 |
| LOSS actual | 0.389262 | 0.403341 | 0.403509 |

#### ≥ 20%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 423 | 350 | 171 |
| avg edge | 0.302358 | 0.284072 | 0.259354 |
| unit ROI | -0.04117 | -0.065386 | -0.057895 |
| WIN predicted | 0.585301 | 0.57814 | 0.567844 |
| WIN actual | 0.404255 | 0.394286 | 0.409357 |
| HALF_WIN predicted | 0.054995 | 0.056018 | 0.042633 |
| HALF_WIN actual | 0.037825 | 0.037143 | 0.017544 |
| PUSH predicted | 0.064496 | 0.057264 | 0.076415 |
| PUSH actual | 0.092199 | 0.074286 | 0.087719 |
| HALF_LOSS predicted | 0.05146 | 0.048581 | 0.038084 |
| HALF_LOSS actual | 0.061466 | 0.085714 | 0.064327 |
| LOSS predicted | 0.243748 | 0.259998 | 0.275025 |
| LOSS actual | 0.404255 | 0.408571 | 0.421053 |

#### ≥ 30%

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| n | 160 | 119 | 28 |
| avg edge | 0.398012 | 0.36185 | 0.371165 |
| unit ROI | 0.075719 | -0.028025 | 0.060357 |
| WIN predicted | 0.634797 | 0.63356 | 0.636599 |
| WIN actual | 0.45 | 0.445378 | 0.464286 |
| HALF_WIN predicted | 0.053529 | 0.039826 | 0.03528 |
| HALF_WIN actual | 0.04375 | 0.02521 | 0.071429 |
| PUSH predicted | 0.065199 | 0.055358 | 0.048383 |
| PUSH actual | 0.1125 | 0.05042 | 0.035714 |
| HALF_LOSS predicted | 0.042272 | 0.040143 | 0.052325 |
| HALF_LOSS actual | 0.05625 | 0.05042 | 0 |
| LOSS predicted | 0.204203 | 0.231114 | 0.227413 |
| LOSS actual | 0.3375 | 0.428571 | 0.428571 |

### HOME / AWAY (v3)

| Group | n | avg edge | unit ROI |
|---|---|---|---|
| HOME | 1863 | -0.050579 | -0.065725 |
| AWAY | 1863 | -0.012328 | 0.005172 |

### AH family (v3)

| Group | n | avg edge | unit ROI |
|---|---|---|---|
| NEGATIVE_HANDICAP | 1655 | -0.114344 | -0.011556 |
| ZERO | 416 | -0.024015 | -0.023942 |
| POSITIVE_HANDICAP | 1655 | 0.049567 | -0.050589 |

### Season (v3)

| Season | n | avg edge | unit ROI | ≥10% n | ≥10% ROI |
|---|---|---|---|---|---|
| 2019/20 | 544 | -0.02995 | -0.022647 | 114 | 0.008904 |
| 2020/21 | 944 | -0.031396 | -0.031679 | 164 | -0.163445 |
| 2021/22 | 738 | -0.029589 | -0.027717 | 119 | -0.052185 |
| 2022/23 | 740 | -0.031673 | -0.030507 | 95 | -0.007684 |
| 2023/24 | 760 | -0.034196 | -0.036257 | 78 | -0.001731 |

### Fitted parameters and optimizer (v3)

- snapshots=1865
- fitting failures=0
- converged=0
- mean iterations=80
- median iterations=80
- max iterations=80
- parameters finite=true
- median home advantage positive=true
- ρ min/median/max=-0.234702/-0.062977/0.031792

| attack | min | p10 | p50 | p90 | p99 | max |
|---|---|---|---|---|---|---|
| V3 | -0.433638 | -0.214941 | 0.003589 | 0.322261 | 0.470137 | 0.54112 |

| defence | min | p10 | p50 | p90 | p99 | max |
|---|---|---|---|---|---|---|
| V3 | -0.427832 | -0.172931 | 0.038527 | 0.280786 | 0.427531 | 0.493284 |

| homeAdvantage | min | p10 | p50 | p90 | p99 | max |
|---|---|---|---|---|---|---|
| V3 | 0.088781 | 0.1196 | 0.157013 | 0.202062 | 0.237047 | 0.247431 |

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI | V3 accepted | V3 ROI |
|---|---|---|---|---|---|---|
| DEFENSIVE | 198 | -0.027486 | 177 | 0.014948 | 220 | 0.007367 |
| BALANCED | 179 | -0.025404 | 186 | 0.025324 | 197 | 0.0195 |
| GROWTH | 219 | -0.023299 | 192 | 0.028127 | 237 | -0.013861 |
| FLAT_STAKE | 332 | -0.057821 | 267 | -0.034416 | 283 | -0.030496 |


## V1 / V2 / V3 comparison

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| PL score log loss | 3.061582 | 3.0157 | 2.991684 |
| PL Spearman | 0.0172 | 0.01207 | 0.013411 |
| PL ≥10% edge ROI | -0.015417 | -0.01634 | -0.007327 |
| PL ≥10% WIN pred/act | 0.553973/0.40188 | 0.547966/0.400245 | 0.530367/0.406627 |
| PL ≥10% LOSS pred/act | 0.269844/0.378378 | 0.282077/0.381885 | 0.295326/0.378012 |
| BL score log loss | 3.143268 | 3.10184 | 3.076402 |
| BL Spearman | -0.010761 | 0.0005 | -0.015666 |
| BL ≥10% edge ROI | -0.074026 | -0.061731 | -0.069048 |
| BL ≥10% WIN pred/act | 0.554189/0.394541 | 0.536038/0.396804 | 0.516528/0.398077 |
| BL ≥10% LOSS pred/act | 0.27812/0.423077 | 0.291855/0.422104 | 0.303833/0.430769 |
| SA score log loss | 2.945234 | 2.912799 | 2.896413 |
| SA Spearman | 0.009322 | -0.009856 | -0.011579 |
| SA ≥10% edge ROI | -0.024597 | -0.054809 | -0.057658 |
| SA ≥10% WIN pred/act | 0.536873/0.395973 | 0.528/0.381862 | 0.51723/0.392982 |
| SA ≥10% LOSS pred/act | 0.278866/0.389262 | 0.294358/0.403341 | 0.30688/0.403509 |

Averages across the three development leagues:

| Metric | V1 | V2 | V3 |
|---|---|---|---|
| mean Spearman | 0.005254 | 0.000905 | -0.004611 |
| mean log loss | 3.050028 | 3.010113 | 2.988167 |

## Edge ranking vs better of v1/v2

- Premier League Spearman delta=-0.003789 log-loss delta=-0.024016 ≥10% WIN-gap shrink=0.023981 ≥10% LOSS-gap shrink=0.017122
- Bundesliga Spearman delta=-0.016166 log-loss delta=-0.025438 ≥10% WIN-gap shrink=0.020782 ≥10% LOSS-gap shrink=0.003312
- Serie A Spearman delta=-0.020901 log-loss delta=-0.016385 ≥10% WIN-gap shrink=0.016652 ≥10% LOSS-gap shrink=0.012355

## ≥10% and ≥20% calibration

| League | ≥10% V3 P(WIN)/act | ≥10% V3 P(LOSS)/act | ≥20% V3 P(WIN)/act | ≥20% V3 P(LOSS)/act |
|---|---|---|---|---|
| Premier League | 0.530367/0.406627 | 0.295326/0.378012 | 0.57888/0.421429 | 0.263596/0.371429 |
| Bundesliga | 0.516528/0.398077 | 0.303833/0.430769 | 0.565121/0.448485 | 0.275338/0.375758 |
| Serie A | 0.51723/0.392982 | 0.30688/0.403509 | 0.567844/0.409357 | 0.275025/0.421053 |

## Strategy secondary metrics

Unchanged presets: DEFENSIVE, BALANCED, GROWTH, FLAT_STAKE. ROI is **not** a v3 success gate.

### Premier League

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI | V3 accepted | V3 ROI |
|---|---|---|---|---|---|---|
| DEFENSIVE | 117 | -0.030255 | 233 | -0.058175 | 223 | -0.003441 |
| BALANCED | 122 | -0.021072 | 153 | -0.06704 | 180 | 0.010686 |
| GROWTH | 123 | -0.021641 | 122 | -0.071655 | 202 | -0.003634 |
| FLAT_STAKE | 588 | -0.038249 | 789 | -0.025473 | 576 | -0.017607 |

### Bundesliga

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI | V3 accepted | V3 ROI |
|---|---|---|---|---|---|---|
| DEFENSIVE | 194 | -0.034371 | 144 | -0.11823 | 97 | -0.14293 |
| BALANCED | 202 | -0.022986 | 106 | -0.127157 | 101 | -0.13194 |
| GROWTH | 109 | -0.047705 | 113 | -0.123356 | 114 | -0.148074 |
| FLAT_STAKE | 375 | -0.028569 | 372 | -0.058958 | 177 | -0.132902 |

### Serie A

### Strategy secondary metrics

| Strategy | V1 accepted | V1 ROI | V2 accepted | V2 ROI | V3 accepted | V3 ROI |
|---|---|---|---|---|---|---|
| DEFENSIVE | 198 | -0.027486 | 177 | 0.014948 | 220 | 0.007367 |
| BALANCED | 179 | -0.025404 | 186 | 0.025324 | 197 | 0.0195 |
| GROWTH | 219 | -0.023299 | 192 | 0.028127 | 237 | -0.013861 |
| FLAT_STAKE | 332 | -0.057821 | 267 | -0.034416 | 283 | -0.030496 |

## Explicit non-conclusions

- Classification: **MODEL_V3_NO_MEANINGFUL_IMPROVEMENT**
- PREMIER_LEAGUE Spearman delta -0.00378905404785282034734204543729366 is below the small-improvement cutoff
- BUNDESLIGA Spearman delta -0.0161662164921549902968401364094423017 is below the small-improvement cutoff
- SERIE_A Spearman worsened by -0.020900723162598204604203098165379227 vs better of v1/v2
- Changes were mixed or smaller than the predeclared material thresholds. ROI was not used.
- This is not a claim that SafeEdge is profitable.
- Parameters were not changed after seeing ROI or after Premier League.
- La Liga and Ligue 1 remain untouched validation leagues.

