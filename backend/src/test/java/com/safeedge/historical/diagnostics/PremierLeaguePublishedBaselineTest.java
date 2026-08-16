package com.safeedge.historical.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PremierLeaguePublishedBaselineTest {

	@Test
	void publishedMarkdownStillContainsFrozenBaselineValues() throws Exception {
		String baseline001 = Files.readString(report("baseline-001-diagnostics.md"), StandardCharsets.UTF_8);
		String baseline002 = Files.readString(report("baseline-002-edge-quality.md"), StandardCharsets.UTF_8);
		assertThat(baseline001).contains("Competition: PREMIER_LEAGUE");
		assertThat(baseline001).contains("HISTORICAL QUOTE SOURCE = MARKET_AVERAGE");
		assertThat(baseline001).contains("not Tippmix");
		assertThat(baseline001).contains("Candidates analyzed: 2940");
		assertThat(baseline001).contains("Positive / zero / negative EV: 1306 / 0 / 1634");
		assertThat(baseline001).contains("Predicted vs actual home goals: 1.562478 vs 1.57551");
		assertThat(baseline001).contains("Predicted vs actual home-win: 0.443058 vs 0.446939");
		assertThat(baseline002).contains("Competition: PREMIER_LEAGUE");
		assertThat(baseline002).contains("Spearman(predicted edge, realized unit return): 0.0172");
		assertThat(baseline002).contains("Pearson(predicted edge, realized unit return): 0.012664");
		assertThat(baseline002).contains("Average overround: 0.035373");
		assertThat(baseline002).contains("| DEFENSIVE | 117 | -0.030255 | true |");
		assertThat(baseline002).contains("| BALANCED | 122 | -0.021072 | true |");
		assertThat(baseline002).contains("| GROWTH | 123 | -0.021641 | true |");
		assertThat(baseline002).contains("| FLAT_STAKE | 588 | -0.038249 | true |");
		assertThat(baseline002).contains("n=851 avgEdge=0.245616");
		LeagueDiagnosticSnapshot snapshot = PremierLeaguePublishedBaseline.snapshot();
		assertThat(snapshot.candidateCount()).isEqualTo(2940);
		assertThat(snapshot.spearman()).isEqualByComparingTo("0.0172");
		assertThat(snapshot.predictionQuality().predictionsAvailable()).isEqualTo(1470);
	}

	private static Path report(String fileName) {
		Path fromBackend = Path.of("..", "docs", "results", fileName);
		if (Files.exists(fromBackend)) {
			return fromBackend;
		}
		return Path.of("docs", "results", fileName);
	}
}
