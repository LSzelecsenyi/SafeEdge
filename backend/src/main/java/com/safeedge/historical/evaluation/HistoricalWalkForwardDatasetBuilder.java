package com.safeedge.historical.evaluation;

import com.safeedge.backtest.HistoricalBettingOpportunity;
import com.safeedge.backtest.HistoricalEventResult;
import com.safeedge.candidate.CandidateContext;
import com.safeedge.candidate.CandidateEngine;
import com.safeedge.candidate.CandidateEvaluation;
import com.safeedge.candidate.CandidateValueStatus;
import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.features.HistoricalMatchRecord;
import com.safeedge.probability.FootballProbabilityModel;
import com.safeedge.probability.MatchPredictionContext;
import com.safeedge.probability.PoissonFootballProbabilityModel;
import com.safeedge.probability.ProbabilityPrediction;
import com.safeedge.probability.ProbabilityPredictionStatus;
import com.safeedge.probability.ProbabilityTrainingMatch;
import com.safeedge.probability.ScoreProbabilityEvaluator;
import com.safeedge.settlement.AsianHandicapLines;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Walk-forward historical dataset builder. For each date D, predictions use only
 * matches with {@code matchDate < D}. Same-date results never train each other.
 * Candidate EV is produced by {@link CandidateEngine}, not recalculated here.
 */
public final class HistoricalWalkForwardDatasetBuilder {

	private static final MathContext MATH = MathContext.DECIMAL128;
	private static final Comparator<HistoricalMatchRecord> MATCH_ORDER = Comparator
			.comparing(HistoricalMatchRecord::matchDate)
			.thenComparingInt(HistoricalMatchRecord::sourceRowNumber)
			.thenComparing(match -> match.persistenceId() == null ? Long.MIN_VALUE : match.persistenceId())
			.thenComparing(HistoricalMatchRecord::homeTeam)
			.thenComparing(HistoricalMatchRecord::awayTeam);

	private final FootballProbabilityModel injectedModel;
	private final CandidateEngine candidateEngine;

	public HistoricalWalkForwardDatasetBuilder() {
		this(null, new CandidateEngine());
	}

	public HistoricalWalkForwardDatasetBuilder(
			FootballProbabilityModel model, CandidateEngine candidateEngine) {
		if (candidateEngine == null) {
			throw new HistoricalDataException("candidateEngine is required");
		}
		this.injectedModel = model;
		this.candidateEngine = candidateEngine;
	}

	public HistoricalWalkForwardDataset build(
			List<HistoricalMatchRecord> matches,
			Map<String, HistoricalAhQuoteSnapshot> quotesByEventId,
			WalkForwardEvaluationRequest request) {
		if (matches == null) {
			throw new HistoricalDataException("matches are required");
		}
		if (request == null) {
			throw new HistoricalDataException("request is required");
		}
		Map<String, HistoricalAhQuoteSnapshot> quotes =
				quotesByEventId == null ? Map.of() : Map.copyOf(quotesByEventId);
		FootballProbabilityModel model = injectedModel != null
				? injectedModel
				: new PoissonFootballProbabilityModel(request.modelConfig());
		List<HistoricalMatchRecord> loaded = selectLoadedMatches(matches, request);
		loaded.sort(MATCH_ORDER);

		StatsAccumulator stats = new StatsAccumulator(request, loaded.size());
		List<HistoricalBettingOpportunity> opportunities = new ArrayList<>();
		List<HistoricalEventResult> eventResults = new ArrayList<>();
		List<ProbabilityTrainingMatch> training = new ArrayList<>();

		Map<LocalDate, List<HistoricalMatchRecord>> byDate = groupByDate(loaded);
		for (Map.Entry<LocalDate, List<HistoricalMatchRecord>> day : byDate.entrySet()) {
			for (HistoricalMatchRecord match : day.getValue()) {
				if (!isEvaluationMatch(match, request)) {
					continue;
				}
				evaluateMatch(match, training, quotes, request, model, stats, opportunities, eventResults);
			}
			for (HistoricalMatchRecord match : day.getValue()) {
				training.add(ProbabilityTrainingMatch.from(match));
			}
		}
		return new HistoricalWalkForwardDataset(stats.toStats(), opportunities, eventResults);
	}

