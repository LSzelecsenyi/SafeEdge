package com.safeedge.historical.service;

import com.safeedge.historical.diagnostics.BaselineDiagnosticsEngine;
import com.safeedge.historical.diagnostics.BaselineDiagnosticsReport;
import com.safeedge.historical.diagnostics.EdgeQualityDiagnosticsEngine;
import com.safeedge.historical.diagnostics.EdgeQualityReport;
import com.safeedge.historical.diagnostics.ProbabilityModelDevelopmentLeagues;
import com.safeedge.historical.diagnostics.ProbabilityModelV3ClassificationEngine;
import com.safeedge.historical.diagnostics.ProbabilityModelV3Comparison;
import com.safeedge.historical.diagnostics.ProbabilityModelV3ComparisonEngine;
import com.safeedge.historical.diagnostics.ProbabilityModelV3DevelopmentReport;
import com.safeedge.historical.diagnostics.ProbabilityModelV3LeagueRun;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonEngine;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;
import com.safeedge.historical.evaluation.NamedStrategyConfig;
import com.safeedge.historical.evaluation.WalkForwardEvaluationRequest;
import com.safeedge.probability.DixonColesFitRecorder;
import com.safeedge.probability.FootballProbabilityModel;
import com.safeedge.probability.JointDixonColesFitRecorder;
import com.safeedge.probability.JointDixonColesFootballProbabilityModel;
import com.safeedge.probability.PoissonFootballProbabilityModel;
import com.safeedge.probability.ProbabilityModelV2Config;
import com.safeedge.probability.ProbabilityModelV3Config;
import com.safeedge.probability.RegularizedDixonColesFootballProbabilityModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Runs frozen Probability Model v1, Regularized Dixon-Coles v2, and jointly
 * fitted Dixon-Coles v3 on the same persisted walk-forward window. Does not
 * change CandidateEngine or BacktestEngine. Refuses reserved validation leagues.
 */
@Service
public class ProbabilityModelV3DevelopmentService {

	private final HistoricalWalkForwardEvaluationService evaluationService;
	private final HistoricalStrategyComparisonEngine comparisonEngine = new HistoricalStrategyComparisonEngine();
	private final BaselineDiagnosticsEngine baselineEngine = new BaselineDiagnosticsEngine();
	private final EdgeQualityDiagnosticsEngine edgeQualityEngine = new EdgeQualityDiagnosticsEngine();
	private final ProbabilityModelV3ComparisonEngine modelComparisonEngine = new ProbabilityModelV3ComparisonEngine();

	public ProbabilityModelV3DevelopmentService(HistoricalWalkForwardEvaluationService evaluationService) {
		this.evaluationService = evaluationService;
	}

	public ProbabilityModelV3DevelopmentReport evaluateDevelopmentLeagues(
			WalkForwardEvaluationRequest template,
			ProbabilityModelV3Config v3Config,
			BigDecimal startingBankroll,
			List<NamedStrategyConfig> strategies,
			Integer maxAcceptedBets) {
		if (template == null) {
			throw new IllegalArgumentException("template request is required");
		}
		if (v3Config == null) {
			throw new IllegalArgumentException("v3Config is required");
		}
		List<ProbabilityModelV3LeagueRun> leagues = new ArrayList<>();
		for (CanonicalCompetition competition : ProbabilityModelDevelopmentLeagues.DEVELOPMENT) {
			WalkForwardEvaluationRequest request = new WalkForwardEvaluationRequest(
					competition,
					template.trainingFromSeason(),
					template.evaluationFromSeason(),
					template.evaluationToSeason(),
					template.quoteSource(),
					template.modelConfig());
			leagues.add(diagnose(request, v3Config, startingBankroll, strategies, maxAcceptedBets));
		}
		List<ProbabilityModelV3Comparison> comparisons = new ArrayList<>();
		for (ProbabilityModelV3LeagueRun league : leagues) {
			comparisons.add(league.comparison());
		}
		ProbabilityModelV3ClassificationEngine.ClassificationDecision decision =
				ProbabilityModelV3ClassificationEngine.classify(comparisons);
		return new ProbabilityModelV3DevelopmentReport(
				v3Config, leagues, decision.classification(), decision.reasons());
	}

