package com.safeedge.historical.service;

import com.safeedge.historical.evaluation.HistoricalBacktestEvaluationResult;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonEngine;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardDataset;
import com.safeedge.historical.evaluation.NamedStrategyConfig;
import com.safeedge.historical.evaluation.WalkForwardEvaluationRequest;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds the walk-forward candidate dataset once, then runs each supplied
 * {@link StrategyConfig} independently. Probability predictions are not rebuilt
 * per strategy.
 */
@Service
public class HistoricalStrategyComparisonService {

	private final HistoricalWalkForwardEvaluationService evaluationService;
	private final HistoricalStrategyComparisonEngine comparisonEngine = new HistoricalStrategyComparisonEngine();

	public HistoricalStrategyComparisonService(HistoricalWalkForwardEvaluationService evaluationService) {
		this.evaluationService = evaluationService;
	}

	public HistoricalWalkForwardDataset buildDataset(WalkForwardEvaluationRequest request) {
		return evaluationService.buildDataset(request);
	}

	public HistoricalBacktestEvaluationResult evaluate(
			WalkForwardEvaluationRequest request,
			BigDecimal startingBankroll,
			StrategyConfig strategyConfig,
			Integer maxAcceptedBets) {
		HistoricalWalkForwardDataset dataset = evaluationService.buildDataset(request);
		return comparisonEngine.evaluate(dataset, startingBankroll, strategyConfig, maxAcceptedBets);
	}

	public HistoricalStrategyComparisonResult compare(
			WalkForwardEvaluationRequest request,
			BigDecimal startingBankroll,
			List<NamedStrategyConfig> strategies,
			Integer maxAcceptedBets) {
		HistoricalWalkForwardDataset dataset = evaluationService.buildDataset(request);
		return comparisonEngine.compare(dataset, startingBankroll, strategies, maxAcceptedBets);
	}
}
