package com.safeedge.historical.diagnostics;

import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.BINARY_60;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.D19;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.dataset;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.priced;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.result;
import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import java.util.List;
import org.junit.jupiter.api.Test;

class EdgeQualityReportFormatterTest {

	@Test
	void reportLabelsHistoricalQuoteSourceAndForbidsStrategyPromotion() {
		EdgeQualityReport report = new EdgeQualityDiagnosticsEngine().analyze(
				dataset(
						List.of(priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60)),
						List.of(result("e1", D19, 1, 0))),
				List.of());
		String markdown = EdgeQualityReportFormatter.format(report);
		assertThat(report.datasetStats().quoteSource()).isEqualTo(HistoricalQuoteSource.MARKET_AVERAGE);
		assertThat(markdown).contains("# Baseline 002 – Edge Quality / Market Calibration");
		assertThat(markdown).contains("HISTORICAL QUOTE SOURCE = MARKET_AVERAGE");
		assertThat(markdown).contains("not Tippmix");
		assertThat(markdown).contains("## 17. Explicit non-conclusions");
		assertThat(markdown).contains("no production filter selected");
		assertThat(markdown).contains("HYPOTHESES TO TEST NEXT");
		assertThat(markdown).doesNotContain("adopt this threshold");
		assertThat(markdown).contains("MARKET-IMPLIED REFERENCE");
	}
}
