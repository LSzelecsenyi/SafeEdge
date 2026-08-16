package com.safeedge.historical.diagnostics;

import com.safeedge.backtest.BacktestBetResult;
import com.safeedge.backtest.BacktestEquityPoint;
import com.safeedge.backtest.BacktestResult;
import com.safeedge.backtest.HistoricalBettingOpportunity;
import com.safeedge.backtest.HistoricalEventResult;
import com.safeedge.candidate.ScoreProbability;
import com.safeedge.candidate.ScoreProbabilityDistribution;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.evaluation.HistoricalPredictionSnapshot;
import com.safeedge.historical.evaluation.HistoricalWalkForwardDataset;
import com.safeedge.historical.evaluation.NamedBacktestResult;
import com.safeedge.settlement.MatchScore;
import com.safeedge.settlement.PayoutCalculator;
import com.safeedge.settlement.PayoutResult;
import com.safeedge.settlement.SettlementEngine;
import com.safeedge.settlement.SettlementResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Pure Baseline 001 autopsy. Consumes one already-prepared walk-forward dataset,
 * captured predictions from that same pass, and already-run strategy backtests.
 * Does not refit Poisson, mutate inputs, or change StrategyConfig.
 */
public final class BaselineDiagnosticsEngine {

	private static final BigDecimal TWO_PERCENT = new BigDecimal("0.02");
	private static final BigDecimal THREE_PERCENT = new BigDecimal("0.03");
	private static final BigDecimal FIVE_PERCENT = new BigDecimal("0.05");
	private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
	private static final BigDecimal ODDS_115 = new BigDecimal("1.15");
	private static final BigDecimal ODDS_135 = new BigDecimal("1.35");
	private static final int LAST_SETTLED_BEFORE_PAUSE = 10;

	private final SettlementEngine settlementEngine;
	private final PayoutCalculator payoutCalculator;

	public BaselineDiagnosticsEngine() {
		this(new SettlementEngine(), new PayoutCalculator());
	}

	public BaselineDiagnosticsEngine(SettlementEngine settlementEngine, PayoutCalculator payoutCalculator) {
		if (settlementEngine == null) {
			throw new IllegalArgumentException("settlementEngine is required");
		}
		if (payoutCalculator == null) {
			throw new IllegalArgumentException("payoutCalculator is required");
		}
		this.settlementEngine = settlementEngine;
		this.payoutCalculator = payoutCalculator;
	}

	public BaselineDiagnosticsReport analyze(
			HistoricalWalkForwardDataset dataset,
			List<HistoricalPredictionSnapshot> predictions,
			List<NamedBacktestResult> strategyResults) {
		if (dataset == null) {
			throw new IllegalArgumentException("dataset is required");
		}
		List<HistoricalPredictionSnapshot> predictionList =
				predictions == null ? List.of() : List.copyOf(predictions);
		List<NamedBacktestResult> strategies =
				strategyResults == null ? List.of() : List.copyOf(strategyResults);
		List<AnalyzedCandidate> candidates = analyzeCandidates(dataset, predictionList);
		UnitStakeSummary all = summarize(candidates);
		return new BaselineDiagnosticsReport(
				new CandidateOverview(dataset.stats(), candidates.size(), all),
				edgeBuckets(candidates),
				positiveEdgeThresholds(candidates),
				oddsBuckets(candidates),
				ahLines(candidates),
				lineFamilies(candidates),
				sides(candidates),
				seasons(candidates, predictionList),
				new EdgeSignDiagnostics(
						summarize(filter(candidates, row -> row.predictedEdge().compareTo(BigDecimal.ZERO) < 0)),
						summarize(filter(candidates, row -> row.predictedEdge().compareTo(BigDecimal.ZERO) > 0))),
				goalCalibration(predictionList),
				marginCalibration(predictionList),
				DiagnosticMath.quantiles(edges(candidates)),
				DiagnosticMath.quantiles(edges(filter(
						candidates, row -> row.predictedEdge().compareTo(BigDecimal.ZERO) > 0))),
				positiveEdgeConcentration(candidates),
				originalOddsRangeSubsets(candidates),
				strategyAcceptedBets(dataset, candidates, strategies));
	}