	public ProbabilityModelV3LeagueRun diagnose(
			WalkForwardEvaluationRequest request,
			ProbabilityModelV3Config v3Config,
			BigDecimal startingBankroll,
			List<NamedStrategyConfig> strategies,
			Integer maxAcceptedBets) {
		if (request == null) {
			throw new IllegalArgumentException("request is required");
		}
		if (v3Config == null) {
			throw new IllegalArgumentException("v3Config is required");
		}
		ProbabilityModelDevelopmentLeagues.requireDevelopment(request.competition());
		FootballProbabilityModel v1Model = new PoissonFootballProbabilityModel(request.modelConfig());
		HistoricalWalkForwardBuildOutput v1Output = evaluationService.buildWithPredictions(request, v1Model);
		HistoricalStrategyComparisonResult v1Strategies =
				comparisonEngine.compare(v1Output.dataset(), startingBankroll, strategies, maxAcceptedBets);
		BaselineDiagnosticsReport v1Baseline =
				baselineEngine.analyze(v1Output.dataset(), v1Output.predictions(), v1Strategies.strategyResults());
		EdgeQualityReport v1Edge = edgeQualityEngine.analyze(v1Output.dataset(), v1Strategies.strategyResults());

		DixonColesFitRecorder v2Recorder = new DixonColesFitRecorder();
		FootballProbabilityModel v2Model =
				new RegularizedDixonColesFootballProbabilityModel(ProbabilityModelV2Config.defaults(), v2Recorder);
		HistoricalWalkForwardBuildOutput v2Output = evaluationService.buildWithPredictions(request, v2Model);
		HistoricalStrategyComparisonResult v2Strategies =
				comparisonEngine.compare(v2Output.dataset(), startingBankroll, strategies, maxAcceptedBets);
		BaselineDiagnosticsReport v2Baseline =
				baselineEngine.analyze(v2Output.dataset(), v2Output.predictions(), v2Strategies.strategyResults());
		EdgeQualityReport v2Edge = edgeQualityEngine.analyze(v2Output.dataset(), v2Strategies.strategyResults());

		JointDixonColesFitRecorder v3Recorder = new JointDixonColesFitRecorder();
		FootballProbabilityModel v3Model = new JointDixonColesFootballProbabilityModel(v3Config, v3Recorder);
		HistoricalWalkForwardBuildOutput v3Output = evaluationService.buildWithPredictions(request, v3Model);
		HistoricalStrategyComparisonResult v3Strategies =
				comparisonEngine.compare(v3Output.dataset(), startingBankroll, strategies, maxAcceptedBets);
		BaselineDiagnosticsReport v3Baseline =
				baselineEngine.analyze(v3Output.dataset(), v3Output.predictions(), v3Strategies.strategyResults());
		EdgeQualityReport v3Edge = edgeQualityEngine.analyze(v3Output.dataset(), v3Strategies.strategyResults());

		return new ProbabilityModelV3LeagueRun(
				request.competition(),
				v3Config,
				v1Output,
				v2Output,
				v3Output,
				v1Strategies,
				v2Strategies,
				v3Strategies,
				v1Baseline,
				v2Baseline,
				v3Baseline,
				v1Edge,
				v2Edge,
				v3Edge,
				v3Recorder.snapshots(),
				v3Recorder.fittingFailures(),
				modelComparisonEngine.compare(
						request.competition(),
						v1Output,
						v1Edge,
						v1Baseline,
						v1Strategies,
						v2Output,
						v2Edge,
						v2Baseline,
						v2Strategies,
						v3Output,
						v3Edge,
						v3Baseline,
						v3Strategies,
						v3Recorder));
	}
}
