package com.safeedge.historical.diagnostics;

import com.safeedge.backtest.HistoricalBettingOpportunity;
import com.safeedge.backtest.HistoricalEventResult;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.evaluation.HistoricalWalkForwardDataset;
import com.safeedge.historical.evaluation.NamedBacktestResult;
import com.safeedge.settlement.PayoutCalculator;
import com.safeedge.settlement.PayoutResult;
import com.safeedge.settlement.SettlementEngine;
import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.GeneralizedKellyCalculator;
import com.safeedge.strategy.SettlementProbabilityDistribution;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Baseline 002 edge-quality autopsy. Consumes one already-prepared walk-forward
 * dataset. Does not refit Poisson, mutate inputs, or change StrategyConfig.
 */
public final class EdgeQualityDiagnosticsEngine {

	static final BigDecimal THREE_PERCENT = new BigDecimal("0.03");
	static final BigDecimal FIVE_PERCENT = new BigDecimal("0.05");
	static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");
	static final BigDecimal TWENTY_PERCENT = new BigDecimal("0.20");
	static final BigDecimal THIRTY_PERCENT = new BigDecimal("0.30");
	static final BigDecimal FORTY_PERCENT = new BigDecimal("0.40");
	private static final int TOP_EDGE_ROWS = 30;
	private static final BigDecimal WEIGHTED_TOLERANCE = new BigDecimal("1E-18");

	private final SettlementEngine settlementEngine;
	private final PayoutCalculator payoutCalculator;
	private final GeneralizedKellyCalculator expectedReturnCalculator;

	public EdgeQualityDiagnosticsEngine() {
		this(new SettlementEngine(), new PayoutCalculator(), new GeneralizedKellyCalculator());
	}

	public EdgeQualityDiagnosticsEngine(
			SettlementEngine settlementEngine,
			PayoutCalculator payoutCalculator,
			GeneralizedKellyCalculator expectedReturnCalculator) {
		if (settlementEngine == null || payoutCalculator == null || expectedReturnCalculator == null) {
			throw new IllegalArgumentException("settlement, payout, and expected-return calculators are required");
		}
		this.settlementEngine = settlementEngine;
		this.payoutCalculator = payoutCalculator;
		this.expectedReturnCalculator = expectedReturnCalculator;
	}

	public EdgeQualityReport analyze(
			HistoricalWalkForwardDataset dataset, List<NamedBacktestResult> strategyResults) {
		if (dataset == null) {
			throw new IllegalArgumentException("dataset is required");
		}
		int originalSize = dataset.opportunities().size();
		List<EdgeQualityCandidate> candidates = assemble(dataset);
		boolean inputNotMutated = dataset.opportunities().size() == originalSize;
		EdgeQualityGroupSummary all = summarize("all candidates", candidates);
		RankQualityStats rankQuality = rankQuality(candidates);
		List<EdgeQualityGroupSummary> edgeBuckets = mapEnum(
				EdgeQualityEdgeBucket.values(),
				candidates,
				EdgeQualityEdgeBucket::label,
				row -> EdgeQualityEdgeBucket.of(row.predictedEdge()));
		List<EdgeQualityGroupSummary> deciles = edgeDeciles(candidates);
		List<EdgeQualityGroupSummary> byLine = summariesByLine(candidates);
		List<EdgeQualityGroupSummary> byFamily = mapEnum(
				DiagnosticLineFamily.values(),
				candidates,
				Enum::name,
				row -> DiagnosticLineFamily.of(row.selectedLine()));
		List<EdgeQualityGroupSummary> bySide = mapEnum(
				new SelectionType[] {SelectionType.HOME, SelectionType.AWAY},
				candidates,
				Enum::name,
				EdgeQualityCandidate::side);
		List<EdgeQualityGroupSummary> byOdds = mapEnum(
				EdgeQualityOddsBucket.values(),
				candidates,
				EdgeQualityOddsBucket::label,
				row -> EdgeQualityOddsBucket.of(row.odds()));
		ConsistencyChecks consistency = consistency(dataset, candidates, all, edgeBuckets, inputNotMutated);
		return new EdgeQualityReport(
				dataset.stats(),
				candidates.size(),
				all,
				rankQuality,
				edgeBuckets,
				deciles,
				edgeBuckets,
				byLine,
				byFamily,
				bySide,
				byOdds,
				cross(
						candidates,
						row -> EdgeQualityEdgeBucket.of(row.predictedEdge()).label(),
						row -> EdgeQualityOddsBucket.of(row.odds()).label(),
						enumLabels(EdgeQualityEdgeBucket.values(), EdgeQualityEdgeBucket::label),
						enumLabels(EdgeQualityOddsBucket.values(), EdgeQualityOddsBucket::label)),
				crossLines(candidates, row -> EdgeQualityEdgeBucket.of(row.predictedEdge()).label(), edgeBucketLabels()),
				cross(
						candidates,
						row -> EdgeQualityEdgeBucket.of(row.predictedEdge()).label(),
						row -> DiagnosticLineFamily.of(row.selectedLine()).name(),
						edgeBucketLabels(),
						List.of("NEGATIVE_HANDICAP", "ZERO", "POSITIVE_HANDICAP")),
				cross(
						candidates,
						row -> row.side().name(),
						row -> DiagnosticLineFamily.of(row.selectedLine()).name(),
						List.of("HOME", "AWAY"),
						List.of("NEGATIVE_HANDICAP", "ZERO", "POSITIVE_HANDICAP")),
				crossLines(candidates, row -> row.side().name(), List.of("HOME", "AWAY")),
				seasonStability(candidates),
				cross(
						candidates,
						EdgeQualityCandidate::seasonDisplay,
						row -> DiagnosticLineFamily.of(row.selectedLine()).name(),
						seasonKeys(candidates),
						List.of("NEGATIVE_HANDICAP", "ZERO", "POSITIVE_HANDICAP")),
				cross(
						candidates,
						EdgeQualityCandidate::seasonDisplay,
						row -> EdgeQualityEdgeBucket.of(row.predictedEdge()).label(),
						seasonKeys(candidates),
						edgeBucketLabels()),
				highEdge(candidates),
				topEdges(candidates),
				overroundBy(candidates, EdgeQualityCandidate::seasonDisplay, seasonKeys(candidates)),
				overroundByLine(candidates),
				overroundBy(
						candidates,
						row -> EdgeQualityOddsBucket.of(row.odds()).label(),
						enumLabels(EdgeQualityOddsBucket.values(), EdgeQualityOddsBucket::label)),
				twoSidedCoherence(candidates),
				disagreement(candidates),
				positiveLines(candidates),
				confidenceIntervals(candidates),
				consistency,
				strategySnapshots(strategyResults));
	}

