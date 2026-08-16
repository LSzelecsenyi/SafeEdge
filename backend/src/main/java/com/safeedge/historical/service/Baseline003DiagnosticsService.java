package com.safeedge.historical.service;

import com.safeedge.historical.diagnostics.BaselineDiagnosticsEngine;
import com.safeedge.historical.diagnostics.BaselineDiagnosticsReport;
import com.safeedge.historical.diagnostics.CrossLeagueComparison;
import com.safeedge.historical.diagnostics.CrossLeagueComparisonEngine;
import com.safeedge.historical.diagnostics.EdgeQualityDiagnosticsEngine;
import com.safeedge.historical.diagnostics.EdgeQualityReport;
import com.safeedge.historical.diagnostics.LeagueCoverageAnalyzer;
import com.safeedge.historical.diagnostics.LeagueCoverageSnapshot;
import com.safeedge.historical.diagnostics.LeagueDiagnosticSnapshot;
import com.safeedge.historical.diagnostics.PremierLeaguePublishedBaseline;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonEngine;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;
import com.safeedge.historical.evaluation.NamedStrategyConfig;
import com.safeedge.historical.evaluation.WalkForwardEvaluationRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * One Bundesliga walk-forward pass, Baseline 001 + 002 diagnostics, then
 * comparison against published Premier League numbers. Does not retune Poisson.
 */
@Service
public class Baseline003DiagnosticsService {

	private final HistoricalWalkForwardEvaluationService evaluationService;
	private final HistoricalAhCoverageService coverageService;
	private final HistoricalStrategyComparisonEngine comparisonEngine = new HistoricalStrategyComparisonEngine();
	private final BaselineDiagnosticsEngine baselineEngine = new BaselineDiagnosticsEngine();
	private final EdgeQualityDiagnosticsEngine edgeQualityEngine = new EdgeQualityDiagnosticsEngine();

	public Baseline003DiagnosticsService(
			HistoricalWalkForwardEvaluationService evaluationService, HistoricalAhCoverageService coverageService) {
		this.evaluationService = evaluationService;
		this.coverageService = coverageService;
	}

	public Baseline003DiagnosticsRun diagnose(
			WalkForwardEvaluationRequest request,
			BigDecimal startingBankroll,
			List<NamedStrategyConfig> strategies,
			Integer maxAcceptedBets) {
		if (request.competition() != CanonicalCompetition.BUNDESLIGA) {
			throw new IllegalArgumentException(
					"Baseline 003 replication expects BUNDESLIGA, got " + request.competition());
		}
		LeagueCoverageSnapshot coverage = LeagueCoverageAnalyzer.analyze(
				request.competition(), request.quoteSource(), coverageService.report());
		HistoricalWalkForwardBuildOutput output = evaluationService.buildWithPredictions(request);
		HistoricalStrategyComparisonResult comparison =
				comparisonEngine.compare(output.dataset(), startingBankroll, strategies, maxAcceptedBets);
		BaselineDiagnosticsReport baseline =
				baselineEngine.analyze(output.dataset(), output.predictions(), comparison.strategyResults());
		EdgeQualityReport edgeQuality = edgeQualityEngine.analyze(output.dataset(), comparison.strategyResults());
		LeagueDiagnosticSnapshot bundesliga = LeagueDiagnosticSnapshot.fromReports(
				baseline, edgeQuality, coverage.missingEvaluationStartYears());
		CrossLeagueComparison crossLeague =
				CrossLeagueComparisonEngine.compare(PremierLeaguePublishedBaseline.snapshot(), bundesliga);
		return new Baseline003DiagnosticsRun(coverage, output, comparison, baseline, edgeQuality, crossLeague);
	}
}
