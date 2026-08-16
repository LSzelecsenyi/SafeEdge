package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import java.math.BigDecimal;
import java.util.List;

/**
 * Frozen Bundesliga Baseline 003 published numbers. Used so Baseline 004 does
 * not rerun Bundesliga. Tests assert the source markdown still contains these
 * values.
 */
public final class BundesligaPublishedBaseline {

	public static final String BASELINE_003_REPORT = "docs/results/baseline-003-bundesliga.md";
	public static final String CROSS_LEAGUE_REPORT = "docs/results/baseline-003-cross-league-validation.md";

	private BundesligaPublishedBaseline() {
	}

	public static LeagueDiagnosticSnapshot snapshot() {
		return new LeagueDiagnosticSnapshot(
				CanonicalCompetition.BUNDESLIGA,
				HistoricalQuoteSource.MARKET_AVERAGE,
				3060,
				1530,
				49,
				0,
				predictionQuality(),
				2962,
				1327,
				0,
				1635,
				bd("-0.031134"),
				bd("-0.032058"),
				bd("-0.000923"),
				bd("-0.010761"),
				bd("-0.033968"),
				deciles(),
				edgeBuckets(),
				highEdge(),
				families(),
				sides(),
				seasons(),
				List.of(),
				bd("0.037353"),
				bd("0.036965"),
				overroundSeasons(),
				strategies(),
				intervals(),
				List.of());
	}

	private static PredictionQualitySnapshot predictionQuality() {
		return new PredictionQualitySnapshot(
				1481,
				1481,
				bd("3.143268"),
				bd("1.748046"),
				bd("1.746793"),
				bd("1.409947"),
				bd("1.405807"),
				bd("0.456563"),
				bd("0.434841"),
				bd("0.2099"),
				bd("0.251857"),
				bd("0.333537"),
				bd("0.313302"),
				List.of(
						margin(DiagnosticMarginCategory.HOME_WIN_BY_2_PLUS, "0.267579", "0.261985", 388),
						margin(DiagnosticMarginCategory.HOME_WIN_BY_1, "0.188984", "0.172856", 256),
						margin(DiagnosticMarginCategory.DRAW, "0.2099", "0.251857", 373),
						margin(DiagnosticMarginCategory.AWAY_WIN_BY_1, "0.160563", "0.147198", 218),
						margin(DiagnosticMarginCategory.AWAY_WIN_BY_2_PLUS, "0.172974", "0.166104", 246)));
	}

	private static List<BucketTrendRow> deciles() {
		return List.of(
				row("decile 1 (lowest edge to highest)", 297, "-0.412241", "0.018182"),
				row("decile 2 (lowest edge to highest)", 296, "-0.26525", "-0.005068"),
				row("decile 3 (lowest edge to highest)", 296, "-0.180915", "-0.046993"),
				row("decile 4 (lowest edge to highest)", 296, "-0.117305", "0.033159"),
				row("decile 5 (lowest edge to highest)", 296, "-0.062534", "-0.041993"),
				row("decile 6 (lowest edge to highest)", 297, "-0.001417", "0.004832"),
				row("decile 7 (lowest edge to highest)", 296, "0.055127", "-0.103649"),
				row("decile 8 (lowest edge to highest)", 296, "0.117701", "-0.036182"),
				row("decile 9 (lowest edge to highest)", 296, "0.203095", "-0.059882"),
				row("decile 10 (lowest edge to highest)", 296, "0.353581", "-0.083277"));
	}

	private static List<BucketTrendRow> edgeBuckets() {
		return List.of(
				row("edge <= 0", 1635, "-0.189639", "-0.009505"),
				row("0 < edge < 0.02", 106, "0.009621", "0.050613"),
				row("0.02 <= edge < 0.05", 165, "0.035537", "-0.130818"),
				row("0.05 <= edge < 0.10", 250, "0.075416", "-0.01412"),
				row("0.10 <= edge < 0.20", 357, "0.14732", "-0.065854"),
				row("0.20 <= edge < 0.30", 250, "0.245972", "-0.05982"),
				row("edge >= 0.30", 199, "0.392041", "-0.106533"));
	}

	private static List<HighEdgeCalibrationSlice> highEdge() {
		return List.of(
				new HighEdgeCalibrationSlice(
						bd("0.10"), 806, bd("0.238341"), bd("-0.074026"),
						bd("0.554189"), bd("0.394541"), bd("0.27812"), bd("0.423077")),
				new HighEdgeCalibrationSlice(
						bd("0.20"), 449, bd("0.310711"), bd("-0.080523"),
						bd("0.594763"), bd("0.389755"), bd("0.247929"), bd("0.420935")),
				new HighEdgeCalibrationSlice(
						bd("0.30"), 199, bd("0.392041"), bd("-0.106533"),
						bd("0.636867"), bd("0.371859"), bd("0.214208"), bd("0.432161")));
	}