	List<EdgeQualityCandidate> assemble(HistoricalWalkForwardDataset dataset) {
		Map<String, HistoricalEventResult> resultsByEvent = new LinkedHashMap<>();
		for (HistoricalEventResult result : dataset.eventResults()) {
			resultsByEvent.put(result.eventId(), result);
		}
		List<EdgeQualityCandidate> analyzed = new ArrayList<>(dataset.opportunities().size());
		for (HistoricalBettingOpportunity historical : dataset.opportunities()) {
			String eventId = historical.opportunity().eventId();
			HistoricalEventResult result = resultsByEvent.get(eventId);
			if (result == null) {
				throw new IllegalArgumentException("missing historical result for event " + eventId);
			}
			SettlementResult settlement =
					settlementEngine.settle(historical.market(), historical.selection(), result.finalScore());
			PayoutResult payout = payoutCalculator.calculate(
					settlement, historical.opportunity().odds(), DiagnosticMath.UNIT_STAKE);
			BigDecimal recomputed = expectedReturnCalculator.expectedReturnRate(
					historical.opportunity().odds(), historical.opportunity().settlementProbabilities());
			EventIdentity identity = parseEvent(eventId);
			analyzed.add(new EdgeQualityCandidate(
					historical.opportunity().opportunityId(),
					eventId,
					seasonOf(historical.opportunity().bettingDate()).displayValue(),
					historical.opportunity().bettingDate(),
					identity.homeTeam(),
					identity.awayTeam(),
					historical.selection().selectionType(),
					historical.selection().line(),
					historical.market().line(),
					historical.opportunity().odds(),
					oppositeOdds(historical),
					historical.opportunity().edge(),
					recomputed,
					historical.opportunity().settlementProbabilities(),
					settlement,
					payout.profit()));
		}
		return List.copyOf(analyzed);
	}

	private static BigDecimal oppositeOdds(HistoricalBettingOpportunity historical) {
		SelectionType side = historical.selection().selectionType();
		SelectionType other = side == SelectionType.HOME ? SelectionType.AWAY : SelectionType.HOME;
		for (BettingSelection selection : historical.market().selections()) {
			if (selection.selectionType() == other && selection.odds() != null) {
				return selection.odds();
			}
		}
		return null;
	}

