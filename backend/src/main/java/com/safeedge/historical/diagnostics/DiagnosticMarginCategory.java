package com.safeedge.historical.diagnostics;

public enum DiagnosticMarginCategory {
	HOME_WIN_BY_2_PLUS,
	HOME_WIN_BY_1,
	DRAW,
	AWAY_WIN_BY_1,
	AWAY_WIN_BY_2_PLUS;

	public static DiagnosticMarginCategory ofHomeMargin(int homeMinusAway) {
		if (homeMinusAway >= 2) {
			return HOME_WIN_BY_2_PLUS;
		}
		if (homeMinusAway == 1) {
			return HOME_WIN_BY_1;
		}
		if (homeMinusAway == 0) {
			return DRAW;
		}
		if (homeMinusAway == -1) {
			return AWAY_WIN_BY_1;
		}
		return AWAY_WIN_BY_2_PLUS;
	}
}
