Architecture notes, Tippmix API discovery, and other project documentation live here.

- [Strategy domain](strategy-domain.md) — configuration model and preset hypotheses
- [Strategy engine](strategy-engine.md) — accept/reject and stake sizing (pure, no persistence)
- [Candidate engine](candidate-engine.md) — score distribution → settlement probabilities and expected return (pure, no predictive model yet)
- [Bankroll accounting](bankroll-accounting.md) — Active vs Vault, sweeps, high-water marks (not persisted yet)
- [Backtest engine](backtest-engine.md) — chronological replay of prepared historical opportunities (pure, no persistence)
- [Historical data import](historical-data-import.md) — football-data.co.uk Big 5 results, bulk import, and AH coverage audit (not Tippmix)
- [Historical features](historical-features.md) — point-in-time pre-match features and score targets
- [Probability model v1](probability-model-v1.md) — frozen time-decayed independent Poisson baseline
- [Probability model v2](probability-model-v2.md) — regularized Dixon-Coles (development leagues only; not a profitability claim)
- [Probability model v3](probability-model-v3.md) — jointly fitted regularized Dixon-Coles team strengths (development leagues only; not a profitability claim)
- [Historical walk-forward evaluation](historical-walk-forward-evaluation.md) — prior-only dataset builder, CandidateEngine, BacktestEngine comparison, and baseline diagnostics (not Tippmix odds)
- [Rich historical football data research](research/rich-historical-football-data.md) — xG / Elo / market sources for a possible v4; research only, not an importer
