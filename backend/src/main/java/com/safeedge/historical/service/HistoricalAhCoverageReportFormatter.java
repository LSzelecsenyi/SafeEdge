package com.safeedge.historical.service;

import com.safeedge.historical.domain.HistoricalAhCoverageReport;
import com.safeedge.historical.domain.HistoricalAhLeagueSeasonCoverage;
import com.safeedge.historical.domain.HistoricalAhQuoteSourceCoverage;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class HistoricalAhCoverageReportFormatter {

	private HistoricalAhCoverageReportFormatter() {
	}

	public static String format(HistoricalAhCoverageReport report) {
		if (report.leagueSeasons().isEmpty()) {
			return "No persisted historical matches for coverage audit.";
		}
		StringBuilder text = new StringBuilder();
		for (HistoricalAhLeagueSeasonCoverage row : report.leagueSeasons()) {
			text.append(row.competition())
					.append(' ')
					.append(row.season().displayValue())
					.append('\n')
					.append("  matches: ")
					.append(row.totalMatches())
					.append('\n');
			for (HistoricalAhQuoteSourceCoverage source : row.sourceCoverages()) {
				text.append("  ")
						.append(pad(source.quoteSource().name()))
						.append(source.matchesWithQuote())
						.append(" / ")
						.append(source.totalMatches())
						.append(" = ")
						.append(percent(source.coverageRate()))
						.append('\n');
			}
			text.append("  ")
					.append(pad("ANY"))
					.append(row.matchesWithAnyQuote())
					.append(" / ")
					.append(row.totalMatches())
					.append(" = ")
					.append(percent(row.anyQuoteCoverageRate()))
					.append('\n');
			if (row.bestQuoteSource() != null) {
				text.append("  bestQuoteSource: ")
						.append(row.bestQuoteSource())
						.append(" (")
						.append(percent(row.bestQuoteSourceCoverageRate()))
						.append(")\n");
			}
			text.append('\n');
		}
		return text.toString().stripTrailing();
	}

	private static String pad(String label) {
		return String.format("%-15s", label + ":");
	}

	private static String percent(BigDecimal rate) {
		return rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
	}
}
