package com.safeedge.historical.evaluation;

import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.AWAY_ODDS;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.HOME_ODDS;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.LINE_ZERO;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.S22;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.S23;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.eval2023;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.flatMinEdge;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.match;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.quote;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.quotes;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.strongHomeWarmup;
import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.backtest.BacktestBetResult;
import com.safeedge.backtest.BacktestResult;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.features.HistoricalMatchRecord;
import com.safeedge.strategy.StrategyDecisionReason;
import com.safeedge.strategy.StrategyPreset;
import com.safeedge.strategy.StrategyPresetFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoricalStrategyComparisonEngineTest {

	private final HistoricalWalkForwardDatasetBuilder builder = new HistoricalWalkForwardDatasetBuilder();
	private final HistoricalStrategyComparisonEngine comparison = new HistoricalStrategyComparisonEngine();

	@Test
	void samePreparedDatasetRunsIndependentStrategiesWithoutRebuildingCandidates() {
		HistoricalWalkForwardDataset dataset = dataset();
		HistoricalStrategyComparisonResult result = comparison.compare(
				dataset,
				new BigDecimal("100000"),
				List.of(
						new NamedStrategyConfig("FLAT_1PCT", HistoricalWalkForwardFixtures.flat("0.01", "0")),
						new NamedStrategyConfig("FLAT_2PCT", HistoricalWalkForwardFixtures.flat("0.02", "0"))),
				null);
		assertThat(result.dataset()).isSameAs(dataset);
		assertThat(result.strategyResults()).hasSize(2);
		assertThat(result.strategyResults().get(0).result().counts().opportunitiesProcessed())
				.isEqualTo(dataset.opportunities().size());
		assertThat(result.strategyResults().get(1).result().counts().opportunitiesProcessed())
				.isEqualTo(dataset.opportunities().size());
		assertThat(result.strategyResults().get(0).result().counts().betsAccepted())
				.isEqualTo(result.strategyResults().get(1).result().counts().betsAccepted());
		assertThat(result.strategyResults().get(0).result().acceptedBetResults().getFirst().stake())
				.isNotEqualByComparingTo(result.strategyResults().get(1).result().acceptedBetResults().getFirst().stake());
	}

	@Test
	void negativeEvStaysInStreamAndIsRejectedByMinimumEdge() {
		HistoricalWalkForwardDataset dataset = dataset();
		assertThat(dataset.stats().negativeEvCandidates()).isGreaterThan(0);
		assertThat(dataset.opportunities())
				.extracting(opportunity -> opportunity.opportunity().edge())
				.anySatisfy(edge -> assertThat(edge).isNegative());
		HistoricalBacktestEvaluationResult evaluated = comparison.evaluate(
				dataset, new BigDecimal("100000"), flatMinEdge("0"), null);
		assertThat(evaluated.backtest().counts().opportunitiesProcessed()).isEqualTo(dataset.opportunities().size());
		assertThat(evaluated.backtest().acceptedBetResults())
				.allSatisfy(bet -> assertThat(bet.edge()).isGreaterThanOrEqualTo(BigDecimal.ZERO));
		assertThat(evaluated.backtest().rejectionReasonCounts().get(StrategyDecisionReason.EDGE_BELOW_MINIMUM))
				.isGreaterThanOrEqualTo((long) dataset.stats().negativeEvCandidates());
	}

	@Test
	void presetConfigsAreDataOnlyAndCanBeComparedOnTheSameDataset() {
		HistoricalWalkForwardDataset dataset = dataset();
		StrategyPresetFactory factory = new StrategyPresetFactory();
		HistoricalStrategyComparisonResult result = comparison.compare(
				dataset,
				new BigDecimal("100000"),
				List.of(
						new NamedStrategyConfig("DEFENSIVE", factory.configFor(StrategyPreset.DEFENSIVE)),
						new NamedStrategyConfig("FLAT_STAKE", factory.configFor(StrategyPreset.FLAT_STAKE))),
				null);
		assertThat(result.strategyResults().get(0).result().counts().opportunitiesProcessed())
				.isEqualTo(result.strategyResults().get(1).result().counts().opportunitiesProcessed());
	}

	@Test
	void fullWalkForwardToBacktestKeepsProfitInvariant() {
		HistoricalWalkForwardDataset dataset = dataset();
		BacktestResult result = comparison.evaluate(
						dataset, new BigDecimal("100000"), flatMinEdge("0"), null)
				.backtest();
		BigDecimal fromEquity = result.finalTotalEquity().subtract(result.startingBankroll());
		BigDecimal fromBets = result.acceptedBetResults().stream()
				.map(BacktestBetResult::profit)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		assertThat(result.metrics().totalProfit()).isEqualByComparingTo(fromEquity);
		assertThat(result.metrics().totalProfit()).isEqualByComparingTo(fromBets);
		assertThat(result.counts().betsAccepted()).isPositive();
	}

	private HistoricalWalkForwardDataset dataset() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 2, 0, 10);
		List<HistoricalMatchRecord> matches = new ArrayList<>(strongHomeWarmup(S22, evalDate, 1));
		matches.add(eval);
		return builder.build(
				matches,
				quotes(quote(eval, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS)),
				eval2023(HistoricalQuoteSource.PINNACLE));
	}
}
