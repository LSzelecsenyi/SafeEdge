package com.safeedge.historical.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.strategy.StrategyPreset;
import com.safeedge.strategy.StrategyPresetFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoricalBacktestReportFormatterTest {

	@Test
	void namesHistoricalQuoteSourceAndNeverCallsItTippmix() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		var eval = HistoricalWalkForwardFixtures.match(
				HistoricalWalkForwardFixtures.S23, "H", "A", evalDate, 2, 0, 10);
		List<com.safeedge.historical.features.HistoricalMatchRecord> matches =
				new ArrayList<>(HistoricalWalkForwardFixtures.strongHomeWarmup(
						HistoricalWalkForwardFixtures.S22, evalDate, 1));
		matches.add(eval);
		HistoricalWalkForwardDataset dataset = new HistoricalWalkForwardDatasetBuilder()
				.build(
						matches,
						HistoricalWalkForwardFixtures.quotes(HistoricalWalkForwardFixtures.quote(
								eval,
								HistoricalQuoteSource.PINNACLE,
								HistoricalWalkForwardFixtures.LINE_ZERO,
								HistoricalWalkForwardFixtures.HOME_ODDS,
								HistoricalWalkForwardFixtures.AWAY_ODDS)),
						HistoricalWalkForwardFixtures.eval2023(HistoricalQuoteSource.PINNACLE));
		HistoricalStrategyComparisonResult comparison = new HistoricalStrategyComparisonEngine()
				.compare(
						dataset,
						new BigDecimal("100000"),
						List.of(new NamedStrategyConfig(
								"FLAT_STAKE", new StrategyPresetFactory().configFor(StrategyPreset.FLAT_STAKE))),
						null);
		String report = HistoricalBacktestReportFormatter.format(comparison);
		assertThat(report).contains("HISTORICAL QUOTE SOURCE = PINNACLE");
		assertThat(report).contains("not Tippmix odds");
		assertThat(report).contains("Synthetic ordering timestamps");
		assertThat(report).contains("not kickoff");
		assertThat(report.toLowerCase()).doesNotContain("tippmix historical");
		assertThat(report).doesNotContain("PROFITABLE");
		assertThat(report).doesNotContain("WINNING STRATEGY");
		assertThat(report).doesNotContain("SAFE TO USE");
	}
}