	private List<AnalyzedCandidate> analyzeCandidates(
			HistoricalWalkForwardDataset dataset, List<HistoricalPredictionSnapshot> predictions) {
		Map<String, HistoricalEventResult> resultsByEvent = new LinkedHashMap<>();
		for (HistoricalEventResult result : dataset.eventResults()) {
			resultsByEvent.put(result.eventId(), result);
		}
		Map<String, HistoricalPredictionSnapshot> predictionsByEvent = new LinkedHashMap<>();
		for (HistoricalPredictionSnapshot snapshot : predictions) {
			predictionsByEvent.put(snapshot.eventId(), snapshot);
		}
		List<AnalyzedCandidate> analyzed = new ArrayList<>(dataset.opportunities().size());
		for (HistoricalBettingOpportunity historical : dataset.opportunities()) {
			String eventId = historical.opportunity().eventId();
			HistoricalEventResult result = resultsByEvent.get(eventId);
			if (result == null) {
				throw new IllegalArgumentException("missing historical result for event " + eventId);
			}
			SettlementResult settlement = settlementEngine.settle(
					historical.market(), historical.selection(), result.finalScore());
			PayoutResult payout = payoutCalculator.calculate(
					settlement, historical.opportunity().odds(), DiagnosticMath.UNIT_STAKE);
			HistoricalPredictionSnapshot prediction = predictionsByEvent.get(eventId);
			String season = prediction != null
					? prediction.season().displayValue()
					: seasonOf(historical.opportunity().bettingDate()).displayValue();
			analyzed.add(new AnalyzedCandidate(
					historical.opportunity().opportunityId(),
					eventId,
					season,
					historical.opportunity().bettingDate(),
					historical.decisionAt(),
					historical.selection().selectionType(),
					historical.selection().line(),
					historical.opportunity().odds(),
					historical.opportunity().edge(),
					settlement,
					payout.profit()));
		}
		return List.copyOf(analyzed);
	}

	private static List<EdgeBucketDiagnostics> edgeBuckets(List<AnalyzedCandidate> candidates) {
		List<EdgeBucketDiagnostics> rows = new ArrayList<>();
		for (DiagnosticEdgeBucket bucket : DiagnosticEdgeBucket.values()) {
			rows.add(new EdgeBucketDiagnostics(
					bucket, summarize(filter(candidates, row -> DiagnosticEdgeBucket.of(row.predictedEdge()) == bucket))));
		}
		return List.copyOf(rows);
	}

	private static List<OddsBucketDiagnostics> oddsBuckets(List<AnalyzedCandidate> candidates) {
		List<OddsBucketDiagnostics> rows = new ArrayList<>();
		for (DiagnosticOddsBucket bucket : DiagnosticOddsBucket.values()) {
			rows.add(new OddsBucketDiagnostics(
					bucket, summarize(filter(candidates, row -> DiagnosticOddsBucket.of(row.odds()) == bucket))));
		}
		return List.copyOf(rows);
	}

	private static List<AhLineDiagnostics> ahLines(List<AnalyzedCandidate> candidates) {
		Map<BigDecimal, List<AnalyzedCandidate>> grouped = new TreeMap<>(Comparator.naturalOrder());
		for (AnalyzedCandidate candidate : candidates) {
			grouped.computeIfAbsent(candidate.selectedLine(), key -> new ArrayList<>()).add(candidate);
		}
		List<AhLineDiagnostics> rows = new ArrayList<>();
		for (Map.Entry<BigDecimal, List<AnalyzedCandidate>> entry : grouped.entrySet()) {
			rows.add(new AhLineDiagnostics(entry.getKey(), summarize(entry.getValue())));
		}
		return List.copyOf(rows);
	}

	private static List<LineFamilyDiagnostics> lineFamilies(List<AnalyzedCandidate> candidates) {
		List<LineFamilyDiagnostics> rows = new ArrayList<>();
		for (DiagnosticLineFamily family : DiagnosticLineFamily.values()) {
			rows.add(new LineFamilyDiagnostics(
					family,
					summarize(filter(
							candidates, row -> DiagnosticLineFamily.of(row.selectedLine()) == family))));
		}
		return List.copyOf(rows);
	}

	private static List<SideDiagnostics> sides(List<AnalyzedCandidate> candidates) {
		return List.of(
				new SideDiagnostics(
						SelectionType.HOME,
						summarize(filter(candidates, row -> row.side() == SelectionType.HOME))),
				new SideDiagnostics(
						SelectionType.AWAY,
						summarize(filter(candidates, row -> row.side() == SelectionType.AWAY))));
	}