	private static RankQualityStats rankQuality(List<EdgeQualityCandidate> candidates) {
		List<BigDecimal> edges = new ArrayList<>(candidates.size());
		List<BigDecimal> realized = new ArrayList<>(candidates.size());
		for (EdgeQualityCandidate candidate : candidates) {
			edges.add(candidate.predictedEdge());
			realized.add(candidate.realizedReturnRate());
		}
		return new RankQualityStats(
				candidates.size(),
				DiagnosticCorrelations.spearman(edges, realized),
				DiagnosticCorrelations.pearson(edges, realized));
	}

	private static List<EdgeQualityGroupSummary> edgeDeciles(List<EdgeQualityCandidate> candidates) {
		List<EdgeQualityCandidate> sorted = new ArrayList<>(candidates);
		sorted.sort(Comparator.comparing(EdgeQualityCandidate::predictedEdge)
				.thenComparing(EdgeQualityCandidate::opportunityId));
		Map<Integer, List<EdgeQualityCandidate>> groups = new LinkedHashMap<>();
		for (int d = 0; d < 10; d++) {
			groups.put(d, new ArrayList<>());
		}
		int n = sorted.size();
		for (int i = 0; i < n; i++) {
			int decile = n == 0 ? 0 : Math.min(9, i * 10 / n);
			groups.get(decile).add(sorted.get(i));
		}
		List<EdgeQualityGroupSummary> rows = new ArrayList<>();
		for (int d = 0; d < 10; d++) {
			rows.add(summarize("decile " + (d + 1) + " (lowest edge to highest)", groups.get(d)));
		}
		return List.copyOf(rows);
	}

	private static List<EdgeQualityGroupSummary> summariesByLine(List<EdgeQualityCandidate> candidates) {
		Map<BigDecimal, List<EdgeQualityCandidate>> grouped = new TreeMap<>();
		for (EdgeQualityCandidate candidate : candidates) {
			grouped.computeIfAbsent(candidate.selectedLine(), key -> new ArrayList<>()).add(candidate);
		}
		List<EdgeQualityGroupSummary> rows = new ArrayList<>();
		for (Map.Entry<BigDecimal, List<EdgeQualityCandidate>> entry : grouped.entrySet()) {
			rows.add(summarize(entry.getKey().stripTrailingZeros().toPlainString(), entry.getValue()));
		}
		return List.copyOf(rows);
	}

	private static <E> List<EdgeQualityGroupSummary> mapEnum(
			E[] values,
			List<EdgeQualityCandidate> candidates,
			Function<E, String> label,
			Function<EdgeQualityCandidate, E> classifier) {
		Map<E, List<EdgeQualityCandidate>> grouped = new LinkedHashMap<>();
		for (E value : values) {
			grouped.put(value, new ArrayList<>());
		}
		for (EdgeQualityCandidate candidate : candidates) {
			E key = classifier.apply(candidate);
			grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(candidate);
		}
		List<EdgeQualityGroupSummary> rows = new ArrayList<>();
		for (E value : values) {
			rows.add(summarize(label.apply(value), grouped.getOrDefault(value, List.of())));
		}
		return List.copyOf(rows);
	}

	private static List<CrossCellDiagnostics> cross(
			List<EdgeQualityCandidate> candidates,
			Function<EdgeQualityCandidate, String> rowKey,
			Function<EdgeQualityCandidate, String> columnKey,
			List<String> rowOrder,
			List<String> columnOrder) {
		Map<String, Map<String, List<EdgeQualityCandidate>>> grid = new LinkedHashMap<>();
		for (String row : rowOrder) {
			Map<String, List<EdgeQualityCandidate>> columns = new LinkedHashMap<>();
			for (String column : columnOrder) {
				columns.put(column, new ArrayList<>());
			}
			grid.put(row, columns);
		}
		for (EdgeQualityCandidate candidate : candidates) {
			String row = rowKey.apply(candidate);
			String column = columnKey.apply(candidate);
			grid.computeIfAbsent(row, ignored -> new LinkedHashMap<>())
					.computeIfAbsent(column, ignored -> new ArrayList<>())
					.add(candidate);
		}
		List<CrossCellDiagnostics> cells = new ArrayList<>();
		for (String row : grid.keySet()) {
			for (Map.Entry<String, List<EdgeQualityCandidate>> column : grid.get(row).entrySet()) {
				cells.add(new CrossCellDiagnostics(row, column.getKey(), summarize(row + " × " + column.getKey(), column.getValue())));
			}
		}
		return List.copyOf(cells);
	}

