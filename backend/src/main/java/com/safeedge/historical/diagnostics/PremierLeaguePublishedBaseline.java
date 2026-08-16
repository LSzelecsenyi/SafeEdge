package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import java.math.BigDecimal;
import java.util.List;

/**
 * Frozen Premier League Baseline 001/002 published numbers. Used so Baseline 003
 * does not rerun Premier League. Tests assert the source markdown still contains
 * these values.
 */
public final class PremierLeaguePublishedBaseline {

	public static final String BASELINE_001_REPORT = "docs/results/baseline-001-diagnostics.md";
	public static final String BASELINE_002_REPORT = "docs/results/baseline-002-edge-quality.md";

	private PremierLeaguePublishedBaseline() {
	}

	public static LeagueDiagnosticSnapshot snapshot() {
		return new LeagueDiagnosticSnapshot(
				CanonicalCompetition.PREMIER_LEAGUE,
				HistoricalQuoteSource.MARKET_AVERAGE,
				3420,
				1520,
				50,
				0,
				predictionQuality(),
				2940,
				1306,
				0,
				1634,
				bd("-0.029834"),
				bd("-0.029741"),
				bd("0.000092"),
				bd("0.0172"),
				bd("0.012664"),
				deciles(),
				edgeBuckets(),
				highEdge(),
				families(),
				sides(),
				seasons(),
				List.of(2021),
				bd("0.035373"),
				null,
				overroundSeasons(),
				strategies(),
				intervals(),
				List.of());
	}

	private static PredictionQualitySnapshot predictionQuality() {
		return new PredictionQualitySnapshot(
				1470,
				1470,
				bd("3.061582"),
				bd("1.562478"),
				bd("1.57551"),
				bd("1.262254"),
				bd("1.313605"),
				bd("0.443058"),
				bd("0.446939"),
				bd("0.225691"),
				bd("0.22381"),
				bd("0.331251"),
				bd("0.329252"),
				List.of(
						margin(DiagnosticMarginCategory.HOME_WIN_BY_2_PLUS, "0.248631", "0.241497", 355),
						margin(DiagnosticMarginCategory.HOME_WIN_BY_1, "0.194427", "0.205442", 302),
						margin(DiagnosticMarginCategory.DRAW, "0.225691", "0.22381", 329),
						margin(DiagnosticMarginCategory.AWAY_WIN_BY_1, "0.167237", "0.156463", 230),
						margin(DiagnosticMarginCategory.AWAY_WIN_BY_2_PLUS, "0.164015", "0.172789", 254)));
	}

	private static List<BucketTrendRow> deciles() {
		return List.of(
				row("decile 1 (lowest edge to highest)", 294, "-0.441356", "-0.023656"),
				row("decile 2 (lowest edge to highest)", 294, "-0.273428", "-0.08784"),
				row("decile 3 (lowest edge to highest)", 294, "-0.18579", "-0.011378"),
				row("decile 4 (lowest edge to highest)", 294, "-0.116097", "-0.011871"),
				row("decile 5 (lowest edge to highest)", 294, "-0.05699", "-0.005714"),
				row("decile 6 (lowest edge to highest)", 294, "-0.002114", "-0.040425"),
				row("decile 7 (lowest edge to highest)", 294, "0.05648", "-0.075544"),
				row("decile 8 (lowest edge to highest)", 294, "0.12715", "-0.05318"),
				row("decile 9 (lowest edge to highest)", 294, "0.213902", "0.059439"),
				row("decile 10 (lowest edge to highest)", 294, "0.379908", "-0.047245"));
	}

	private static List<BucketTrendRow> edgeBuckets() {
		return List.of(
				row("edge <= 0", 1634, "-0.194579", "-0.023299"),
				row("0 < edge < 0.02", 107, "0.010497", "-0.092944"),
				row("0.02 <= edge < 0.05", 155, "0.036024", "-0.109581"),
				row("0.05 <= edge < 0.10", 193, "0.075159", "-0.04829"),
				row("0.10 <= edge < 0.20", 377, "0.146841", "-0.007188"),
				row("0.20 <= edge < 0.30", 236, "0.245061", "-0.059619"),
				row("edge >= 0.30", 238, "0.402628", "0.015378"));
	}