	private static List<AhFamilySnapshot> families() {
		return List.of(
				new AhFamilySnapshot(DiagnosticLineFamily.NEGATIVE_HANDICAP, 1314, bd("-0.009862"), bd("-0.064098")),
				new AhFamilySnapshot(DiagnosticLineFamily.ZERO, 334, bd("-0.025547"), bd("-0.021287")),
				new AhFamilySnapshot(DiagnosticLineFamily.POSITIVE_HANDICAP, 1314, bd("-0.053827"), bd("-0.002755")));
	}

	private static List<SideSnapshot> sides() {
		return List.of(
				new SideSnapshot("HOME", 1481, bd("-0.016772"), bd("-0.033025")),
				new SideSnapshot("AWAY", 1481, bd("-0.045497"), bd("-0.03109")));
	}

	private static List<SeasonStabilityRow> seasons() {
		return List.of(
				new SeasonStabilityRow(
						"2019/20", 592, bd("-0.029564"), bd("-0.033041"), 270, bd("-0.029426"),
						248, bd("0.008246"), 176, bd("0.048892")),
				new SeasonStabilityRow(
						"2020/21", 592, bd("-0.030881"), bd("-0.031318"), 262, bd("-0.058874"),
						230, bd("-0.114413"), 159, bd("-0.153836")),
				new SeasonStabilityRow(
						"2021/22", 574, bd("-0.028936"), bd("-0.02973"), 268, bd("-0.08056"),
						231, bd("-0.125498"), 147, bd("-0.077313")),
				new SeasonStabilityRow(
						"2022/23", 612, bd("-0.032014"), bd("-0.031275"), 271, bd("-0.064889"),
						235, bd("-0.041702"), 174, bd("-0.092931")),
				new SeasonStabilityRow(
						"2023/24", 592, bd("-0.034181"), bd("-0.034882"), 256, bd("-0.065898"),
						226, bd("-0.080996"), 150, bd("-0.1085")));
	}

	private static List<OverroundSeasonRow> overroundSeasons() {
		return List.of(
				new OverroundSeasonRow("2019/20", 296, bd("0.035916")),
				new OverroundSeasonRow("2020/21", 296, bd("0.037328")),
				new OverroundSeasonRow("2021/22", 287, bd("0.034891")),
				new OverroundSeasonRow("2022/23", 306, bd("0.037649")),
				new OverroundSeasonRow("2023/24", 296, bd("0.040899")));
	}

	private static List<StrategyRegressionSnapshot> strategies() {
		return List.of(
				new StrategyRegressionSnapshot("DEFENSIVE", 194, bd("-0.034371"), true),
				new StrategyRegressionSnapshot("BALANCED", 202, bd("-0.022986"), true),
				new StrategyRegressionSnapshot("GROWTH", 109, bd("-0.047705"), true),
				new StrategyRegressionSnapshot("FLAT_STAKE", 375, bd("-0.028569"), true));
	}

	private static List<NamedMeanInterval> intervals() {
		return List.of(
				interval("all candidates", 2962, "-0.032058", "-0.065289", "-0.00015"),
				interval("positive-edge", 1327, "-0.059846", "-0.107826", "-0.010799"),
				interval("edge >= 0.10", 806, "-0.074026", "-0.1316", "-0.011476"),
				interval("NEGATIVE_HANDICAP", 1314, "-0.064098", "-0.110495", "-0.013505"),
				interval("POSITIVE_HANDICAP", 1314, "-0.002755", "-0.053189", "0.043569"));
	}

	private static NamedMeanInterval interval(String label, int n, String mean, String low, String high) {
		return new NamedMeanInterval(
				label,
				new MeanConfidenceInterval(n, 2000, 20260816L, bd(mean), bd(low), bd(high)));
	}

	private static MarginCategoryCalibration margin(
			DiagnosticMarginCategory category, String predicted, String actual, int count) {
		return new MarginCategoryCalibration(category, bd(predicted), bd(actual), count);
	}

	private static BucketTrendRow row(String key, int n, String edge, String roi) {
		return new BucketTrendRow(key, n, bd(edge), bd(roi));
	}

	private static BigDecimal bd(String value) {
		return new BigDecimal(value);
	}
}
