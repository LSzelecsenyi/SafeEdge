package com.safeedge.historical.footballdata.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalAhQuoteDraft;
import com.safeedge.historical.domain.MappedHistoricalMatch;
import com.safeedge.historical.footballdata.dto.FootballDataCsvRow;
import com.safeedge.settlement.AsianHandicapLines;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FootballDataAhLineMappingTest {

	private final FootballDataHistoricalMapper mapper = new FootballDataHistoricalMapper();
	private final FootballSeason season = new FootballSeason(2023, 2024);

	@ParameterizedTest
	@CsvSource({
			"-2.00, -2, 2",
			"-1.25, -1.25, 1.25",
			"-0.75, -0.75, 0.75",
			"-0.25, -0.25, 0.25",
			"0, 0, 0",
			"0.25, 0.25, -0.25",
			"+0.75, 0.75, -0.75",
			"+1.25, 1.25, -1.25",
			"+1.50, 1.50, -1.50"
	})
	void sourceHomeLineIsCanonicalHomeLine(String source, String canonicalHome, String canonicalAway) {
		FootballDataCsvRow row = new FootballDataCsvRow(
				2,
				"E0",
				"12/08/2023",
				null,
				"Arsenal",
				"Chelsea",
				"1",
				"0",
				Map.of("B365AH", source, "B365AHH", "1.90", "B365AHA", "2.00"));
		MappedHistoricalMatch mapped = mapper.map(row, FootballDataLeague.E0, season, "2324/E0.csv").orElseThrow();
		HistoricalAhQuoteDraft quote = mapped.quotes().getFirst();
		assertThat(quote.homeHandicapLine()).isEqualByComparingTo(canonicalHome);
		assertThat(quote.awayHandicapLine()).isEqualByComparingTo(canonicalAway);
		assertThat(quote.awayHandicapLine()).isEqualByComparingTo(AsianHandicapLines.awayLine(quote.homeHandicapLine()));
		assertThat(quote.homeHandicapLine()).isEqualByComparingTo(new BigDecimal(source));
	}

}
