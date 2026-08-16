package com.safeedge.historical.footballdata.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.domain.MappedHistoricalMatch;
import com.safeedge.historical.footballdata.dto.FootballDataCsvRow;
import com.safeedge.historical.footballdata.parser.FootballDataCsvParser;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FootballDataHistoricalMapperTest {

	private final FootballDataCsvParser parser = new FootballDataCsvParser();
	private final FootballDataHistoricalMapper mapper = new FootballDataHistoricalMapper();
	private final FootballSeason season = new FootballSeason(2023, 2024);

	@Test
	void mapsCoreMatchAndPreservesExactTeamNamesAndProvenance() {
		FootballDataCsvRow row = parser.parse(fixture("sample-e0.csv")).get(1);
		MappedHistoricalMatch mapped = mapper.map(row, FootballDataLeague.E0, season, "mmz4281/2324/E0.csv").orElseThrow();
		assertThat(mapped.match().source()).isEqualTo(HistoricalSource.FOOTBALL_DATA_UK);
		assertThat(mapped.match().sourceCompetitionCode()).isEqualTo("E0");
		assertThat(mapped.match().sourceHomeTeamName()).isEqualTo("Man United");
		assertThat(mapped.match().sourceAwayTeamName()).isEqualTo("Wolves");
		assertThat(mapped.match().homeGoals()).isEqualTo(1);
		assertThat(mapped.match().awayGoals()).isEqualTo(0);
		assertThat(mapped.match().matchDate()).isEqualTo(LocalDate.of(2023, 8, 12));
		assertThat(mapped.match().sourceKickoffTime()).isEqualTo(java.time.LocalTime.of(17, 30));
		assertThat(mapped.match().kickoffUtc()).isNull();
		assertThat(mapped.match().sourceFile()).isEqualTo("mmz4281/2324/E0.csv");
		assertThat(mapped.match().sourceRowNumber()).isGreaterThan(1);
	}

	@Test
	void mapsQuoteSourcesFromVerifiedColumns() {
		MappedHistoricalMatch arsenal = mapper.map(
				parser.parse(fixture("sample-e0.csv")).getFirst(),
				FootballDataLeague.E0,
				season,
				"file.csv").orElseThrow();
		assertThat(arsenal.quotes())
				.extracting(quote -> quote.quoteSource())
				.containsExactly(
						HistoricalQuoteSource.BET365,
						HistoricalQuoteSource.PINNACLE,
						HistoricalQuoteSource.MARKET_MAX,
						HistoricalQuoteSource.MARKET_AVERAGE);
		assertThat(arsenal.quotes().getFirst().observationType())
				.isEqualTo(HistoricalObservationType.PRE_MATCH_SNAPSHOT);
		assertThat(arsenal.quotes().getFirst().observedAt()).isNull();
		assertThat(arsenal.quotes().getFirst().sourceLineColumn()).isEqualTo("B365AH");
	}

	@Test
	void missingAhStillMapsTheMatch() {
		MappedHistoricalMatch everton = mapper.map(
				parser.parse(fixture("sample-e0.csv")).get(4),
				FootballDataLeague.E0,
				season,
				"file.csv").orElseThrow();
		assertThat(everton.match().sourceHomeTeamName()).isEqualTo("Everton");
		assertThat(everton.quotes()).isEmpty();
	}

	@Test
	void incompleteAndInvalidQuotesAreCountedNotInvented() {
		List<FootballDataCsvRow> rows = parser.parse(fixture("sample-e0.csv"));
		MappedHistoricalMatch liverpool = mapper.map(rows.get(5), FootballDataLeague.E0, season, "file.csv").orElseThrow();
		assertThat(liverpool.quotesSkippedIncomplete()).isGreaterThanOrEqualTo(1);
		assertThat(liverpool.quotes()).extracting(quote -> quote.quoteSource())
				.doesNotContain(HistoricalQuoteSource.BET365);
		MappedHistoricalMatch newcastle = mapper.map(rows.get(6), FootballDataLeague.E0, season, "file.csv").orElseThrow();
		assertThat(newcastle.quotesSkippedInvalidOdds()).isGreaterThanOrEqualTo(1);
		MappedHistoricalMatch westHam = mapper.map(rows.get(7), FootballDataLeague.E0, season, "file.csv").orElseThrow();
		assertThat(westHam.quotesSkippedInvalidLine()).isGreaterThanOrEqualTo(1);
		assertThat(westHam.quotes()).isEmpty();
	}

	@Test
	void twoDigitDatesAndMalformedGoals() {
		FootballDataCsvRow dated = parser.parse(fixture("older-schema.csv")).getFirst();
		MappedHistoricalMatch mapped = mapper.map(dated, FootballDataLeague.E0, season, "file.csv").orElseThrow();
		assertThat(mapped.match().matchDate()).isEqualTo(LocalDate.of(2023, 8, 16));
		assertThat(mapper.map(
				new FootballDataCsvRow(2, "E0", "12/08/2023", null, "A", "B", "1.5", "0", java.util.Map.of()),
				FootballDataLeague.E0,
				season,
				"file.csv")).isEmpty();
	}

	private static String fixture(String name) {
		try {
			return new ClassPathResource("historical/footballdata/" + name).getContentAsString(StandardCharsets.UTF_8);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