	private static List<CrossCellDiagnostics> crossLines(
			List<EdgeQualityCandidate> candidates,
			Function<EdgeQualityCandidate, String> rowKey,
			List<String> rowOrder) {
		Map<BigDecimal, Integer> lines = new TreeMap<>();
		for (EdgeQualityCandidate candidate : candidates) {
			lines.put(candidate.selectedLine(), 1);
		}
		List<String> columnOrder = new ArrayList<>();
		for (BigDecimal line : lines.keySet()) {
			columnOrder.add(line.stripTrailingZeros().toPlainString());
		}
		return cross(
				candidates,
				rowKey,
				row -> row.selectedLine().stripTrailingZeros().toPlainString(),
				rowOrder,
				columnOrder);
	}

	private static List<SeasonStabilityRow> seasonStability(List<EdgeQualityCandidate> candidates) {
		List<SeasonStabilityRow> rows = new ArrayList<>();
		for (String season : seasonKeys(candidates)) {
			List<EdgeQualityCandidate> inSeason = filter(candidates, row -> row.seasonDisplay().equals(season));
			EdgeQualityGroupSummary all = summarize(season, inSeason);
			EdgeQualityGroupSummary positive =
					summarize(season + " +EV", filter(inSeason, row -> row.predictedEdge().compareTo(BigDecimal.ZERO) > 0));
			EdgeQualityGroupSummary atLeast03 =
					summarize(season + " >=3%", filter(inSeason, row -> row.predictedEdge().compareTo(THREE_PERCENT) >= 0));
			EdgeQualityGroupSummary atLeast10 =
					summarize(season + " >=10%", filter(inSeason, row -> row.predictedEdge().compareTo(TEN_PERCENT) >= 0));
			rows.add(new SeasonStabilityRow(
					season,
					all.n(),
					all.averageEdge(),
					all.unitStakeRoi(),
					positive.n(),
					positive.unitStakeRoi(),
					atLeast03.n(),
					atLeast03.unitStakeRoi(),
					atLeast10.n(),
					atLeast10.unitStakeRoi()));
		}
		return List.copyOf(rows);
	}

	private static List<HighEdgeThresholdDiagnostics> highEdge(List<EdgeQualityCandidate> candidates) {
		return List.of(
				highEdgeAt(candidates, TEN_PERCENT),
				highEdgeAt(candidates, TWENTY_PERCENT),
				highEdgeAt(candidates, THIRTY_PERCENT),
				highEdgeAt(candidates, FORTY_PERCENT));
	}

	static HighEdgeThresholdDiagnostics highEdgeAt(List<EdgeQualityCandidate> candidates, BigDecimal threshold) {
		List<EdgeQualityCandidate> subset =
				filter(candidates, row -> row.predictedEdge().compareTo(threshold) >= 0);
		EdgeQualityGroupSummary summary = summarize("edge >= " + threshold.toPlainString(), subset);
		int home = 0;
		int away = 0;
		Map<String, Integer> byLine = new TreeMap<>();
		Map<String, Integer> bySeason = new TreeMap<>();
		for (EdgeQualityCandidate candidate : subset) {
			if (candidate.side() == SelectionType.HOME) {
				home++;
			}
			else {
				away++;
			}
			byLine.merge(candidate.selectedLine().stripTrailingZeros().toPlainString(), 1, Integer::sum);
			bySeason.merge(candidate.seasonDisplay(), 1, Integer::sum);
		}
		return new HighEdgeThresholdDiagnostics(
				threshold, summary, home, away, shares(byLine, subset.size()), shares(bySeason, subset.size()));
	}

