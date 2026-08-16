package com.safeedge.historical.diagnostics;

/**
 * Structural quality of Probability Model v3 versus the better of frozen v1/v2
 * on development leagues. Positive ROI is not a criterion. Thresholds were
 * declared before the live evaluation and must not be moved afterwards.
 */
public enum ProbabilityModelV3Classification {
	MODEL_V3_REGRESSION,
	MODEL_V3_NO_MEANINGFUL_IMPROVEMENT,
	MODEL_V3_PARTIAL_IMPROVEMENT,
	MODEL_V3_CLEAR_IMPROVEMENT
}
