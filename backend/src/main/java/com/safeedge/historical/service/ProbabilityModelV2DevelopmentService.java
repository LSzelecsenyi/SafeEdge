package com.safeedge.historical.service;

import com.safeedge.historical.diagnostics.BaselineDiagnosticsEngine;
import com.safeedge.historical.diagnostics.BaselineDiagnosticsReport;
import com.safeedge.historical.diagnostics.EdgeQualityDiagnosticsEngine;
import com.safeedge.historical.diagnostics.EdgeQualityReport;
import com.safeedge.historical.diagnostics.ProbabilityModelComparison;
import com.safeedge.historical.diagnostics.ProbabilityModelDevelopmentLeagues;
import com.safeedge.historical.diagnostics.ProbabilityModelV2ClassificationEngine;
import com.safeedge.historical.diagnostics.ProbabilityModelV2ComparisonEngine;
import com.safeedge.historical.diagnostics.ProbabilityModelV2DevelopmentReport;
import com.safeedge.historical.diagnostics.ProbabilityModelV2LeagueRun;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonEngine;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;
import com.safeedge.historical.evaluation.NamedStrategyConfig;
import com.safeedge.historical.evaluation.WalkForwardEvaluationRequest;
import com.safeedge.probability.DixonColesFitRecorder;
import com.safeedge.probability.FootballProbabilityModel;
import com.safeedge.probability.PoissonFootballProbabilityModel;
import com.safeedge.probability.ProbabilityModelV2Config;
import com.safeedge.probability.RegularizedDixonColesFootballProbabilityModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Runs frozen Probability Model v1 and Regularized Dixon-Coles v2 on the same
 * persisted walk-forward window. Does not change CandidateEngine or BacktestEngine.
 * Refuses reserved validation leagues.
 */
@Service
public class ProbabilityModelV2DevelopmentService {

	private final HistoricalWalkForwardEvaluationService evaluationService;
	private final HistoricalStrategyComparisonEngine comparisonEngine = new HistoricalStrategyComparisonEngine();
	private final BaselineDiagnosticsEngine baselineEngine = new BaselineDiagnosticsEngine();
	private final EdgeQualityDiagnosticsEngine edgeQualityEngine = new EdgeQualityDiagnosticsEngine();
	private final ProbabilityModelV2ComparisonEngine modelComparisonEngine = new ProbabilityModelV2ComparisonEngine();

	public ProbabilityModelV2DevelopmentService(HistoricalWalkForwardEvaluationService evaluationService) {
		this.evaluationService = evaluationService;
	}

	public ProbabilityModelV2DevelopmentReport evaluateDevelopmentLeagues(
			WalkForwardEvaluationRequest template,
			ProbabilityModelV2Config v2Config,
			BigDecimal startingBankroll,
			List<NamedStrategyConfig> strategies,
			Integer maxAcceptedBets) {
		if (template == null) {
			throw new IllegalArgumentException("template request is required");
		}
		if (v2Config == null) {
			throw new IllegalArgumentException("v2Config is required");
		}
		List<ProbabilityModelV2LeagueRun> leagues = new ArrayList<>();
		List<ProbabilityModelComparison> comparisons = new ArrayList<>();
		for (CanonicalCompetition competition : ProbabilityModelDevelopmentLeagues.DEVELOPMENT) {
			WalkForwardEvaluationRequest request = new WalkForwardEvaluationRequest(
					competition,
					template.trainingFromSeason(),
					template.evaluationFromSeason(),
					template.evaluationToSeason(),
					template.quoteSource(),
					template.modelConfig());
			ProbabilityModelV2LeagueRun league = diagnose(request, v2Config, startingBankroll, strategies, maxAcceptedBets);
			leagues.add(league);
			comparisons.add(league.comparison());
		}
		ProbabilityModelV2ClassificationEngine.ClassificationDecision decision =
				ProbabilityModelV2ClassificationEngine.classify(comparisons);
		return new ProbabilityModelV2DevelopmentReport(
				v2Config, leagues, decision.classification(), decision.reasons());
	}

	public ProbabilityModelV2LeagueRun diagnose(
			WalkForwardEvaluationRequest request,
			ProbabilityModelV2Config v2Config,
			BigDecimal startingBankroll,
			List<NamedStrategyConfig> strategies,
			Integer maxAcceptedBets) {
		if (request == null) {
			throw new IllegalArgumentException("request is required");
		}
		if (v2Config == null) {
			throw new IllegalArgumentException("v2Config is required");
		}
		ProbabilityModelDevelopmentLeagues.requireDevelopment(request.competition());
		FootballProbabilityModel v1Model = new PoissonFootballProbabilityModel(request.modelConfig());
		HistoricalWalkForwardBuildOutput v1Output = evaluationService.buildWithPredictions(request, v1Model);
		HistoricalStrategyComparisonResult v1Strategies =
				comparisonEngine.compare(v1Output.dataset(), startingBankroll, strategies, maxAcceptedBets);
		BaselineDiagnosticsReport v1Baseline =
				baselineEngine.analyze(v1Output.dataset(), v1Output.predictions(), v1Strategies.strategyResults());
		EdgeQualityReport v1Edge = edgeQualityEngine.analyze(v1Output.dataset(), v1Strategies.strategyResults());

		DixonColesFitRecorder recorder = new DixonColesFitRecorder();
		FootballProbabilityModel v2Model = new RegularizedDixonColesFootballProbabilityModel(v2Config, recorder);
		HistoricalWalkForwardBuildOutput v2Output = evaluationService.buildWithPredictions(request, v2Model);
		HistoricalStrategyComparisonResult v2Strategies =
				comparisonEngine.compare(v2Output.dataset(), startingBankroll, strategies, maxAcceptedBets);
		BaselineDiagnosticsReport v2Baseline =
				baselineEngine.analyze(v2Output.dataset(), v2Output.predictions(), v2Strategies.strategyResults());
		EdgeQualityReport v2Edge = edgeQualityEngine.analyze(v2Output.dataset(), v2Strategies.strategyResults());

		ProbabilityModelComparison comparison = modelComparisonEngine.compare(
				request.competition(),
				v1Output,
				v1Edge,
				v1Baseline,
				v1Strategies,
				v2Output,
				v2Edge,
				v2Baseline,
				v2Strategies,
				recorder.snapshots());
		return new ProbabilityModelV2LeagueRun(
				request.competition(),
				v2Config,
				v1Output,
				v2Output,
				v1Strategies,
				v2Strategies,
				v1Baseline,
				v2Baseline,
				v1Edge,
				v2Edge,
				recorder.snapshots(),
				comparison);
	}
}