	private static List<ConcentrationShare> shares(Map<String, Integer> counts, int total) {
		List<ConcentrationShare> rows = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			rows.add(new ConcentrationShare(
					entry.getKey(),
					entry.getValue(),
					DiagnosticMath.divide(BigDecimal.valueOf(entry.getValue()), total)));
		}
		return List.copyOf(rows);
	}

	private static List<ForensicCandidateRow> topEdges(List<EdgeQualityCandidate> candidates) {
		List<EdgeQualityCandidate> sorted = new ArrayList<>(candidates);
		sorted.sort(Comparator.comparing(EdgeQualityCandidate::predictedEdge)
				.reversed()
				.thenComparing(EdgeQualityCandidate::opportunityId));
		int limit = Math.min(TOP_EDGE_ROWS, sorted.size());
		List<ForensicCandidateRow> rows = new ArrayList<>(limit);
		for (int i = 0; i < limit; i++) {
			EdgeQualityCandidate candidate = sorted.get(i);
			rows.add(new ForensicCandidateRow(
					candidate.matchDate(),
					candidate.eventId(),
					candidate.homeTeam(),
					candidate.awayTeam(),
					candidate.side(),
					candidate.selectedLine(),
					candidate.odds(),
					candidate.settlementProbabilities(),
					candidate.predictedEdge(),
					candidate.settlement(),
					candidate.realizedReturnRate()));
		}
		return List.copyOf(rows);
	}

	private static List<OverroundGroup> overroundBy(
			List<EdgeQualityCandidate> candidates,
			Function<EdgeQualityCandidate, String> key,
			List<String> order) {
		Map<String, List<BigDecimal>> values = new LinkedHashMap<>();
		for (String item : order) {
			values.put(item, new ArrayList<>());
		}
		for (TwoSidedMarket market : twoSidedMarkets(candidates)) {
			String group = key.apply(market.home());
			values.computeIfAbsent(group, ignored -> new ArrayList<>()).add(market.overround());
		}
		List<OverroundGroup> rows = new ArrayList<>();
		for (Map.Entry<String, List<BigDecimal>> entry : values.entrySet()) {
			rows.add(overroundGroup(entry.getKey(), entry.getValue()));
		}
		return List.copyOf(rows);
	}

	private static List<OverroundGroup> overroundByLine(List<EdgeQualityCandidate> candidates) {
		Map<BigDecimal, List<BigDecimal>> values = new TreeMap<>();
		for (TwoSidedMarket market : twoSidedMarkets(candidates)) {
			values.computeIfAbsent(market.home().marketHomeLine(), ignored -> new ArrayList<>()).add(market.overround());
		}
		List<OverroundGroup> rows = new ArrayList<>();
		for (Map.Entry<BigDecimal, List<BigDecimal>> entry : values.entrySet()) {
			rows.add(overroundGroup(entry.getKey().stripTrailingZeros().toPlainString(), entry.getValue()));
		}
		return List.copyOf(rows);
	}

	private static TwoSidedCoherence twoSidedCoherence(List<EdgeQualityCandidate> candidates) {
		List<TwoSidedMarket> markets = twoSidedMarkets(candidates);
		int both = 0;
		int one = 0;
		int neither = 0;
		List<BigDecimal> sumEdges = new ArrayList<>();
		List<BigDecimal> overrounds = new ArrayList<>();
		for (TwoSidedMarket market : markets) {
			boolean homePos = market.home().predictedEdge().compareTo(BigDecimal.ZERO) > 0;
			boolean awayPos = market.away().predictedEdge().compareTo(BigDecimal.ZERO) > 0;
			if (homePos && awayPos) {
				both++;
			}
			else if (homePos || awayPos) {
				one++;
			}
			else {
				neither++;
			}
			sumEdges.add(market.home().predictedEdge().add(market.away().predictedEdge(), DiagnosticMath.MATH));
			overrounds.add(market.overround());
		}
		return new TwoSidedCoherence(
				markets.size(),
				both,
				one,
				neither,
				DiagnosticMath.average(sumEdges),
				DiagnosticMath.average(overrounds),
				DiagnosticMath.median(overrounds));
	}

	private static OverroundGroup overroundGroup(String key, List<BigDecimal> values) {
		return new OverroundGroup(
				key,
				values.size(),
				DiagnosticMath.average(values),
				DiagnosticMath.median(values),
				values.size() < EdgeQualityGroupSummary.LOW_SAMPLE_THRESHOLD);
	}

	private static List<TwoSidedMarket> twoSidedMarkets(List<EdgeQualityCandidate> candidates) {
		Map<String, EdgeQualityCandidate> home = new LinkedHashMap<>();
		Map<String, EdgeQualityCandidate> away = new LinkedHashMap<>();
		for (EdgeQualityCandidate candidate : candidates) {
			if (candidate.side() == SelectionType.HOME) {
				home.put(candidate.eventId(), candidate);
			}
			else if (candidate.side() == SelectionType.AWAY) {
				away.put(candidate.eventId(), candidate);
			}
		}
		List<TwoSidedMarket> markets = new ArrayList<>();
		for (Map.Entry<String, EdgeQualityCandidate> entry : home.entrySet()) {
			EdgeQualityCandidate awaySide = away.get(entry.getKey());
			if (awaySide == null || entry.getValue().odds() == null || awaySide.odds() == null) {
				continue;
			}
			BigDecimal qHome = BigDecimal.ONE.divide(entry.getValue().odds(), DiagnosticMath.MATH);
			BigDecimal qAway = BigDecimal.ONE.divide(awaySide.odds(), DiagnosticMath.MATH);
			BigDecimal overround = qHome.add(qAway, DiagnosticMath.MATH).subtract(BigDecimal.ONE, DiagnosticMath.MATH);
			markets.add(new TwoSidedMarket(entry.getValue(), awaySide, overround));
		}
		return markets;
	}

	private static List<DisagreementGroup> disagreement(List<EdgeQualityCandidate> candidates) {
		List<DisagreementGroup> rows = new ArrayList<>();
		for (DisagreementMagnitudeBucket bucket : DisagreementMagnitudeBucket.values()) {
			rows.add(new DisagreementGroup(
					bucket,
					summarize(
							bucket.label(),
							filter(
									candidates,
									row -> DisagreementMagnitudeBucket.ofAbsoluteEdge(row.predictedEdge().abs())
											== bucket))));
		}
		return List.copyOf(rows);
	}

	private static List<PositiveLineForensics> positiveLines(List<EdgeQualityCandidate> candidates) {
		Map<BigDecimal, List<EdgeQualityCandidate>> grouped = new TreeMap<>();
		for (EdgeQualityCandidate candidate : candidates) {
			if (candidate.selectedLine().compareTo(BigDecimal.ZERO) > 0) {
				grouped.computeIfAbsent(candidate.selectedLine(), key -> new ArrayList<>()).add(candidate);
			}
		}
		List<PositiveLineForensics> rows = new ArrayList<>();
		for (Map.Entry<BigDecimal, List<EdgeQualityCandidate>> entry : grouped.entrySet()) {
			String key = entry.getKey().stripTrailingZeros().toPlainString();
			List<EdgeQualityCandidate> line = entry.getValue();
			rows.add(new PositiveLineForensics(
					entry.getKey(),
					summarize(key, line),
					summarize(key + " +EV", filter(line, row -> row.predictedEdge().compareTo(BigDecimal.ZERO) > 0)),
					summarize(key + " >=3%", filter(line, row -> row.predictedEdge().compareTo(THREE_PERCENT) >= 0)),
					summarize(key + " >=10%", filter(line, row -> row.predictedEdge().compareTo(TEN_PERCENT) >= 0))));
		}
		return List.copyOf(rows);
	}

	private static List<NamedMeanInterval> confidenceIntervals(List<EdgeQualityCandidate> candidates) {
		List<NamedMeanInterval> rows = new ArrayList<>();
		rows.add(interval("all candidates", candidates));
		rows.add(interval("positive-edge", filter(candidates, row -> row.predictedEdge().compareTo(BigDecimal.ZERO) > 0)));
		rows.add(interval("edge >= 0.03", filter(candidates, row -> row.predictedEdge().compareTo(THREE_PERCENT) >= 0)));
		rows.add(interval("edge >= 0.10", filter(candidates, row -> row.predictedEdge().compareTo(TEN_PERCENT) >= 0)));
		rows.add(interval(
				"NEGATIVE_HANDICAP",
				filter(candidates, row -> DiagnosticLineFamily.of(row.selectedLine()) == DiagnosticLineFamily.NEGATIVE_HANDICAP)));
		rows.add(interval(
				"ZERO", filter(candidates, row -> DiagnosticLineFamily.of(row.selectedLine()) == DiagnosticLineFamily.ZERO)));
		rows.add(interval(
				"POSITIVE_HANDICAP",
				filter(candidates, row -> DiagnosticLineFamily.of(row.selectedLine()) == DiagnosticLineFamily.POSITIVE_HANDICAP)));
		rows.add(interval("HOME", filter(candidates, row -> row.side() == SelectionType.HOME)));
		rows.add(interval("AWAY", filter(candidates, row -> row.side() == SelectionType.AWAY)));
		for (EdgeQualityOddsBucket bucket : EdgeQualityOddsBucket.values()) {
			rows.add(interval(
					bucket.label(), filter(candidates, row -> EdgeQualityOddsBucket.of(row.odds()) == bucket)));
		}
		return List.copyOf(rows);
	}

	private static NamedMeanInterval interval(String label, List<EdgeQualityCandidate> candidates) {
		List<BigDecimal> values = new ArrayList<>(candidates.size());
		for (EdgeQualityCandidate candidate : candidates) {
			values.add(candidate.realizedReturnRate());
		}
		return new NamedMeanInterval(label, DeterministicBootstrap.meanInterval(values));
	}

	private static List<StrategyRegressionSnapshot> strategySnapshots(List<NamedBacktestResult> strategyResults) {
		if (strategyResults == null) {
			return List.of();
		}
		List<StrategyRegressionSnapshot> rows = new ArrayList<>();
		for (NamedBacktestResult named : strategyResults) {
			rows.add(new StrategyRegressionSnapshot(
					named.name(),
					named.result().counts().betsAccepted(),
					named.result().metrics().roi(),
					named.result().pausedByDrawdown()));
		}
		return List.copyOf(rows);
	}

	private ConsistencyChecks consistency(
			HistoricalWalkForwardDataset dataset,
			List<EdgeQualityCandidate> candidates,
			EdgeQualityGroupSummary all,
			List<EdgeQualityGroupSummary> edgeBuckets,
			boolean inputNotMutated) {
		int bucketCount = 0;
		BigDecimal weightedRealized = BigDecimal.ZERO;
		BigDecimal weightedEdge = BigDecimal.ZERO;
		for (EdgeQualityGroupSummary bucket : edgeBuckets) {
			bucketCount += bucket.n();
			if (bucket.n() > 0) {
				weightedRealized = weightedRealized.add(
						bucket.unitStakeRoi().multiply(BigDecimal.valueOf(bucket.n()), DiagnosticMath.MATH),
						DiagnosticMath.MATH);
				weightedEdge = weightedEdge.add(
						bucket.averageEdge().multiply(BigDecimal.valueOf(bucket.n()), DiagnosticMath.MATH),
						DiagnosticMath.MATH);
			}
		}
		boolean exhaustive = bucketCount == candidates.size();
		boolean realizedOk = all.n() == 0
				|| close(
						weightedRealized.divide(BigDecimal.valueOf(all.n()), DiagnosticMath.MATH),
						all.unitStakeRoi());
		boolean edgeOk = all.n() == 0
				|| close(
						weightedEdge.divide(BigDecimal.valueOf(all.n()), DiagnosticMath.MATH),
						all.averageEdge());
		boolean probsSum = true;
		boolean exclusiveSettlement = true;
		boolean expectedMatches = true;
		boolean payoutMatches = true;
		for (EdgeQualityCandidate candidate : candidates) {
			SettlementProbabilityDistribution p = candidate.settlementProbabilities();
			BigDecimal sum = p.winProbability()
					.add(p.halfWinProbability())
					.add(p.pushProbability())
					.add(p.halfLossProbability())
					.add(p.lossProbability());
			if (sum.compareTo(BigDecimal.ONE) != 0) {
				probsSum = false;
			}
			if (candidate.settlement() == null) {
				exclusiveSettlement = false;
			}
			if (candidate.predictedEdge().compareTo(candidate.recomputedExpectedReturn()) != 0) {
				expectedMatches = false;
			}
			PayoutResult payout = payoutCalculator.calculate(
					candidate.settlement(), candidate.odds(), DiagnosticMath.UNIT_STAKE);
			if (payout.profit().compareTo(candidate.realizedReturnRate()) != 0) {
				payoutMatches = false;
			}
		}
		boolean countMatches = candidates.size() == dataset.stats().candidatesGenerated()
				|| dataset.stats().candidatesGenerated() == 0;
		return new ConsistencyChecks(
				exhaustive && countMatches,
				realizedOk,
				edgeOk,
				probsSum,
				exclusiveSettlement,
				expectedMatches,
				payoutMatches,
				inputNotMutated);
	}

	private static boolean close(BigDecimal left, BigDecimal right) {
		if (left == null || right == null) {
			return left == null && right == null;
		}
		return left.subtract(right, DiagnosticMath.MATH).abs().compareTo(WEIGHTED_TOLERANCE) <= 0;
	}

	private static EdgeQualityGroupSummary summarize(String key, List<EdgeQualityCandidate> rows) {
		int n = rows.size();
		List<BigDecimal> edges = new ArrayList<>(n);
		List<BigDecimal> odds = new ArrayList<>(n);
		BigDecimal predictedProfit = BigDecimal.ZERO;
		BigDecimal realizedProfit = BigDecimal.ZERO;
		SettlementCounts settlements = SettlementCounts.empty();
		BigDecimal pWin = BigDecimal.ZERO;
		BigDecimal pHalfWin = BigDecimal.ZERO;
		BigDecimal pPush = BigDecimal.ZERO;
		BigDecimal pHalfLoss = BigDecimal.ZERO;
		BigDecimal pLoss = BigDecimal.ZERO;
		for (EdgeQualityCandidate row : rows) {
			edges.add(row.predictedEdge());
			odds.add(row.odds());
			predictedProfit = predictedProfit.add(row.predictedEdge(), DiagnosticMath.MATH);
			realizedProfit = realizedProfit.add(row.realizedReturnRate(), DiagnosticMath.MATH);
			settlements = settlements.plus(row.settlement());
			SettlementProbabilityDistribution p = row.settlementProbabilities();
			pWin = pWin.add(p.winProbability(), DiagnosticMath.MATH);
			pHalfWin = pHalfWin.add(p.halfWinProbability(), DiagnosticMath.MATH);
			pPush = pPush.add(p.pushProbability(), DiagnosticMath.MATH);
			pHalfLoss = pHalfLoss.add(p.halfLossProbability(), DiagnosticMath.MATH);
			pLoss = pLoss.add(p.lossProbability(), DiagnosticMath.MATH);
		}
		BigDecimal averageEdge = DiagnosticMath.average(edges);
		BigDecimal averageRealized = DiagnosticMath.divide(realizedProfit, n);
		List<BigDecimal> sortedEdges = new ArrayList<>(edges);
		sortedEdges.sort(Comparator.naturalOrder());
		return new EdgeQualityGroupSummary(
				key,
				n,
				n > 0 && n < EdgeQualityGroupSummary.LOW_SAMPLE_THRESHOLD,
				averageEdge,
				DiagnosticMath.quantile(sortedEdges, new BigDecimal("0.50")),
				DiagnosticMath.average(odds),
				averageRealized,
				predictedProfit,
				realizedProfit,
				averageRealized == null || averageEdge == null
						? null
						: averageRealized.subtract(averageEdge, DiagnosticMath.MATH),
				settlements,
				new SettlementCalibration(
						outcome(pWin, settlements.win(), n),
						outcome(pHalfWin, settlements.halfWin(), n),
						outcome(pPush, settlements.push(), n),
						outcome(pHalfLoss, settlements.halfLoss(), n),
						outcome(pLoss, settlements.loss(), n)));
	}

	private static OutcomeCalibration outcome(BigDecimal predictedSum, int actualCount, int n) {
		BigDecimal predicted = DiagnosticMath.divide(predictedSum, n);
		BigDecimal actual = DiagnosticMath.divide(BigDecimal.valueOf(actualCount), n);
		BigDecimal gap = predicted == null || actual == null ? null : actual.subtract(predicted, DiagnosticMath.MATH);
		return new OutcomeCalibration(predicted, actual, gap);
	}

	private static List<EdgeQualityCandidate> filter(
			List<EdgeQualityCandidate> rows, Predicate<EdgeQualityCandidate> predicate) {
		List<EdgeQualityCandidate> filtered = new ArrayList<>();
		for (EdgeQualityCandidate row : rows) {
			if (predicate.test(row)) {
				filtered.add(row);
			}
		}
		return filtered;
	}

	private static List<String> edgeBucketLabels() {
		return enumLabels(EdgeQualityEdgeBucket.values(), EdgeQualityEdgeBucket::label);
	}

	private static <E> List<String> enumLabels(E[] values, Function<E, String> label) {
		List<String> labels = new ArrayList<>();
		for (E value : values) {
			labels.add(label.apply(value));
		}
		return labels;
	}

	private static List<String> seasonKeys(List<EdgeQualityCandidate> candidates) {
		Map<String, Integer> years = new TreeMap<>();
		for (EdgeQualityCandidate candidate : candidates) {
			years.putIfAbsent(candidate.seasonDisplay(), seasonOf(candidate.matchDate()).startYear());
		}
		List<String> ordered = new ArrayList<>(years.keySet());
		ordered.sort(Comparator.comparingInt((String display) -> years.get(display))
				.thenComparing(Comparator.naturalOrder()));
		return ordered;
	}

	private static FootballSeason seasonOf(LocalDate date) {
		int startYear = date.getMonthValue() >= 7 ? date.getYear() : date.getYear() - 1;
		return new FootballSeason(startYear, startYear + 1);
	}

	private static EventIdentity parseEvent(String eventId) {
		String[] parts = eventId.split(":", 6);
		if (parts.length == 6) {
			return new EventIdentity(parts[3], parts[4]);
		}
		return new EventIdentity(null, null);
	}

	private record EventIdentity(String homeTeam, String awayTeam) {
	}

	private record TwoSidedMarket(EdgeQualityCandidate home, EdgeQualityCandidate away, BigDecimal overround) {
	}
}
