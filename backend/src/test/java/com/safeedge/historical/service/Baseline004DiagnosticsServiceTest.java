package com.safeedge.historical.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.evaluation.WalkForwardEvaluationRequest;
import com.safeedge.probability.ProbabilityModelConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class Baseline004DiagnosticsServiceTest {

	@Test
	void rejectsNonSerieACompetition() {
		Baseline004DiagnosticsService service = new Baseline004DiagnosticsService(null, null);
		WalkForwardEvaluationRequest request = new WalkForwardEvaluationRequest(
				CanonicalCompetition.BUNDESLIGA,
				2014,
				2019,
				2023,
				HistoricalQuoteSource.MARKET_AVERAGE,
				ProbabilityModelConfig.defaults());
		assertThatThrownBy(() -> service.diagnose(request, new BigDecimal("100000"), List.of(), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("SERIE_A");
	}
}
