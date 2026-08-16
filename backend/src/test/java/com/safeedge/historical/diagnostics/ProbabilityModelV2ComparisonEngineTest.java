package com.safeedge.historical.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.candidate.ScoreProbability;
import com.safeedge.candidate.ScoreProbabilityDistribution;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.evaluation.HistoricalPredictionSnapshot;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProbabilityModelV2ComparisonEngineTest {

	@Test
	void lowScoreCalibrationCountsOnlyFourCells() {
		ScoreProbabilityDistribution distribution = new ScoreProbabilityDistribution(List.of(
				new ScoreProbability(new MatchScore(0, 0), new BigDecimal("0.20")),
				new ScoreProbability(new MatchScore(1, 0), new BigDecimal("0.30")),
				new ScoreProbability(new MatchScore(0, 1), new BigDecimal("0.10")),
				new ScoreProbability(new MatchScore(1, 1), new BigDecimal("0.40"))));
		HistoricalPredictionSnapshot first = snapshot("a", distribution, new MatchScore(0, 0));
		HistoricalPredictionSnapshot second = snapshot("b", distribution, new MatchScore(2, 2));
		LowScoreCalibration calibration = ProbabilityModelV2ComparisonEngine.lowScores(List.of(first, second));
		assertThat(calibration.predictionCount()).isEqualTo(2);
		assertThat(calibration.score00().averagePredicted()).isEqualByComparingTo("0.20");
		assertThat(calibration.score00().actualFrequency()).isEqualByComparingTo("0.5");
		assertThat(calibration.score00().actualCount()).isEqualTo(1);
		assertThat(calibration.score11().actualCount()).isZero();
	}

	@Test
	void decileInversionsIgnoreLowSampleRows() {
		List<EdgeQualityGroupSummary> deciles = List.of(
				summary("d1", 40, "0.10"),
				summary("d2", 10, "-0.50"),
				summary("d3", 40, "0.05"),
				summary("d4", 40, "0.20"));
		assertThat(ProbabilityModelV2ComparisonEngine.decileRoiInversions(deciles)).isEqualTo(1);
	}

	private static EdgeQualityGroupSummary summary(String key, int n, String roi) {
		SettlementCalibration calibration = new SettlementCalibration(
				new OutcomeCalibration(null, null, null),
				new OutcomeCalibration(null, null, null),
				new OutcomeCalibration(null, null, null),
				new OutcomeCalibration(null, null, null),
				new OutcomeCalibration(null, null, null));
		return new EdgeQualityGroupSummary(
				key,
				n,
				n < 30,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ONE,
				new BigDecimal(roi),
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				new SettlementCounts(0, 0, 0, 0, 0),
				calibration);
	}

	private static HistoricalPredictionSnapshot snapshot(
			String eventId, ScoreProbabilityDistribution distribution, MatchScore actual) {
		return new HistoricalPredictionSnapshot(
				eventId,
				new FootballSeason(2019, 2020),
				LocalDate.of(2019, 8, 10),
				BigDecimal.ONE,
				BigDecimal.ONE,
				distribution,
				actual);
	}
}
