package com.safeedge.historical.service;

import com.safeedge.historical.diagnostics.EdgeQualityDiagnosticsEngine;
import com.safeedge.historical.diagnostics.EdgeQualityReport;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonEngine;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;
import com.safeedge.historical.evaluation.NamedStrategyConfig;
import com.safeedge.historical.evaluation.WalkForwardEvaluationRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * One walk-forward pass, four strategy backtests for regression snapshot, then
 * Baseline 002 edge-quality diagnostics. Does not rebuild Poisson per bucket.
 */
@Service
public class EdgeQualityDiagnosticsService {

	private final HistoricalWalkForwardEvaluationService evaluationService;
	private final HistoricalStrategyComparisonEngine comparisonEngine = new HistoricalStrategyComparisonEngine();
	private final EdgeQualityDiagnosticsEngine diagnosticsEngine = new EdgeQualityDiagnosticsEngine();

	public EdgeQualityDiagnosticsService(HistoricalWalkForwardEvaluationService evaluationService) {
		this.evaluationService = evaluationService;
	}

	public EdgeQualityDiagnosticsRun diagnose(
			WalkForwardEvaluationRequest request,
			BigDecimal startingBankroll,
			List<NamedStrategyConfig> strategies,
			Integer maxAcceptedBets) {
		HistoricalWalkForwardBuildOutput output = evaluationService.buildWithPredictions(request);
		HistoricalStrategyComparisonResult comparison =
				comparisonEngine.compare(output.dataset(), startingBankroll, strategies, maxAcceptedBets);
		EdgeQualityReport report = diagnosticsEngine.analyze(output.dataset(), comparison.strategyResults());
		return new EdgeQualityDiagnosticsRun(output, comparison, report);
	}
}