	private static List<SeasonDiagnostics> seasons(
			List<AnalyzedCandidate> candidates, List<HistoricalPredictionSnapshot> predictions) {
		Map<String, Integer> predictionCounts = new LinkedHashMap<>();
		Map<String, Integer> seasonStartYears = new LinkedHashMap<>();
		for (HistoricalPredictionSnapshot snapshot : predictions) {
			String display = snapshot.season().displayValue();
			predictionCounts.merge(display, 1, Integer::sum);
			seasonStartYears.putIfAbsent(display, snapshot.season().startYear());
		}
		for (AnalyzedCandidate candidate : candidates) {
			seasonStartYears.putIfAbsent(candidate.seasonDisplay(), seasonOf(candidate.matchDate()).startYear());
			predictionCounts.putIfAbsent(candidate.seasonDisplay(), 0);
		}
		List<String> ordered = new ArrayList<>(seasonStartYears.keySet());
		ordered.sort(Comparator.comparingInt((String display) -> seasonStartYears.get(display))
				.thenComparing(Comparator.naturalOrder()));
		List<SeasonDiagnostics> rows = new ArrayList<>();
		for (String season : ordered) {
			rows.add(new SeasonDiagnostics(
					season,
					predictionCounts.getOrDefault(season, 0),
					summarize(filter(candidates, row -> row.seasonDisplay().equals(season)))));
		}
		return List.copyOf(rows);
	}

	private static List<CandidateSubsetDiagnostics> positiveEdgeThresholds(List<AnalyzedCandidate> candidates) {
		return List.of(
				subset("edge > 0", filter(candidates, row -> row.predictedEdge().compareTo(BigDecimal.ZERO) > 0)),
				subset("edge >= 0.02", filter(candidates, row -> row.predictedEdge().compareTo(TWO_PERCENT) >= 0)),
				subset("edge >= 0.03", filter(candidates, row -> row.predictedEdge().compareTo(THREE_PERCENT) >= 0)),
				subset("edge >= 0.05", filter(candidates, row -> row.predictedEdge().compareTo(FIVE_PERCENT) >= 0)),
				subset("edge >= 0.10", filter(candidates, row -> row.predictedEdge().compareTo(TEN_PERCENT) >= 0)));
	}

	private static List<CandidateSubsetDiagnostics> originalOddsRangeSubsets(List<AnalyzedCandidate> candidates) {
		List<AnalyzedCandidate> inRange = filter(candidates, BaselineDiagnosticsEngine::inOriginalOddsRange);
		return List.of(
				subset("odds 1.15-1.35", inRange),
				subset(
						"odds 1.15-1.35 AND edge > 0",
						filter(inRange, row -> row.predictedEdge().compareTo(BigDecimal.ZERO) > 0)),
				subset(
						"odds 1.15-1.35 AND edge >= 0.03",
						filter(inRange, row -> row.predictedEdge().compareTo(THREE_PERCENT) >= 0)));
	}

	private static CandidateSubsetDiagnostics subset(String label, List<AnalyzedCandidate> rows) {
		return new CandidateSubsetDiagnostics(label, summarize(rows));
	}

	private static boolean inOriginalOddsRange(AnalyzedCandidate candidate) {
		return candidate.odds().compareTo(ODDS_115) >= 0 && candidate.odds().compareTo(ODDS_135) <= 0;
	}

