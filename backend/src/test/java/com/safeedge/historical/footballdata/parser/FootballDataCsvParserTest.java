package com.safeedge.historical.footballdata.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.historical.footballdata.dto.FootballDataCsvRow;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FootballDataCsvParserTest {

	private final FootballDataCsvParser parser = new FootballDataCsvParser();

	@Test
	void parsesByHeaderNameIncludingQuotedAndOptionalFields() {
		List<FootballDataCsvRow> rows = parser.parse(fixture("sample-e0.csv"));
		assertThat(rows).hasSize(8);
		FootballDataCsvRow united = rows.get(1);
		assertThat(united.homeTeam()).isEqualTo("Man United");
		assertThat(united.time()).isEqualTo("17:30");
		assertThat(united.optional("B365AH")).isEqualTo("-0.25");
		assertThat(rows.get(4).time()).isNull();
		assertThat(rows.get(4).optional("AHh")).isNull();
	}

	@Test
	void stripsBomAndAcceptsReorderedAndOlderSchemas() {
		List<FootballDataCsvRow> bom = parser.parse(
				"\uFEFFDiv,Date,HomeTeam,AwayTeam,FTHG,FTAG\nE0,12/08/2023,Arsenal,Chelsea,1,0\n");
		assertThat(bom.getFirst().div()).isEqualTo("E0");
		assertThat(bom.getFirst().homeTeam()).isEqualTo("Arsenal");
		List<FootballDataCsvRow> reordered = parser.parse(fixture("reordered.csv"));
		assertThat(reordered.getFirst().homeTeam()).isEqualTo("Arsenal");
		assertThat(reordered.getFirst().fthg()).isEqualTo("2");
		List<FootballDataCsvRow> older = parser.parse(fixture("older-schema.csv"));
		assertThat(older.getFirst().time()).isNull();
		assertThat(older.getFirst().optional("AHh")).isNull();
		assertThat(older.getFirst().date()).isEqualTo("16/08/23");
	}

	@Test
	void extraUnknownColumnsDoNotBreakParsing() {
		List<FootballDataCsvRow> rows = parser.parse(
				"Div,Date,HomeTeam,AwayTeam,FTHG,FTAG,HS,B365H\nE0,12/08/2023,Arsenal,Chelsea,2,1,14,1.70\n");
		assertThat(rows.getFirst().fthg()).isEqualTo("2");
		assertThat(rows.getFirst().optional("B365H")).isNull();
	}

	@Test
	void missingRequiredHeaderFailsTheFile() {
		assertThatThrownBy(() -> parser.parse("Div,Date,HomeTeam,AwayTeam,FTHG\nE0,12/08/2023,Arsenal,Chelsea,1\n"))
				.isInstanceOf(FootballDataParseException.class)
				.hasMessageContaining("FTAG");
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
