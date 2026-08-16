package com.safeedge.historical.service;

import com.safeedge.historical.diagnostics.BaselineDiagnosticsEngine;
import com.safeedge.historical.diagnostics.BaselineDiagnosticsReport;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonEngine;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;
import com.safeedge.historical.evaluation.NamedStrategyConfig;
import com.safeedge.historical.evaluation.WalkForwardEvaluationRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Assembles one walk-forward pass and one backtest per strategy, then runs the
 * pure diagnostics engine. Does not query Tippmix or rebuild Poisson per bucket.
 */
@Service
public class BaselineDiagnosticsService {

	private final HistoricalWalkForwardEvaluationService evaluationService;
	private final HistoricalStrategyComparisonEngine comparisonEngine = new HistoricalStrategyComparisonEngine();
	private final BaselineDiagnosticsEngine diagnosticsEngine = new BaselineDiagnosticsEngine();

	public BaselineDiagnosticsService(HistoricalWalkForwardEvaluationService evaluationService) {
		this.evaluationService = evaluationService;
	}

	public BaselineDiagnosticsRun diagnose(
			WalkForwardEvaluationRequest request,
			BigDecimal startingBankroll,
			List<NamedStrategyConfig> strategies,
			Integer maxAcceptedBets) {
		HistoricalWalkForwardBuildOutput output = evaluationService.buildWithPredictions(request);
		HistoricalStrategyComparisonResult comparison =
				comparisonEngine.compare(output.dataset(), startingBankroll, strategies, maxAcceptedBets);
		BaselineDiagnosticsReport report =
				diagnosticsEngine.analyze(output.dataset(), output.predictions(), comparison.strategyResults());
		return new BaselineDiagnosticsRun(output, comparison, report);
	}
}