	private static List<HighEdgeCalibrationSlice> highEdge() {
		return List.of(
				new HighEdgeCalibrationSlice(
						bd("0.10"), 851, bd("0.245616"), bd("-0.015417"),
						bd("0.553973"), bd("0.40188"), bd("0.269844"), bd("0.378378")),
				new HighEdgeCalibrationSlice(
						bd("0.20"), 474, bd("0.324177"), bd("-0.021962"),
						bd("0.601874"), bd("0.394515"), bd("0.235229"), bd("0.383966")),
				new HighEdgeCalibrationSlice(
						bd("0.30"), 238, bd("0.402628"), bd("0.015378"),
						bd("0.646961"), bd("0.415966"), bd("0.205045"), bd("0.37395")));
	}

	private static List<AhFamilySnapshot> families() {
		return List.of(
				new AhFamilySnapshot(DiagnosticLineFamily.NEGATIVE_HANDICAP, 1310, bd("-0.04501"), bd("-0.055611")),
				new AhFamilySnapshot(DiagnosticLineFamily.ZERO, 320, bd("-0.02485"), bd("-0.027844")),
				new AhFamilySnapshot(DiagnosticLineFamily.POSITIVE_HANDICAP, 1310, bd("-0.015874"), bd("-0.004336")));
	}

	private static List<SideSnapshot> sides() {
		return List.of(
				new SideSnapshot("HOME", 1470, bd("-0.025542"), bd("-0.033347")),
				new SideSnapshot("AWAY", 1470, bd("-0.034125"), bd("-0.026136")));
	}

	private static List<SeasonStabilityRow> seasons() {
		return List.of(
				new SeasonStabilityRow(
						"2019/20", 608, bd("-0.029664"), bd("-0.030748"), 276, bd("-0.041159"),
						242, bd("-0.034607"), 165, bd("-0.018152")),
				new SeasonStabilityRow(
						"2020/21", 872, bd("-0.028067"), bd("-0.02801"), 374, bd("-0.025428"),
						325, bd("0.008677"), 226, bd("0.022544")),
				new SeasonStabilityRow(
						"2022/23", 720, bd("-0.029422"), bd("-0.030736"), 322, bd("-0.052127"),
						286, bd("-0.026713"), 233, bd("-0.021202")),
				new SeasonStabilityRow(
						"2023/24", 740, bd("-0.032454"), bd("-0.029986"), 334, bd("-0.035075"),
						297, bd("-0.033923"), 227, bd("-0.045286")));
	}

	private static List<OverroundSeasonRow> overroundSeasons() {
		return List.of(
				new OverroundSeasonRow("2019/20", 304, bd("0.033883")),
				new OverroundSeasonRow("2020/21", 436, bd("0.033866")),
				new OverroundSeasonRow("2022/23", 360, bd("0.035578")),
				new OverroundSeasonRow("2023/24", 370, bd("0.038175")));
	}

	private static List<StrategyRegressionSnapshot> strategies() {
		return List.of(
				new StrategyRegressionSnapshot("DEFENSIVE", 117, bd("-0.030255"), true),
				new StrategyRegressionSnapshot("BALANCED", 122, bd("-0.021072"), true),
				new StrategyRegressionSnapshot("GROWTH", 123, bd("-0.021641"), true),
				new StrategyRegressionSnapshot("FLAT_STAKE", 588, bd("-0.038249"), true));
	}

	private static List<NamedMeanInterval> intervals() {
		return List.of(
				interval("all candidates", 2940, "-0.029741", "-0.062548", "0.002031"),
				interval("positive-edge", 1306, "-0.037802", "-0.082366", "0.011765"),
				interval("edge >= 0.10", 851, "-0.015417", "-0.073519", "0.043637"),
				interval("NEGATIVE_HANDICAP", 1310, "-0.055611", "-0.10271", "-0.007439"),
				interval("POSITIVE_HANDICAP", 1310, "-0.004336", "-0.053", "0.042573"));
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
