package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pearson and Spearman without an extra statistics library.
 * Spearman uses average ranks for ties, then Pearson on those ranks.
 */
final class DiagnosticCorrelations {

	private DiagnosticCorrelations() {
	}

	static BigDecimal pearson(List<BigDecimal> xs, List<BigDecimal> ys) {
		if (xs == null || ys == null || xs.size() != ys.size() || xs.size() < 2) {
			return null;
		}
		BigDecimal meanX = DiagnosticMath.average(xs);
		BigDecimal meanY = DiagnosticMath.average(ys);
		BigDecimal sumXy = BigDecimal.ZERO;
		BigDecimal sumX2 = BigDecimal.ZERO;
		BigDecimal sumY2 = BigDecimal.ZERO;
		for (int i = 0; i < xs.size(); i++) {
			BigDecimal dx = xs.get(i).subtract(meanX, DiagnosticMath.MATH);
			BigDecimal dy = ys.get(i).subtract(meanY, DiagnosticMath.MATH);
			sumXy = sumXy.add(dx.multiply(dy, DiagnosticMath.MATH), DiagnosticMath.MATH);
			sumX2 = sumX2.add(dx.multiply(dx, DiagnosticMath.MATH), DiagnosticMath.MATH);
			sumY2 = sumY2.add(dy.multiply(dy, DiagnosticMath.MATH), DiagnosticMath.MATH);
		}
		if (sumX2.compareTo(BigDecimal.ZERO) == 0 || sumY2.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		return sumXy.divide(sqrt(sumX2.multiply(sumY2, DiagnosticMath.MATH)), DiagnosticMath.MATH);
	}

	static BigDecimal spearman(List<BigDecimal> xs, List<BigDecimal> ys) {
		if (xs == null || ys == null || xs.size() != ys.size() || xs.size() < 2) {
			return null;
		}
		return pearson(averageRanks(xs), averageRanks(ys));
	}

	static List<BigDecimal> averageRanks(List<BigDecimal> values) {
		int n = values.size();
		List<IndexedValue> indexed = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			indexed.add(new IndexedValue(i, values.get(i)));
		}
		indexed.sort(Comparator.comparing(IndexedValue::value).thenComparingInt(IndexedValue::index));
		BigDecimal[] ranks = new BigDecimal[n];
		int i = 0;
		while (i < n) {
			int j = i;
			while (j + 1 < n && indexed.get(j + 1).value().compareTo(indexed.get(i).value()) == 0) {
				j++;
			}
			BigDecimal rankSum = BigDecimal.ZERO;
			for (int k = i; k <= j; k++) {
				rankSum = rankSum.add(BigDecimal.valueOf(k + 1), DiagnosticMath.MATH);
			}
			BigDecimal average = rankSum.divide(BigDecimal.valueOf(j - i + 1), DiagnosticMath.MATH);
			for (int k = i; k <= j; k++) {
				ranks[indexed.get(k).index()] = average;
			}
			i = j + 1;
		}
		return List.of(ranks);
	}

	private static BigDecimal sqrt(BigDecimal value) {
		return value.sqrt(DiagnosticMath.MATH);
	}

	private record IndexedValue(int index, BigDecimal value) {
	}
}
