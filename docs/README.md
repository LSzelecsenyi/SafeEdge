Architecture notes, Tippmix API discovery, and other project documentation live here.

- [Strategy domain](strategy-domain.md) — configuration model and preset hypotheses
- [Strategy engine](strategy-engine.md) — accept/reject and stake sizing (pure, no persistence)
- [Candidate engine](candidate-engine.md) — score distribution → settlement probabilities and expected return (pure, no predictive model yet)
- [Bankroll accounting](bankroll-accounting.md) — Active vs Vault, sweeps, high-water marks (not persisted yet)
- [Backtest engine](backtest-engine.md) — chronological replay of prepared historical opportunities (pure, no persistence)
- [Historical data import](historical-data-import.md) — football-data.co.uk Big 5 results, bulk import, and AH coverage audit (not Tippmix)
- [Historical features](historical-features.md) — point-in-time pre-match features and score targets
- [Probability model v1](probability-model-v1.md) — time-decayed independent Poisson score distribution (baseline, not a strategy)
- [Historical walk-forward evaluation](historical-walk-forward-evaluation.md) — prior-only dataset builder, CandidateEngine, and BacktestEngine comparison (not Tippmix odds)
