package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Predeclared v3 vs better-of-v1/v2 classification. Thresholds were chosen
 * before historical evaluation and are not ROI-fitted.
 *
 * {@link ProbabilityModelV3Classification#MODEL_V3_CLEAR_IMPROVEMENT} requires:
 * <ol>
 *   <li>Spearman improvement ≥ {@link #SPEARMAN_MATERIAL_DELTA} versus the better
 *       of v1/v2 in at least 2 of 3 development leagues</li>
 *   <li>no material ranking deterioration in the third
 *       (Spearman not worse by ≥ {@link #SPEARMAN_DETERIORATION})</li>
 *   <li>≥ {@link #HIGH_EDGE_GAP_SHRINK} shrink in ≥10% edge WIN <em>and</em> LOSS
 *       absolute calibration gaps versus the better of v1/v2 in at least 2 of 3
 *       leagues</li>
 *   <li>score log loss not worse by more than {@link #LOG_LOSS_MATERIAL_WORSE}
 *       versus the better of v1/v2 in any league</li>
 * </ol>
 *
 * Positive ROI is ignored.
 */
public final class ProbabilityModelV3ClassificationEngine {

	static final BigDecimal SPEARMAN_MATERIAL_DELTA = new BigDecimal("0.05");
	static final BigDecimal SPEARMAN_SMALL_DELTA = new BigDecimal("0.02");
	static final BigDecimal SPEARMAN_DETERIORATION = new BigDecimal("0.02");
	static final BigDecimal HIGH_EDGE_GAP_SHRINK = new BigDecimal("0.03");
	static final BigDecimal LOG_LOSS_MATERIAL_WORSE = new BigDecimal("0.02");

	private ProbabilityModelV3ClassificationEngine() {
	}

	public static ClassificationDecision classify(List<ProbabilityModelV3Comparison> leagues) {
		if (leagues == null || leagues.size() != ProbabilityModelDevelopmentLeagues.DEVELOPMENT.size()) {
			throw new IllegalArgumentException("classification requires the three development leagues");
		}
		List<String> reasons = new ArrayList<>();
		int spearmanMaterial = 0;
		int spearmanSmall = 0;
		int spearmanWorse = 0;
		int winAndLossGapShrink = 0;
		int winGapWorse = 0;
		int logLossWorse = 0;
		for (ProbabilityModelV3Comparison league : leagues) {
			String name = league.competition().name();
			BigDecimal spearmanDelta = league.spearmanDeltaVsBetterBaseline();
			if (atLeast(spearmanDelta, SPEARMAN_MATERIAL_DELTA)) {
				spearmanMaterial++;
				reasons.add(name + " Spearman improved by " + spearmanDelta.toPlainString() + " vs better of v1/v2");
			}
			else if (atLeast(spearmanDelta, SPEARMAN_SMALL_DELTA)) {
				spearmanSmall++;
				reasons.add(name + " Spearman improved slightly by " + spearmanDelta.toPlainString());
			}
			else if (atMost(spearmanDelta, SPEARMAN_DETERIORATION.negate())) {
				spearmanWorse++;
				reasons.add(name + " Spearman worsened by " + spearmanDelta.toPlainString() + " vs better of v1/v2");
			}
			else {
				reasons.add(name + " Spearman delta " + nullSafe(spearmanDelta) + " is below the small-improvement cutoff");
			}
			boolean winShrink = atLeast(league.winGap10ShrinkVsBetterBaseline(), HIGH_EDGE_GAP_SHRINK);
			boolean lossShrink = atLeast(league.lossGap10ShrinkVsBetterBaseline(), HIGH_EDGE_GAP_SHRINK);
			if (winShrink && lossShrink) {
				winAndLossGapShrink++;
			}
			if (atMost(league.winGap10ShrinkVsBetterBaseline(), HIGH_EDGE_GAP_SHRINK.negate())) {
				winGapWorse++;
			}
			if (worsenedBy(league.logLossDeltaVsBetterBaseline(), LOG_LOSS_MATERIAL_WORSE)) {
				logLossWorse++;
				reasons.add(name + " score log loss worsened by " + nullSafe(league.logLossDeltaVsBetterBaseline()));
			}
		}
		boolean clear = spearmanMaterial >= 2
				&& spearmanWorse == 0
				&& winAndLossGapShrink >= 2
				&& logLossWorse == 0;
		if (clear) {
			reasons.add("At least two leagues met the predeclared CLEAR IMPROVEMENT gates versus the better of v1/v2.");
			return new ClassificationDecision(ProbabilityModelV3Classification.MODEL_V3_CLEAR_IMPROVEMENT, reasons);
		}
		boolean regression = (spearmanWorse >= 2 || logLossWorse >= 2 || winGapWorse >= 2)
				&& spearmanMaterial == 0
				&& winAndLossGapShrink <= 1;
		if (regression) {
			reasons.add("Ranking, score likelihood, or high-edge WIN calibration worsened in at least two leagues without offsetting CLEAR/PARTIAL gains.");
			return new ClassificationDecision(ProbabilityModelV3Classification.MODEL_V3_REGRESSION, reasons);
		}
		boolean partial = logLossWorse == 0
				&& spearmanWorse <= 1
				&& (spearmanMaterial + spearmanSmall >= 2 || winAndLossGapShrink >= 2);
		if (partial) {
			reasons.add("Some ranking or calibration gates improved versus the better of v1/v2, but not the full CLEAR set.");
			return new ClassificationDecision(ProbabilityModelV3Classification.MODEL_V3_PARTIAL_IMPROVEMENT, reasons);
		}
		reasons.add("Changes were mixed or smaller than the predeclared material thresholds. ROI was not used.");
		return new ClassificationDecision(ProbabilityModelV3Classification.MODEL_V3_NO_MEANINGFUL_IMPROVEMENT, reasons);
	}

	private static boolean atLeast(BigDecimal value, BigDecimal threshold) {
		return value != null && value.compareTo(threshold) >= 0;
	}

	private static boolean atMost(BigDecimal value, BigDecimal threshold) {
		return value != null && value.compareTo(threshold) <= 0;
	}

	private static boolean worsenedBy(BigDecimal delta, BigDecimal threshold) {
		return delta != null && delta.compareTo(threshold) > 0;
	}

	private static String nullSafe(BigDecimal value) {
		return value == null ? "n/a" : value.stripTrailingZeros().toPlainString();
	}

	public record ClassificationDecision(ProbabilityModelV3Classification classification, List<String> reasons) {

		public ClassificationDecision {
			if (classification == null) {
				throw new IllegalArgumentException("classification is required");
			}
			reasons = List.copyOf(reasons == null ? List.of() : reasons);
		}
	}
}
