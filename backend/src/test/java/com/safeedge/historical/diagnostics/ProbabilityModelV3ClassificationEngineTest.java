package com.safeedge.historical.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.CanonicalCompetition;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProbabilityModelV3ClassificationEngineTest {

	@Test
	void clearImprovementRequiresPredeclaredGatesVersusBetterBaseline() {
		List<ProbabilityModelV3Comparison> leagues = List.of(
				league(CanonicalCompetition.PREMIER_LEAGUE, "0.02", "0.03", "0.09", "3.10", "3.08", "3.07", "0.55", "0.54", "0.40", "0.40", "0.28", "0.29", "0.42"),
				league(CanonicalCompetition.BUNDESLIGA, "0.00", "0.01", "0.07", "3.20", "3.18", "3.17", "0.54", "0.53", "0.39", "0.39", "0.28", "0.29", "0.41"),
				league(CanonicalCompetition.SERIE_A, "0.01", "0.02", "0.03", "3.05", "3.04", "3.04", "0.53", "0.52", "0.40", "0.40", "0.30", "0.31", "0.42"));
		assertThat(ProbabilityModelV3ClassificationEngine.classify(leagues).classification())
				.isEqualTo(ProbabilityModelV3Classification.MODEL_V3_CLEAR_IMPROVEMENT);
	}

	@Test
	void partialWhenSpearmanImprovesSlightlyWithoutClearCalibration() {
		List<ProbabilityModelV3Comparison> leagues = List.of(
				league(CanonicalCompetition.PREMIER_LEAGUE, "0.02", "0.03", "0.06", "3.10", "3.08", "3.08", "0.55", "0.54", "0.40", "0.40", "0.28", "0.29", "0.50"),
				league(CanonicalCompetition.BUNDESLIGA, "0.00", "0.01", "0.04", "3.20", "3.18", "3.18", "0.54", "0.53", "0.39", "0.39", "0.28", "0.29", "0.51"),
				league(CanonicalCompetition.SERIE_A, "0.01", "0.02", "0.02", "3.05", "3.04", "3.04", "0.53", "0.52", "0.40", "0.40", "0.30", "0.31", "0.50"));
		assertThat(ProbabilityModelV3ClassificationEngine.classify(leagues).classification())
				.isEqualTo(ProbabilityModelV3Classification.MODEL_V3_PARTIAL_IMPROVEMENT);
	}

	@Test
	void regressionWhenRankingWorsensInTwoLeagues() {
		List<ProbabilityModelV3Comparison> leagues = List.of(
				league(CanonicalCompetition.PREMIER_LEAGUE, "0.05", "0.04", "0.01", "3.10", "3.11", "3.12", "0.50", "0.51", "0.40", "0.32", "0.28", "0.27", "0.58"),
				league(CanonicalCompetition.BUNDESLIGA, "0.04", "0.03", "0.00", "3.20", "3.21", "3.22", "0.49", "0.50", "0.39", "0.31", "0.28", "0.27", "0.57"),
				league(CanonicalCompetition.SERIE_A, "0.03", "0.03", "0.03", "3.05", "3.06", "3.06", "0.51", "0.51", "0.40", "0.39", "0.30", "0.30", "0.52"));
		assertThat(ProbabilityModelV3ClassificationEngine.classify(leagues).classification())
				.isEqualTo(ProbabilityModelV3Classification.MODEL_V3_REGRESSION);
	}

	@Test
	void noMeaningfulImprovementWhenDeltasAreTiny() {
		List<ProbabilityModelV3Comparison> leagues = List.of(
				league(CanonicalCompetition.PREMIER_LEAGUE, "0.017", "0.018", "0.019", "3.06", "3.06", "3.06", "0.55", "0.54", "0.40", "0.41", "0.28", "0.29", "0.54"),
				league(CanonicalCompetition.BUNDESLIGA, "-0.011", "-0.010", "-0.009", "3.12", "3.12", "3.12", "0.55", "0.54", "0.39", "0.40", "0.28", "0.29", "0.54"),
				league(CanonicalCompetition.SERIE_A, "0.009", "0.010", "0.011", "3.08", "3.07", "3.07", "0.54", "0.53", "0.40", "0.41", "0.30", "0.31", "0.53"));
		assertThat(ProbabilityModelV3ClassificationEngine.classify(leagues).classification())
				.isEqualTo(ProbabilityModelV3Classification.MODEL_V3_NO_MEANINGFUL_IMPROVEMENT);
	}

	@Test
	void gatesAreFrozenAndDoNotInspectRoi() {
		assertThat(ProbabilityModelV3ClassificationEngine.SPEARMAN_MATERIAL_DELTA).isEqualByComparingTo("0.05");
		assertThat(ProbabilityModelV3ClassificationEngine.SPEARMAN_DETERIORATION).isEqualByComparingTo("0.02");
		assertThat(ProbabilityModelV3ClassificationEngine.HIGH_EDGE_GAP_SHRINK).isEqualByComparingTo("0.03");
		assertThat(ProbabilityModelV3ClassificationEngine.LOG_LOSS_MATERIAL_WORSE).isEqualByComparingTo("0.02");
		assertThat(ProbabilityModelV3ClassificationEngine.class.getDeclaredMethods())
				.extracting(method -> method.getName() + java.util.Arrays.toString(method.getParameterTypes()).toLowerCase())
				.noneMatch(signature -> signature.contains("roi"));
	}

	private static ProbabilityModelV3Comparison league(
			CanonicalCompetition competition,
			String spearmanV1,
			String spearmanV2,
			String spearmanV3,
			String logLossV1,
			String logLossV2,
			String logLossV3,
			String predWinV1,
			String predWinV2,
			String actWin,
			String actLoss,
			String predLossV1,
			String predLossV2,
			String predWinV3) {
		return new ProbabilityModelV3Comparison(
				competition,
				metrics(spearmanV1, logLossV1, predWinV1, actWin, predLossV1, actLoss),
				metrics(spearmanV2, logLossV2, predWinV2, actWin, predLossV2, actLoss),
				metrics(spearmanV3, logLossV3, predWinV3, actWin, "0.35", actLoss),
				emptyExtras(),
				emptyExtras(),
				emptyExtras());
	}

	private static ProbabilityModelV2LeagueMetrics metrics(
			String spearman,
			String logLoss,
			String predWin,
			String actWin,
			String predLoss,
			String actLoss) {
		HighEdgeCalibrationSnapshot high10 = new HighEdgeCalibrationSnapshot(
				new BigDecimal("0.10"),
				100,
				new BigDecimal("0.15"),
				new BigDecimal("-0.10"),
				new BigDecimal(predWin),
				new BigDecimal(actWin),
				new BigDecimal(predLoss),
				new BigDecimal(actLoss));
		HighEdgeCalibrationSnapshot high20 = new HighEdgeCalibrationSnapshot(
				new BigDecimal("0.20"), 40, new BigDecimal("0.25"), new BigDecimal("-0.12"),
				new BigDecimal(predWin), new BigDecimal(actWin), new BigDecimal(predLoss), new BigDecimal(actLoss));
		HighEdgeCalibrationSnapshot high30 = new HighEdgeCalibrationSnapshot(
				new BigDecimal("0.30"), 10, new BigDecimal("0.35"), new BigDecimal("-0.15"),
				new BigDecimal(predWin), new BigDecimal(actWin), new BigDecimal(predLoss), new BigDecimal(actLoss));
		return new ProbabilityModelV2LeagueMetrics(
				100,
				200,
				new BigDecimal(logLoss),
				100,
				new GoalCalibrationDiagnostics(100, null, null, null, null, null, null, null, null, null, null, null, null),
				List.of(),
				new LowScoreCalibration(
						100,
						new LowScoreCellCalibration("0-0", BigDecimal.ZERO, BigDecimal.ZERO, 0),
						new LowScoreCellCalibration("1-0", BigDecimal.ZERO, BigDecimal.ZERO, 0),
						new LowScoreCellCalibration("0-1", BigDecimal.ZERO, BigDecimal.ZERO, 0),
						new LowScoreCellCalibration("1-1", BigDecimal.ZERO, BigDecimal.ZERO, 0)),
				new RankQualityStats(200, new BigDecimal(spearman), BigDecimal.ZERO),
				List.of(),
				3,
				high10,
				high20,
				high30,
				new BigDecimal("-0.03"),
				new BigDecimal("-0.03"),
				emptyQuantiles(),
				emptyQuantiles(),
				emptyQuantiles(),
				emptyQuantiles(),
				emptyQuantiles(),
				List.of(),
				new RhoSummary(0, null, null, null),
				List.of());
	}

	private static ProbabilityModelV3Extras emptyExtras() {
		OutcomeCalibration empty = new OutcomeCalibration(null, null, null);
		HighEdgeFiveWaySnapshot five = new HighEdgeFiveWaySnapshot(
				new BigDecimal("0.10"), 0, null, null, empty, empty, empty, empty, empty);
		return new ProbabilityModelV3Extras(
				0, 0, 0, 0, 0, 0, 0, 0, 0,
				new HighEdgeCalibrationSnapshot(new BigDecimal("0.03"), 0, null, null, null, null, null, null),
				new HighEdgeCalibrationSnapshot(new BigDecimal("0.05"), 0, null, null, null, null, null, null),
				five, five, five, five, five,
				List.of(), List.of(), List.of(), List.of(),
				new JointDixonColesOptimizerSummary(
						0, 0, 0, null, null, 0, emptyQuantiles(), emptyQuantiles(), emptyQuantiles(),
						new RhoSummary(0, null, null, null), true, false));
	}

	private static EdgeQuantiles emptyQuantiles() {
		return new EdgeQuantiles(null, null, null, null, null, null, null, null, null);
	}
}
