package com.safeedge.strategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * Generalized Kelly for the five {@code SettlementResult} outcomes.
 *
 * Solves {@code G'(f) = Σ p_i R_i / (1 + f R_i) = 0} by bisection on
 * {@code [0, F_UPPER]} where {@code F_UPPER = 1 - 10^-12}. The upper bound is
 * strictly below 1 because a LOSS has {@code R = -1}.
 *
 * Arithmetic uses {@link MathContext#DECIMAL128}. No binary Kelly shortcut.
 */
public final class GeneralizedKellyCalculator {

	static final MathContext MATH = MathContext.DECIMAL128;
	static final BigDecimal F_UPPER = new BigDecimal("0.999999999999");
	private static final BigDecimal TWO = new BigDecimal("2");
	private static final BigDecimal HALF = new BigDecimal("0.5");
	private static final BigDecimal TOLERANCE = new BigDecimal("1E-24");
	private static final int MAX_ITERATIONS = 128;

	public BigDecimal expectedReturnRate(BigDecimal odds, SettlementProbabilityDistribution distribution) {
		return derivative(BigDecimal.ZERO, terms(odds, distribution));
	}

	public BigDecimal fullKellyFraction(BigDecimal odds, SettlementProbabilityDistribution distribution) {
		List<Term> terms = terms(odds, distribution);
		BigDecimal derivativeAtZero = derivative(BigDecimal.ZERO, terms);
		if (derivativeAtZero.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal derivativeAtUpper = derivative(F_UPPER, terms);
		if (derivativeAtUpper.compareTo(BigDecimal.ZERO) >= 0) {
			return F_UPPER;
		}
		BigDecimal low = BigDecimal.ZERO;
		BigDecimal high = F_UPPER;
		for (int i = 0; i < MAX_ITERATIONS; i++) {
			if (high.subtract(low).compareTo(TOLERANCE) <= 0) {
				break;
			}
			BigDecimal mid = low.add(high).divide(TWO, MATH);
			int sign = derivative(mid, terms).compareTo(BigDecimal.ZERO);
			if (sign == 0) {
				return mid.stripTrailingZeros();
			}
			if (sign > 0) {
				low = mid;
			}
			else {
				high = mid;
			}
		}
		return low.add(high).divide(TWO, MATH).stripTrailingZeros();
	}

	private static List<Term> terms(BigDecimal odds, SettlementProbabilityDistribution distribution) {
		if (odds == null || odds.compareTo(BigDecimal.ONE) <= 0) {
			throw new StrategyException("odds must be greater than 1");
		}
		if (distribution == null) {
			throw new StrategyException("settlementProbabilities is required");
		}
		BigDecimal winReturn = odds.subtract(BigDecimal.ONE);
		return List.of(
				new Term(distribution.winProbability(), winReturn),
				new Term(distribution.halfWinProbability(), winReturn.divide(TWO, MATH)),
				new Term(distribution.pushProbability(), BigDecimal.ZERO),
				new Term(distribution.halfLossProbability(), HALF.negate()),
				new Term(distribution.lossProbability(), BigDecimal.ONE.negate()));
	}

	private static BigDecimal derivative(BigDecimal fraction, List<Term> terms) {
		BigDecimal slope = BigDecimal.ZERO;
		for (Term term : terms) {
			if (term.probability().compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			BigDecimal denominator = BigDecimal.ONE.add(fraction.multiply(term.netReturn()));
			if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
				throw new StrategyException("Kelly fraction is outside the valid domain");
			}
			slope = slope.add(term.probability().multiply(term.netReturn()).divide(denominator, MATH));
		}
		return slope;
	}

	private record Term(BigDecimal probability, BigDecimal netReturn) {
	}

}
