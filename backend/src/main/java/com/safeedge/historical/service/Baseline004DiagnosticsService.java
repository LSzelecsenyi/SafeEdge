package com.safeedge.historical.service;

import com.safeedge.historical.diagnostics.BaselineDiagnosticsEngine;
import com.safeedge.historical.diagnostics.BaselineDiagnosticsReport;
import com.safeedge.historical.diagnostics.BundesligaPublishedBaseline;
import com.safeedge.historical.diagnostics.CrossLeagueComparisonEngine;
import com.safeedge.historical.diagnostics.EdgeQualityDiagnosticsEngine;
import com.safeedge.historical.diagnostics.EdgeQualityReport;
import com.safeedge.historical.diagnostics.LeagueCoverageAnalyzer;
import com.safeedge.historical.diagnostics.LeagueCoverageSnapshot;
import com.safeedge.historical.diagnostics.LeagueDiagnosticSnapshot;
import com.safeedge.historical.diagnostics.PremierLeaguePublishedBaseline;
import com.safeedge.historical.diagnostics.ThreeLeagueComparison;
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
 * One Serie A walk-forward pass, Baseline 001 + 002 diagnostics, then comparison
 * against published Premier League and Bundesliga numbers. Does not retune Poisson.
 */
@Service
public class Baseline004DiagnosticsService {

	private final HistoricalWalkForwardEvaluationService evaluationService;
	private final HistoricalAhCoverageService coverageService;
	private final HistoricalStrategyComparisonEngine comparisonEngine = new HistoricalStrategyComparisonEngine();
	private final BaselineDiagnosticsEngine baselineEngine = new BaselineDiagnosticsEngine();
	private final EdgeQualityDiagnosticsEngine edgeQualityEngine = new EdgeQualityDiagnosticsEngine();

	public Baseline004DiagnosticsService(
			HistoricalWalkForwardEvaluationService evaluationService, HistoricalAhCoverageService coverageService) {
		this.evaluationService = evaluationService;
		this.coverageService = coverageService;
	}

	public Baseline004DiagnosticsRun diagnose(
			WalkForwardEvaluationRequest request,
			BigDecimal startingBankroll,
			List<NamedStrategyConfig> strategies,
			Integer maxAcceptedBets) {
		if (request.competition() != CanonicalCompetition.SERIE_A) {
			throw new IllegalArgumentException(
					"Baseline 004 replication expects SERIE_A, got " + request.competition());
		}
		LeagueCoverageSnapshot coverage = LeagueCoverageAnalyzer.analyze(
				request.competition(), request.quoteSource(), coverageService.report());
		HistoricalWalkForwardBuildOutput output = evaluationService.buildWithPredictions(request);
		HistoricalStrategyComparisonResult comparison =
				comparisonEngine.compare(output.dataset(), startingBankroll, strategies, maxAcceptedBets);
		BaselineDiagnosticsReport baseline =
				baselineEngine.analyze(output.dataset(), output.predictions(), comparison.strategyResults());
		EdgeQualityReport edgeQuality = edgeQualityEngine.analyze(output.dataset(), comparison.strategyResults());
		LeagueDiagnosticSnapshot serieA = LeagueDiagnosticSnapshot.fromReports(
				baseline, edgeQuality, coverage.missingEvaluationStartYears());
		ThreeLeagueComparison threeLeague = CrossLeagueComparisonEngine.compareThree(
				PremierLeaguePublishedBaseline.snapshot(), BundesligaPublishedBaseline.snapshot(), serieA);
		return new Baseline004DiagnosticsRun(coverage, output, comparison, baseline, edgeQuality, threeLeague);
	}
}
