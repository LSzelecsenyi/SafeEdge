package com.safeedge.historical.diagnostics;

import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.D19;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.dataset;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.home;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.result;
import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.HistoricalQuoteSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class BaselineDiagnosticsReportFormatterTest {

	@Test
	void reportLabelsHistoricalQuoteSourceAndDoesNotCallItTippmix() {
		BaselineDiagnosticsReport report = new BaselineDiagnosticsEngine().analyze(
				dataset(List.of(home("a", "e1", D19, "0", "1.80", "0.02")), List.of(result("e1", D19, 1, 0))),
				List.of(),
				List.of());
		String markdown = BaselineDiagnosticsReportFormatter.format(report);
		assertThat(report.overview().datasetStats().quoteSource()).isEqualTo(HistoricalQuoteSource.MARKET_AVERAGE);
		assertThat(markdown).contains("HISTORICAL QUOTE SOURCE = MARKET_AVERAGE");
		assertThat(markdown).contains("not Tippmix");
		assertThat(markdown).contains("MARKET_AVERAGE is not Tippmix");
		assertThat(markdown).contains("# Baseline 001 Diagnostics");
		assertThat(markdown).contains("## Explicit non-conclusions");
		assertThat(markdown).contains("no parameter optimization performed");
		assertThat(markdown).contains("best-looking bucket is not validated strategy");
		assertThat(markdown).doesNotContain("adopt this threshold");
		assertThat(markdown).doesNotContain("recommended new preset");
	}
}
