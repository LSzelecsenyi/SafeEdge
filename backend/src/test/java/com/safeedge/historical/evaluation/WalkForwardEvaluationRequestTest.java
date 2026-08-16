package com.safeedge.historical.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.probability.ProbabilityModelConfig;
import org.junit.jupiter.api.Test;

class WalkForwardEvaluationRequestTest {

	@Test
	void rejectsTrainingAfterEvaluationStart() {
		assertThatThrownBy(() -> new WalkForwardEvaluationRequest(
						CanonicalCompetition.PREMIER_LEAGUE,
						2019,
						2018,
						2023,
						HistoricalQuoteSource.PINNACLE,
						ProbabilityModelConfig.defaults()))
				.isInstanceOf(HistoricalDataException.class)
				.hasMessageContaining("Season range");
	}

	@Test
	void rejectsEvaluationToBeforeFrom() {
		assertThatThrownBy(() -> new WalkForwardEvaluationRequest(
						CanonicalCompetition.PREMIER_LEAGUE,
						2014,
						2023,
						2018,
						HistoricalQuoteSource.PINNACLE,
						ProbabilityModelConfig.defaults()))
				.isInstanceOf(HistoricalDataException.class)
				.hasMessageContaining("Season range");
	}

	@Test
	void acceptsTrainingEqualToEvaluationStart() {
		WalkForwardEvaluationRequest request = new WalkForwardEvaluationRequest(
				CanonicalCompetition.PREMIER_LEAGUE,
				2018,
				2018,
				2023,
				HistoricalQuoteSource.BET365,
				ProbabilityModelConfig.defaults());
		assertThat(request.trainingFromSeason()).isEqualTo(2018);
		assertThat(request.quoteSource()).isEqualTo(HistoricalQuoteSource.BET365);
	}
}