	private void evaluateMatch(
			HistoricalMatchRecord match,
			List<ProbabilityTrainingMatch> training,
			Map<String, HistoricalAhQuoteSnapshot> quotes,
			WalkForwardEvaluationRequest request,
			FootballProbabilityModel model,
			StatsAccumulator stats,
			List<HistoricalBettingOpportunity> opportunities,
			List<HistoricalEventResult> eventResults) {
		stats.matchesEvaluated++;
		String eventId = HistoricalWalkForwardIdentities.eventId(match);
		MatchPredictionContext target = new MatchPredictionContext(
				match.competition(), match.homeTeam(), match.awayTeam(), match.matchDate());
		ProbabilityPrediction prediction = model.predict(training, target);
		if (prediction.status() == ProbabilityPredictionStatus.NO_LEAGUE_HISTORY) {
			stats.matchesSkippedNoLeagueHistory++;
			if (selectedQuote(quotes, eventId, request) == null) {
				stats.matchesSkippedMissingQuote++;
			}
			return;
		}
		if (prediction.status() == ProbabilityPredictionStatus.INSUFFICIENT_HISTORY) {
			stats.matchesSkippedInsufficientHistory++;
			if (selectedQuote(quotes, eventId, request) == null) {
				stats.matchesSkippedMissingQuote++;
			}
			return;
		}
		if (!prediction.available()) {
			throw new HistoricalDataException("Unexpected prediction status " + prediction.status());
		}
		stats.predictionsAvailable++;
		ScoreProbabilityEvaluator.logLoss(prediction.scoreDistribution(), match.score())
				.ifPresentOrElse(
						loss -> {
							stats.logLossSum = stats.logLossSum.add(loss, MATH);
							stats.logLossObservations++;
						},
						() -> stats.logLossMissingFromGrid++);
		HistoricalAhQuoteSnapshot quote = selectedQuote(quotes, eventId, request);
		if (quote == null) {
			stats.matchesSkippedMissingQuote++;
			return;
		}
		stats.predictionsWithSelectedAhQuote++;
		appendCandidates(match, eventId, quote, prediction, stats, opportunities, eventResults);
	}

	private void appendCandidates(
			HistoricalMatchRecord match,
			String eventId,
			HistoricalAhQuoteSnapshot quote,
			ProbabilityPrediction prediction,
			StatsAccumulator stats,
			List<HistoricalBettingOpportunity> opportunities,
			List<HistoricalEventResult> eventResults) {
		AsianHandicapLines.requireSupportedIncrement(quote.homeHandicapLine());
		BigDecimal homeLine = quote.homeHandicapLine();
		BigDecimal awayLine = AsianHandicapLines.awayLine(homeLine);
		String marketId = HistoricalWalkForwardIdentities.marketId(eventId, quote.quoteSource(), homeLine);
		String provider = HistoricalWalkForwardIdentities.PROVIDER;
		BettingSelection homeSelection = new BettingSelection(
				provider, 1, 1, SelectionType.HOME.name(), SelectionType.HOME, homeLine, quote.homeOdds());
		BettingSelection awaySelection = new BettingSelection(
				provider, 2, 2, SelectionType.AWAY.name(), SelectionType.AWAY, awayLine, quote.awayOdds());
		BettingMarket market = new BettingMarket(
				provider,
				marketId,
				null,
				"Asian Handicap",
				null,
				null,
				null,
				MarketType.ASIAN_HANDICAP,
				homeLine,
				List.of(homeSelection, awaySelection));
		Instant decisionAt = HistoricalSyntheticChronology.decisionAt(match.matchDate());
		CandidateEvaluation home = candidateEngine.evaluate(
				market,
				homeSelection,
				quote.homeOdds(),
				prediction.scoreDistribution(),
				new CandidateContext(
						HistoricalWalkForwardIdentities.opportunityId(marketId, SelectionType.HOME),
						eventId,
						match.competition().name(),
						match.matchDate()));
		CandidateEvaluation away = candidateEngine.evaluate(
				market,
				awaySelection,
				quote.awayOdds(),
				prediction.scoreDistribution(),
				new CandidateContext(
						HistoricalWalkForwardIdentities.opportunityId(marketId, SelectionType.AWAY),
						eventId,
						match.competition().name(),
						match.matchDate()));
		opportunities.add(new HistoricalBettingOpportunity(home.opportunity(), market, homeSelection, decisionAt));
		opportunities.add(new HistoricalBettingOpportunity(away.opportunity(), market, awaySelection, decisionAt));
		eventResults.add(new HistoricalEventResult(
				eventId, HistoricalSyntheticChronology.settlementAt(match.matchDate()), match.score()));
		recordCandidate(stats, home, true);
		recordCandidate(stats, away, false);
	}

