# Strategy domain

SafeEdge stores staking and risk **configuration** in `StrategyConfig`. A future Strategy Engine, backtests, and UI-editable settings must all consume that object.

This package does not execute strategy. It does not compute Kelly stakes, sweep Vault, track exposure, or apply drawdown state.

## Preset is data, not behavior

```text
StrategyPreset  →  StrategyConfig  →  future StrategyEngine
```

`StrategyPresetFactory.configFor(preset)` returns a fresh immutable `StrategyConfig`. The engine must not branch on preset identity (`if (preset == DEFENSIVE)`).

The four predefined presets (`DEFENSIVE`, `BALANCED`, `GROWTH`, `FLAT_STAKE`) are **initial hypotheses**, not optimized or proven values. Later backtests should compare configurations, not treat these numbers as truth.

There is no `CUSTOM` or `MODIFIED` preset. A custom strategy is any valid `StrategyConfig` constructed by the caller. Whether a live config still matches a preset is an application/UI comparison (`current.equals(presetConfig)`), not a field on the config.

## Rates

All percentages are decimal fractions on `BigDecimal`:

- 30% → `0.30`
- 2% → `0.02`
- quarter Kelly → `0.25`

Do not store display percents or `double`/`float` rates in this domain.

## Vault, Kelly, exposure, drawdown

`vaultEnabled` and `vaultSweepRate` are configuration only. Vault is not active bankroll. Execution of sweeps, transfers, and balances lives in the bankroll accounting domain; see [bankroll-accounting.md](bankroll-accounting.md).

`stakingMode` and `kellyFraction` / `flatStakeRate` are configuration only. Kelly mathematics (including Asian Handicap) is a later engine.

Match / league / daily exposure fields are caps, not live exposure.

Drawdown fields are thresholds and a stake **multiplier that cannot exceed 1**. There is no high-water mark, pause state, or loss-chasing multiplier (`lossMultiplier`, `doubleAfterLoss`, and similar must never appear).

## Validation

Invalid configs are rejected. Values are not silently rewritten. See `StrategyConfig` for the rules.