	private static PositiveEdgeConcentration positiveEdgeConcentration(List<AnalyzedCandidate> candidates) {
		List<AnalyzedCandidate> positive =
				filter(candidates, row -> row.predictedEdge().compareTo(BigDecimal.ZERO) > 0);
		int total = positive.size();
		Map<String, Integer> bySide = new LinkedHashMap<>();
		bySide.put(SelectionType.HOME.name(), 0);
		bySide.put(SelectionType.AWAY.name(), 0);
		Map<BigDecimal, Integer> byLine = new TreeMap<>(Comparator.naturalOrder());
		Map<DiagnosticOddsBucket, Integer> byOdds = new EnumMap<>(DiagnosticOddsBucket.class);
		for (DiagnosticOddsBucket bucket : DiagnosticOddsBucket.values()) {
			byOdds.put(bucket, 0);
		}
		Map<String, Integer> bySeason = new TreeMap<>();
		for (AnalyzedCandidate candidate : positive) {
			bySide.merge(candidate.side().name(), 1, Integer::sum);
			byLine.merge(candidate.selectedLine(), 1, Integer::sum);
			byOdds.merge(DiagnosticOddsBucket.of(candidate.odds()), 1, Integer::sum);
			bySeason.merge(candidate.seasonDisplay(), 1, Integer::sum);
		}
		return new PositiveEdgeConcentration(
				shares(bySide, total),
				shares(byLine.entrySet().stream()
						.collect(Collectors.toMap(
								entry -> entry.getKey().toPlainString(),
								Map.Entry::getValue,
								Integer::sum,
								LinkedHashMap::new)),
						total),
				shares(byOdds.entrySet().stream()
						.collect(Collectors.toMap(
								entry -> entry.getKey().label(),
								Map.Entry::getValue,
								Integer::sum,
								LinkedHashMap::new)),
						total),
				shares(bySeason, total));
	}

