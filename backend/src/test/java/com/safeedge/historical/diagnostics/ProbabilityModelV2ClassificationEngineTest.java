package com.safeedge.historical.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.CanonicalCompetition;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProbabilityModelV2ClassificationEngineTest {

	@Test
	void clearImprovementRequiresAllPredeclaredGates() {
		List<ProbabilityModelComparison> leagues = List.of(
				league(CanonicalCompetition.PREMIER_LEAGUE, "0.02", "0.12", "3.10", "3.08", "0.55", "0.40", "0.42", "0.40", "0.48", "0.12"),
				league(CanonicalCompetition.BUNDESLIGA, "0.00", "0.11", "3.20", "3.18", "0.54", "0.39", "0.41", "0.39", "0.47", "0.11"),
				league(CanonicalCompetition.SERIE_A, "-0.01", "0.10", "3.05", "3.04", "0.53", "0.40", "0.43", "0.40", "0.46", "0.10"));
		ProbabilityModelV2ClassificationEngine.ClassificationDecision decision =
				ProbabilityModelV2ClassificationEngine.classify(leagues);
		assertThat(decision.classification())
				.isEqualTo(ProbabilityModelV2Classification.MODEL_V2_CLEAR_STRUCTURAL_IMPROVEMENT);
	}

	@Test
	void partialWhenOnlySomeLeaguesImprove() {
		List<ProbabilityModelComparison> leagues = List.of(
				league(CanonicalCompetition.PREMIER_LEAGUE, "0.02", "0.08", "3.10", "3.09", "0.55", "0.40", "0.50", "0.38", "0.48", "0.12"),
				league(CanonicalCompetition.BUNDESLIGA, "0.00", "0.01", "3.20", "3.19", "0.54", "0.39", "0.53", "0.38", "0.47", "0.11"),
				league(CanonicalCompetition.SERIE_A, "0.01", "0.02", "3.05", "3.05", "0.53", "0.40", "0.52", "0.39", "0.46", "0.10"));
		assertThat(ProbabilityModelV2ClassificationEngine.classify(leagues).classification())
				.isEqualTo(ProbabilityModelV2Classification.MODEL_V2_PARTIAL_IMPROVEMENT);
	}

	@Test
	void worseWhenRankingAndWinGapsDegrade() {
		List<ProbabilityModelComparison> leagues = List.of(
				league(CanonicalCompetition.PREMIER_LEAGUE, "0.05", "0.00", "3.10", "3.11", "0.50", "0.40", "0.58", "0.32", "0.48", "0.20"),
				league(CanonicalCompetition.BUNDESLIGA, "0.04", "-0.02", "3.20", "3.21", "0.49", "0.39", "0.57", "0.31", "0.47", "0.19"),
				league(CanonicalCompetition.SERIE_A, "0.03", "0.03", "3.05", "3.06", "0.51", "0.40", "0.52", "0.39", "0.46", "0.18"));
		assertThat(ProbabilityModelV2ClassificationEngine.classify(leagues).classification())
				.isEqualTo(ProbabilityModelV2Classification.MODEL_V2_WORSE);
	}

	@Test
	void noMeaningfulImprovementWhenDeltasAreTiny() {
		List<ProbabilityModelComparison> leagues = List.of(
				league(CanonicalCompetition.PREMIER_LEAGUE, "0.017", "0.018", "3.06", "3.06", "0.55", "0.40", "0.54", "0.41", "0.48", "0.12"),
				league(CanonicalCompetition.BUNDESLIGA, "-0.011", "-0.010", "3.12", "3.12", "0.55", "0.39", "0.54", "0.40", "0.47", "0.11"),
				league(CanonicalCompetition.SERIE_A, "0.009", "0.010", "3.08", "3.07", "0.54", "0.40", "0.53", "0.41", "0.46", "0.10"));
		assertThat(ProbabilityModelV2ClassificationEngine.classify(leagues).classification())
				.isEqualTo(ProbabilityModelV2Classification.MODEL_V2_NO_MEANINGFUL_IMPROVEMENT);
	}

	@Test
	void classificationDoesNotInspectRoiFields() {
		assertThat(ProbabilityModelV2ClassificationEngine.class.getDeclaredMethods())
				.extracting(method -> method.getName())
				.contains("classify");
		assertThat(ProbabilityModelV2ClassificationEngine.SPEARMAN_MATERIAL_DELTA).isEqualByComparingTo("0.05");
		assertThat(ProbabilityModelV2ClassificationEngine.HIGH_EDGE_GAP_SHRINK).isEqualByComparingTo("0.03");
		assertThat(ProbabilityModelV2ClassificationEngine.LOG_LOSS_MATERIAL_WORSE).isEqualByComparingTo("0.02");
	}

	private static ProbabilityModelComparison league(
			CanonicalCompetition competition,
			String spearmanV1,
			String spearmanV2,
			String logLossV1,
			String logLossV2,
			String predWinV1,
			String actWin,
			String predWinV2,
			String actLoss,
			String p99WinV1,
			String p99WinV2) {
		ProbabilityModelV2LeagueMetrics v1 = metrics(
				spearmanV1, logLossV1, predWinV1, actWin, "0.28", actLoss, "0.60", p99WinV1);
		ProbabilityModelV2LeagueMetrics v2 = metrics(
				spearmanV2, logLossV2, predWinV2, actWin, "0.35", actLoss, "0.50", p99WinV2);
		return new ProbabilityModelComparison(competition, v1, v2, emptyQuantiles(), emptyQuantiles());
	}

	private static ProbabilityModelV2LeagueMetrics metrics(
			String spearman,
			String logLoss,
			String predWin,
			String actWin,
			String predLoss,
			String actLoss,
			String p99Edge,
			String p99Win) {
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
				quantiles(p99Edge),
				quantiles(p99Win),
				emptyQuantiles(),
				emptyQuantiles(),
				emptyQuantiles(),
				List.of(),
				new RhoSummary(0, null, null, null),
				List.of());
	}

	private static EdgeQuantiles emptyQuantiles() {
		return new EdgeQuantiles(null, null, null, null, null, null, null, null, null);
	}

	private static EdgeQuantiles quantiles(String p99) {
		return new EdgeQuantiles(
				null, null, null, null, null, null, null, new BigDecimal(p99), new BigDecimal(p99));
	}
}
