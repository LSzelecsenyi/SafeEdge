package com.safeedge.historical.evaluation;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.service.HistoricalStrategyComparisonService;
import com.safeedge.probability.ProbabilityModelConfig;
import com.safeedge.strategy.StrategyPreset;
import com.safeedge.strategy.StrategyPresetFactory;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-historical-backtest")
class ManualHistoricalBacktestRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualHistoricalBacktestRunner.class);
	private static final BigDecimal DEFAULT_STARTING_BANKROLL = new BigDecimal("100000");

	private final HistoricalStrategyComparisonService comparisonService;
	private final HistoricalBacktestProperties properties;
	private final StrategyPresetFactory presetFactory = new StrategyPresetFactory();

	ManualHistoricalBacktestRunner(
			HistoricalStrategyComparisonService comparisonService, HistoricalBacktestProperties properties) {
		this.comparisonService = comparisonService;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (blank(properties.competition())
				|| blank(properties.trainingFromSeason())
				|| blank(properties.fromSeason())
				|| blank(properties.toSeason())
				|| blank(properties.quoteSource())) {
			log.warn(
					"manual-historical-backtest profile is active but SAFEEDGE_HISTORICAL_BACKTEST_COMPETITION / TRAINING_FROM_SEASON / FROM_SEASON / TO_SEASON / QUOTE_SOURCE are not set; skipping");
			return;
		}
		CanonicalCompetition competition = CanonicalCompetition.valueOf(properties.competition().trim());
		int trainingFromSeason = Integer.parseInt(properties.trainingFromSeason().trim());
		int evaluationFromSeason = Integer.parseInt(properties.fromSeason().trim());
		int evaluationToSeason = Integer.parseInt(properties.toSeason().trim());
		HistoricalQuoteSource quoteSource = HistoricalQuoteSource.valueOf(properties.quoteSource().trim());
		WalkForwardEvaluationRequest request = new WalkForwardEvaluationRequest(
				competition,
				trainingFromSeason,
				evaluationFromSeason,
				evaluationToSeason,
				quoteSource,
				ProbabilityModelConfig.defaults());
		BigDecimal startingBankroll = blank(properties.startingBankroll())
				? DEFAULT_STARTING_BANKROLL
				: new BigDecimal(properties.startingBankroll().trim());
		Integer maxAcceptedBets = blank(properties.maxAcceptedBets())
				? null
				: Integer.valueOf(properties.maxAcceptedBets().trim());
		log.info(
				"Starting historical walk-forward evaluation: competition={} trainFrom={} eval={}→{} HISTORICAL QUOTE SOURCE={} startingBankroll={} (not Tippmix)",
				competition,
				trainingFromSeason,
				evaluationFromSeason,
				evaluationToSeason,
				quoteSource,
				startingBankroll);
		HistoricalStrategyComparisonResult comparison = comparisonService.compare(
				request, startingBankroll, defaultStrategies(), maxAcceptedBets);
		log.info("\n{}", HistoricalBacktestReportFormatter.format(comparison));
	}

	private List<NamedStrategyConfig> defaultStrategies() {
		return List.of(
				new NamedStrategyConfig("DEFENSIVE", presetFactory.configFor(StrategyPreset.DEFENSIVE)),
				new NamedStrategyConfig("BALANCED", presetFactory.configFor(StrategyPreset.BALANCED)),
				new NamedStrategyConfig("GROWTH", presetFactory.configFor(StrategyPreset.GROWTH)),
				new NamedStrategyConfig("FLAT_STAKE", presetFactory.configFor(StrategyPreset.FLAT_STAKE)));
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