	private static List<ConcentrationShare> shares(Map<String, Integer> counts, int total) {
		List<ConcentrationShare> rows = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			rows.add(new ConcentrationShare(
					entry.getKey(), entry.getValue(), DiagnosticMath.divide(BigDecimal.valueOf(entry.getValue()), total)));
		}
		return List.copyOf(rows);
	}

	private List<StrategyAcceptedBetDiagnostics> strategyAcceptedBets(
			HistoricalWalkForwardDataset dataset,
			List<AnalyzedCandidate> candidates,
			List<NamedBacktestResult> strategyResults) {
		Map<String, AnalyzedCandidate> byOpportunityId = new LinkedHashMap<>();
		for (AnalyzedCandidate candidate : candidates) {
			byOpportunityId.put(candidate.opportunityId(), candidate);
		}
		List<StrategyAcceptedBetDiagnostics> rows = new ArrayList<>();
		for (NamedBacktestResult named : strategyResults) {
			Set<String> acceptedIds = named.result().acceptedBetResults().stream()
					.map(BacktestBetResult::opportunityId)
					.collect(Collectors.toCollection(java.util.LinkedHashSet::new));
			List<AnalyzedCandidate> accepted = filter(candidates, row -> acceptedIds.contains(row.opportunityId()));
			UnitStakeSummary summary = summarize(accepted);
			rows.add(new StrategyAcceptedBetDiagnostics(
					named.name(),
					accepted.size(),
					summary.averagePredictedEdge(),
					summary.unitStakeRoi(),
					sides(accepted),
					ahLines(accepted),
					oddsBuckets(accepted),
					edgeBuckets(accepted),
					pauseDiagnostics(dataset, named.result(), byOpportunityId)));
		}
		return List.copyOf(rows);
	}

	private static DrawdownPauseDiagnostics pauseDiagnostics(
			HistoricalWalkForwardDataset dataset,
			BacktestResult result,
			Map<String, AnalyzedCandidate> byOpportunityId) {
		int accepted = result.counts().betsAccepted();
		if (!result.pausedByDrawdown()) {
			return new DrawdownPauseDiagnostics(false, null, null, null, accepted, null, null, null, List.of());
		}
		int skipped = result.counts().opportunitiesSkippedByDrawdownPause();
		List<HistoricalBettingOpportunity> opportunities = dataset.opportunities();
		Integer pauseIndex = null;
		LocalDate pauseDate = null;
		Instant pauseAt = null;
		if (skipped > 0 && skipped <= opportunities.size()) {
			pauseIndex = opportunities.size() - skipped;
			HistoricalBettingOpportunity pauseOpportunity = opportunities.get(pauseIndex);
			pauseDate = pauseOpportunity.opportunity().bettingDate();
			pauseAt = pauseOpportunity.decisionAt();
		}
		BacktestEquityPoint equityAtPause = equityAtOrBefore(result.equityCurve(), pauseAt);
		List<SettledBetSnapshot> lastSettled = lastSettledBefore(result.acceptedBetResults(), pauseAt, byOpportunityId);
		return new DrawdownPauseDiagnostics(
				true,
				pauseIndex,
				pauseDate,
				pauseAt,
				accepted,
				equityAtPause == null ? null : equityAtPause.activeBankroll(),
				equityAtPause == null ? null : equityAtPause.activeDrawdownRate(),
				equityAtPause == null ? null : equityAtPause.totalEquity(),
				lastSettled);
	}

	private static BacktestEquityPoint equityAtOrBefore(List<BacktestEquityPoint> curve, Instant at) {
		if (curve == null || curve.isEmpty() || at == null) {
			return curve == null || curve.isEmpty() ? null : curve.getLast();
		}
		BacktestEquityPoint match = null;
		for (BacktestEquityPoint point : curve) {
			if (!point.timestamp().isAfter(at)) {
				match = point;
			}
		}
		return match;
	}

	private static List<SettledBetSnapshot> lastSettledBefore(
			List<BacktestBetResult> accepted, Instant pauseAt, Map<String, AnalyzedCandidate> byOpportunityId) {
		if (accepted == null || accepted.isEmpty() || pauseAt == null) {
			return List.of();
		}
		List<BacktestBetResult> before = new ArrayList<>();
		for (BacktestBetResult bet : accepted) {
			if (!bet.settlementAt().isAfter(pauseAt)) {
				before.add(bet);
			}
		}
		int from = Math.max(0, before.size() - LAST_SETTLED_BEFORE_PAUSE);
		List<SettledBetSnapshot> snapshots = new ArrayList<>();
		for (int i = from; i < before.size(); i++) {
			BacktestBetResult bet = before.get(i);
			AnalyzedCandidate candidate = byOpportunityId.get(bet.opportunityId());
			if (candidate == null) {
				continue;
			}
			snapshots.add(new SettledBetSnapshot(
					bet.bettingDate(),
					candidate.side(),
					candidate.selectedLine(),
					bet.odds(),
					bet.edge(),
					bet.settlementResult(),
					bet.profit()));
		}
		return List.copyOf(snapshots);
	}

	private static GoalCalibrationDiagnostics goalCalibration(List<HistoricalPredictionSnapshot> predictions) {
		int n = predictions.size();
		if (n == 0) {
			return new GoalCalibrationDiagnostics(
					0, null, null, null, null, null, null, null, null, null, null, null, null);
		}
		List<BigDecimal> predictedHome = new ArrayList<>(n);
		List<BigDecimal> actualHome = new ArrayList<>(n);
		List<BigDecimal> predictedAway = new ArrayList<>(n);
		List<BigDecimal> actualAway = new ArrayList<>(n);
		List<BigDecimal> predictedTotal = new ArrayList<>(n);
		List<BigDecimal> actualTotal = new ArrayList<>(n);
		List<BigDecimal> pHome = new ArrayList<>(n);
		List<BigDecimal> pDraw = new ArrayList<>(n);
		List<BigDecimal> pAway = new ArrayList<>(n);
		int actualHomeWins = 0;
		int actualDraws = 0;
		int actualAwayWins = 0;
		for (HistoricalPredictionSnapshot snapshot : predictions) {
			ScorelineMoments moments = moments(snapshot.scoreDistribution());
			predictedHome.add(moments.expectedHomeGoals());
			predictedAway.add(moments.expectedAwayGoals());
			predictedTotal.add(moments.expectedHomeGoals().add(moments.expectedAwayGoals(), DiagnosticMath.MATH));
			actualHome.add(BigDecimal.valueOf(snapshot.actualScore().homeGoals()));
			actualAway.add(BigDecimal.valueOf(snapshot.actualScore().awayGoals()));
			actualTotal.add(BigDecimal.valueOf(
					snapshot.actualScore().homeGoals() + snapshot.actualScore().awayGoals()));
			pHome.add(moments.homeWinProbability());
			pDraw.add(moments.drawProbability());
			pAway.add(moments.awayWinProbability());
			int cmp = Integer.compare(snapshot.actualScore().homeGoals(), snapshot.actualScore().awayGoals());
			if (cmp > 0) {
				actualHomeWins++;
			}
			else if (cmp == 0) {
				actualDraws++;
			}
			else {
				actualAwayWins++;
			}
		}
		return new GoalCalibrationDiagnostics(
				n,
				DiagnosticMath.average(predictedHome),
				DiagnosticMath.average(actualHome),
				DiagnosticMath.average(predictedAway),
				DiagnosticMath.average(actualAway),
				DiagnosticMath.average(predictedTotal),
				DiagnosticMath.average(actualTotal),
				DiagnosticMath.average(pHome),
				DiagnosticMath.divide(BigDecimal.valueOf(actualHomeWins), n),
				DiagnosticMath.average(pDraw),
				DiagnosticMath.divide(BigDecimal.valueOf(actualDraws), n),
				DiagnosticMath.average(pAway),
				DiagnosticMath.divide(BigDecimal.valueOf(actualAwayWins), n));
	}

	private static MarginCalibrationDiagnostics marginCalibration(List<HistoricalPredictionSnapshot> predictions) {
		int n = predictions.size();
		EnumMap<DiagnosticMarginCategory, BigDecimal> predictedCategory = new EnumMap<>(DiagnosticMarginCategory.class);
		EnumMap<DiagnosticMarginCategory, Integer> actualCategory = new EnumMap<>(DiagnosticMarginCategory.class);
		for (DiagnosticMarginCategory category : DiagnosticMarginCategory.values()) {
			predictedCategory.put(category, BigDecimal.ZERO);
			actualCategory.put(category, 0);
		}
		EnumMap<DiagnosticExactMarginBucket, BigDecimal> predictedExact = new EnumMap<>(DiagnosticExactMarginBucket.class);
		EnumMap<DiagnosticExactMarginBucket, Integer> actualExact = new EnumMap<>(DiagnosticExactMarginBucket.class);
		for (DiagnosticExactMarginBucket bucket : DiagnosticExactMarginBucket.values()) {
			predictedExact.put(bucket, BigDecimal.ZERO);
			actualExact.put(bucket, 0);
		}
		for (HistoricalPredictionSnapshot snapshot : predictions) {
			ScorelineMoments moments = moments(snapshot.scoreDistribution());
			for (DiagnosticMarginCategory category : DiagnosticMarginCategory.values()) {
				predictedCategory.merge(category, moments.categoryProbability(category), BigDecimal::add);
			}
			for (DiagnosticExactMarginBucket bucket : DiagnosticExactMarginBucket.values()) {
				predictedExact.merge(bucket, moments.exactProbability(bucket), BigDecimal::add);
			}
			int margin = snapshot.actualScore().homeGoals() - snapshot.actualScore().awayGoals();
			actualCategory.merge(DiagnosticMarginCategory.ofHomeMargin(margin), 1, Integer::sum);
			actualExact.merge(DiagnosticExactMarginBucket.ofHomeMargin(margin), 1, Integer::sum);
		}
		List<MarginCategoryCalibration> categories = new ArrayList<>();
		for (DiagnosticMarginCategory category : DiagnosticMarginCategory.values()) {
			int actual = actualCategory.get(category);
			categories.add(new MarginCategoryCalibration(
					category,
					DiagnosticMath.divide(predictedCategory.get(category), n),
					DiagnosticMath.divide(BigDecimal.valueOf(actual), n),
					actual));
		}
		List<ExactMarginCalibration> exact = new ArrayList<>();
		for (DiagnosticExactMarginBucket bucket : DiagnosticExactMarginBucket.values()) {
			int actual = actualExact.get(bucket);
			exact.add(new ExactMarginCalibration(
					bucket,
					DiagnosticMath.divide(predictedExact.get(bucket), n),
					DiagnosticMath.divide(BigDecimal.valueOf(actual), n),
					actual));
		}
		return new MarginCalibrationDiagnostics(n, categories, exact);
	}

	private static ScorelineMoments moments(ScoreProbabilityDistribution distribution) {
		BigDecimal expectedHome = BigDecimal.ZERO;
		BigDecimal expectedAway = BigDecimal.ZERO;
		BigDecimal homeWin = BigDecimal.ZERO;
		BigDecimal draw = BigDecimal.ZERO;
		BigDecimal awayWin = BigDecimal.ZERO;
		EnumMap<DiagnosticMarginCategory, BigDecimal> categories = new EnumMap<>(DiagnosticMarginCategory.class);
		EnumMap<DiagnosticExactMarginBucket, BigDecimal> exact = new EnumMap<>(DiagnosticExactMarginBucket.class);
		for (DiagnosticMarginCategory category : DiagnosticMarginCategory.values()) {
			categories.put(category, BigDecimal.ZERO);
		}
		for (DiagnosticExactMarginBucket bucket : DiagnosticExactMarginBucket.values()) {
			exact.put(bucket, BigDecimal.ZERO);
		}
		for (ScoreProbability entry : distribution.entries()) {
			MatchScore score = entry.score();
			BigDecimal p = entry.probability();
			expectedHome = expectedHome.add(p.multiply(BigDecimal.valueOf(score.homeGoals()), DiagnosticMath.MATH), DiagnosticMath.MATH);
			expectedAway = expectedAway.add(p.multiply(BigDecimal.valueOf(score.awayGoals()), DiagnosticMath.MATH), DiagnosticMath.MATH);
			int margin = score.homeGoals() - score.awayGoals();
			if (margin > 0) {
				homeWin = homeWin.add(p, DiagnosticMath.MATH);
			}
			else if (margin == 0) {
				draw = draw.add(p, DiagnosticMath.MATH);
			}
			else {
				awayWin = awayWin.add(p, DiagnosticMath.MATH);
			}
			categories.merge(DiagnosticMarginCategory.ofHomeMargin(margin), p, BigDecimal::add);
			exact.merge(DiagnosticExactMarginBucket.ofHomeMargin(margin), p, BigDecimal::add);
		}
		return new ScorelineMoments(expectedHome, expectedAway, homeWin, draw, awayWin, categories, exact);
	}

	private static UnitStakeSummary summarize(List<AnalyzedCandidate> rows) {
		int count = rows.size();
		int positive = 0;
		BigDecimal edgeSum = BigDecimal.ZERO;
		BigDecimal oddsSum = BigDecimal.ZERO;
		BigDecimal realizedSum = BigDecimal.ZERO;
		SettlementCounts settlements = SettlementCounts.empty();
		for (AnalyzedCandidate row : rows) {
			if (row.predictedEdge().compareTo(BigDecimal.ZERO) > 0) {
				positive++;
			}
			edgeSum = edgeSum.add(row.predictedEdge(), DiagnosticMath.MATH);
			oddsSum = oddsSum.add(row.odds(), DiagnosticMath.MATH);
			realizedSum = realizedSum.add(row.realizedReturnRate(), DiagnosticMath.MATH);
			settlements = settlements.plus(row.settlement());
		}
		BigDecimal averageEdge = DiagnosticMath.divide(edgeSum, count);
		BigDecimal averageRealized = DiagnosticMath.divide(realizedSum, count);
		BigDecimal gap = averageEdge == null || averageRealized == null
				? null
				: averageRealized.subtract(averageEdge, DiagnosticMath.MATH);
		return new UnitStakeSummary(
				count,
				positive,
				averageEdge,
				DiagnosticMath.divide(oddsSum, count),
				averageRealized,
				gap,
				realizedSum,
				DiagnosticMath.divide(realizedSum, count),
				settlements);
	}

	private static List<AnalyzedCandidate> filter(
			List<AnalyzedCandidate> rows, java.util.function.Predicate<AnalyzedCandidate> predicate) {
		List<AnalyzedCandidate> filtered = new ArrayList<>();
		for (AnalyzedCandidate row : rows) {
			if (predicate.test(row)) {
				filtered.add(row);
			}
		}
		return filtered;
	}

	private static List<BigDecimal> edges(List<AnalyzedCandidate> rows) {
		List<BigDecimal> values = new ArrayList<>(rows.size());
		for (AnalyzedCandidate row : rows) {
			values.add(row.predictedEdge());
		}
		return values;
	}

	private static FootballSeason seasonOf(LocalDate date) {
		int startYear = date.getMonthValue() >= 7 ? date.getYear() : date.getYear() - 1;
		return new FootballSeason(startYear, startYear + 1);
	}

	private record ScorelineMoments(
			BigDecimal expectedHomeGoals,
			BigDecimal expectedAwayGoals,
			BigDecimal homeWinProbability,
			BigDecimal drawProbability,
			BigDecimal awayWinProbability,
			Map<DiagnosticMarginCategory, BigDecimal> categories,
			Map<DiagnosticExactMarginBucket, BigDecimal> exact) {

		BigDecimal categoryProbability(DiagnosticMarginCategory category) {
			return categories.getOrDefault(category, BigDecimal.ZERO);
		}

		BigDecimal exactProbability(DiagnosticExactMarginBucket bucket) {
			return exact.getOrDefault(bucket, BigDecimal.ZERO);
		}
	}
}
