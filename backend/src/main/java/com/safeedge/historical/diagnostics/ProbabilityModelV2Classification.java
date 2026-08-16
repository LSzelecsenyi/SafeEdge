package com.safeedge.historical.diagnostics;

/**
 * Structural quality of Probability Model v2 versus frozen v1 on development
 * leagues. Positive ROI is not a criterion.
 */
public enum ProbabilityModelV2Classification {
	MODEL_V2_WORSE,
	MODEL_V2_NO_MEANINGFUL_IMPROVEMENT,
	MODEL_V2_PARTIAL_IMPROVEMENT,
	MODEL_V2_CLEAR_STRUCTURAL_IMPROVEMENT
}