	private static void recordCandidate(StatsAccumulator stats, CandidateEvaluation evaluation, boolean home) {
		stats.candidatesGenerated++;
		if (home) {
			stats.homeCandidatesGenerated++;
		}
		else {
			stats.awayCandidatesGenerated++;
		}
		stats.edgeSum = stats.edgeSum.add(evaluation.expectedReturnRate(), MATH);
		if (evaluation.status() == CandidateValueStatus.POSITIVE_EV) {
			stats.positiveEvCandidates++;
		}
		else if (evaluation.status() == CandidateValueStatus.ZERO_EV) {
			stats.zeroEvCandidates++;
		}
		else {
			stats.negativeEvCandidates++;
		}
	}

	private static HistoricalAhQuoteSnapshot selectedQuote(
			Map<String, HistoricalAhQuoteSnapshot> quotes,
			String eventId,
			WalkForwardEvaluationRequest request) {
		HistoricalAhQuoteSnapshot quote = quotes.get(eventId);
		if (quote == null || quote.quoteSource() != request.quoteSource()) {
			return null;
		}
		return quote;
	}

	private static boolean isEvaluationMatch(HistoricalMatchRecord match, WalkForwardEvaluationRequest request) {
		int season = match.season().startYear();
		return season >= request.evaluationFromSeason() && season <= request.evaluationToSeason();
	}

	private static List<HistoricalMatchRecord> selectLoadedMatches(
			List<HistoricalMatchRecord> matches, WalkForwardEvaluationRequest request) {
		List<HistoricalMatchRecord> loaded = new ArrayList<>();
		for (HistoricalMatchRecord match : matches) {
			if (match == null) {
				throw new HistoricalDataException("matches must not contain null");
			}
			if (match.competition() != request.competition()) {
				continue;
			}
			int season = match.season().startYear();
			if (season < request.trainingFromSeason() || season > request.evaluationToSeason()) {
				continue;
			}
			loaded.add(match);
		}
		return loaded;
	}

	private static Map<LocalDate, List<HistoricalMatchRecord>> groupByDate(List<HistoricalMatchRecord> matches) {
		Map<LocalDate, List<HistoricalMatchRecord>> byDate = new LinkedHashMap<>();
		for (HistoricalMatchRecord match : matches) {
			byDate.computeIfAbsent(match.matchDate(), date -> new ArrayList<>()).add(match);
		}
		return byDate;
	}

	private static final class StatsAccumulator {
		private final WalkForwardEvaluationRequest request;
		private final int matchesLoaded;
		private int matchesEvaluated;
		private int matchesSkippedNoLeagueHistory;
		private int matchesSkippedInsufficientHistory;
		private int matchesSkippedMissingQuote;
		private int predictionsAvailable;
		private int predictionsWithSelectedAhQuote;
		private int candidatesGenerated;
		private int homeCandidatesGenerated;
		private int awayCandidatesGenerated;
		private int positiveEvCandidates;
		private int zeroEvCandidates;
		private int negativeEvCandidates;
		private int logLossObservations;
		private int logLossMissingFromGrid;
		private BigDecimal logLossSum = BigDecimal.ZERO;
		private BigDecimal edgeSum = BigDecimal.ZERO;

		private StatsAccumulator(WalkForwardEvaluationRequest request, int matchesLoaded) {
			this.request = request;
			this.matchesLoaded = matchesLoaded;
		}

		private WalkForwardBuildStats toStats() {
			return new WalkForwardBuildStats(
					request.competition(),
					request.trainingFromSeason(),
					request.evaluationFromSeason(),
					request.evaluationToSeason(),
					request.quoteSource(),
					matchesLoaded,
					matchesEvaluated,
					matchesSkippedNoLeagueHistory,
					matchesSkippedInsufficientHistory,
					matchesSkippedMissingQuote,
					predictionsAvailable,
					predictionsWithSelectedAhQuote,
					candidatesGenerated,
					homeCandidatesGenerated,
					awayCandidatesGenerated,
					positiveEvCandidates,
					zeroEvCandidates,
					negativeEvCandidates,
					logLossObservations,
					logLossMissingFromGrid,
					logLossObservations == 0 ? null : logLossSum.divide(BigDecimal.valueOf(logLossObservations), MATH),
					candidatesGenerated == 0 ? null : edgeSum.divide(BigDecimal.valueOf(candidatesGenerated), MATH));
		}
	}
}
