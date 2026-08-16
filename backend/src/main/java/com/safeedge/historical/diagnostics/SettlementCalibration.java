package com.safeedge.historical.diagnostics;

public record SettlementCalibration(
		OutcomeCalibration win,
		OutcomeCalibration halfWin,
		OutcomeCalibration push,
		OutcomeCalibration halfLoss,
		OutcomeCalibration loss) {

	public SettlementCalibration {
		if (win == null || halfWin == null || push == null || halfLoss == null || loss == null) {
			throw new IllegalArgumentException("all five settlement calibrations are required");
		}
	}
}
