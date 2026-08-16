package com.safeedge.historical.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BundesligaPublishedBaselineTest {

	@Test
	void publishedMarkdownStillContainsFrozenBaselineValues() throws Exception {
		String bundesliga = Files.readString(report("baseline-003-bundesliga.md"), StandardCharsets.UTF_8);
		String crossLeague = Files.readString(report("baseline-003-cross-league-validation.md"), StandardCharsets.UTF_8);
		assertThat(bundesliga).contains("Competition: BUNDESLIGA");
		assertThat(bundesliga).contains("HISTORICAL QUOTE SOURCE = MARKET_AVERAGE");
		assertThat(bundesliga).contains("not Tippmix");
		assertThat(bundesliga).contains("Candidates analyzed: 2962");
		assertThat(bundesliga).contains("Positive / zero / negative EV: 1327 / 0 / 1635");
		assertThat(bundesliga).contains("Predicted vs actual home goals: 1.748046 vs 1.746793");
		assertThat(bundesliga).contains("Spearman(predicted edge, realized unit return): -0.010761");
		assertThat(bundesliga).contains("Pearson(predicted edge, realized unit return): -0.033968");
		assertThat(bundesliga).contains("| DEFENSIVE | 194 | -0.034371 | true |");
		assertThat(bundesliga).contains("| BALANCED | 202 | -0.022986 | true |");
		assertThat(bundesliga).contains("| GROWTH | 109 | -0.047705 | true |");
		assertThat(bundesliga).contains("| FLAT_STAKE | 375 | -0.028569 | true |");
		assertThat(crossLeague).contains("FAILURE STRONGLY REPLICATES");
		assertThat(crossLeague).contains("PREMIER_LEAGUE");
		assertThat(crossLeague).contains("BUNDESLIGA");
		LeagueDiagnosticSnapshot snapshot = BundesligaPublishedBaseline.snapshot();
		assertThat(snapshot.candidateCount()).isEqualTo(2962);
		assertThat(snapshot.spearman()).isEqualByComparingTo("-0.010761");
		assertThat(snapshot.predictionQuality().predictionsAvailable()).isEqualTo(1481);
		assertThat(snapshot.matchesSkippedMissingQuote()).isZero();
		assertThat(CrossLeagueComparisonEngine.compare(
						PremierLeaguePublishedBaseline.snapshot(), snapshot)
				.classification())
				.isEqualTo(StructuralReplicationClassification.FAILURE_STRONGLY_REPLICATES);
	}

	private static Path report(String fileName) {
		Path fromBackend = Path.of("..", "docs", "results", fileName);
		if (Files.exists(fromBackend)) {
			return fromBackend;
		}
		return Path.of("docs", "results", fileName);
	}
}
