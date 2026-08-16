package com.safeedge.historical.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.evaluation.WalkForwardEvaluationRequest;
import com.safeedge.probability.ProbabilityModelConfig;
import com.safeedge.probability.ProbabilityModelV2Config;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProbabilityModelV2DevelopmentServiceTest {

	@Test
	void refusesLaLiga() {
		ProbabilityModelV2DevelopmentService service = new ProbabilityModelV2DevelopmentService(null);
		WalkForwardEvaluationRequest request = request(CanonicalCompetition.LA_LIGA);
		assertThatThrownBy(() -> service.diagnose(
						request, ProbabilityModelV2Config.defaults(), new BigDecimal("100000"), List.of(), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reserved");
	}

	@Test
	void refusesLigue1() {
		ProbabilityModelV2DevelopmentService service = new ProbabilityModelV2DevelopmentService(null);
		WalkForwardEvaluationRequest request = request(CanonicalCompetition.LIGUE_1);
		assertThatThrownBy(() -> service.diagnose(
						request, ProbabilityModelV2Config.defaults(), new BigDecimal("100000"), List.of(), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reserved");
	}

	private static WalkForwardEvaluationRequest request(CanonicalCompetition competition) {
		return new WalkForwardEvaluationRequest(
				competition, 2014, 2019, 2023, HistoricalQuoteSource.MARKET_AVERAGE, ProbabilityModelConfig.defaults());
	}
}
