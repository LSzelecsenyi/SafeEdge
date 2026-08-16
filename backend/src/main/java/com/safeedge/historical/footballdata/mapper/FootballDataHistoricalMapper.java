package com.safeedge.historical.footballdata.mapper;

import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalAhQuoteDraft;
import com.safeedge.historical.domain.HistoricalMatchDraft;
import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.domain.MappedHistoricalMatch;
import com.safeedge.historical.footballdata.dto.FootballDataCsvRow;
import com.safeedge.settlement.AsianHandicapLines;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public final class FootballDataHistoricalMapper {

	private static final Pattern INTEGER = Pattern.compile("\\d+");
	private static final DateTimeFormatter DATE_FOUR_DIGIT =
			DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
	private static final DateTimeFormatter DATE_TWO_DIGIT = new DateTimeFormatterBuilder()
			.appendPattern("dd/MM/")
			.appendValueReduced(ChronoField.YEAR, 2, 2, 1990)
			.toFormatter()
			.withResolverStyle(ResolverStyle.STRICT);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm");

	public Optional<MappedHistoricalMatch> map(
			FootballDataCsvRow row,
			FootballDataLeague expectedLeague,
			FootballSeason season,
			String sourceFile) {
		if (row == null) {
			return Optional.empty();
		}
		try {
			if (isBlank(row.div())
					|| isBlank(row.date())
					|| isBlank(row.homeTeam())
					|| isBlank(row.awayTeam())
					|| isBlank(row.fthg())
					|| isBlank(row.ftag())) {
				return Optional.empty();
			}
			FootballDataLeague rowLeague = FootballDataLeague.fromCode(row.div());
			if (rowLeague != expectedLeague) {
				return Optional.empty();
			}
			LocalDate matchDate = parseDate(row.date());
			if (matchDate == null) {
				return Optional.empty();
			}
			Integer homeGoals = parseGoals(row.fthg());
			Integer awayGoals = parseGoals(row.ftag());
			if (homeGoals == null || awayGoals == null) {
				return Optional.empty();
			}
			HistoricalMatchDraft match = new HistoricalMatchDraft(
					HistoricalSource.FOOTBALL_DATA_UK,
					rowLeague.code(),
					rowLeague.canonicalCompetition(),
					season,
					matchDate,
					parseTime(row.time()),
					null,
					row.homeTeam(),
					row.awayTeam(),
					homeGoals,
					awayGoals,
					sourceFile,
					row.sourceRowNumber());
			QuoteParseResult quotes = parseQuotes(row);
			return Optional.of(new MappedHistoricalMatch(
					match,
					quotes.quotes(),
					quotes.incomplete(),
					quotes.invalidOdds(),
					quotes.invalidLine()));
		}
		catch (RuntimeException ex) {
			return Optional.empty();
		}
	}

	private static QuoteParseResult parseQuotes(FootballDataCsvRow row) {
		List<HistoricalAhQuoteDraft> quotes = new ArrayList<>();
		int incomplete = 0;
		int invalidOdds = 0;
		int invalidLine = 0;
		for (FootballDataAhQuoteMapping mapping : FootballDataAhQuoteMapping.values()) {
			String rawLine = row.optional(mapping.lineColumn());
			String rawHomeOdds = row.optional(mapping.homeOddsColumn());
			String rawAwayOdds = row.optional(mapping.awayOddsColumn());
			if (rawLine == null && rawHomeOdds == null && rawAwayOdds == null) {
				continue;
			}
			if (rawLine == null || rawHomeOdds == null || rawAwayOdds == null) {
				incomplete++;
				continue;
			}
			BigDecimal line = parseDecimal(rawLine);
			if (line == null || !AsianHandicapLines.isSupportedIncrement(line)) {
				invalidLine++;
				continue;
			}
			BigDecimal homeOdds = parseDecimal(rawHomeOdds);
			BigDecimal awayOdds = parseDecimal(rawAwayOdds);
			if (homeOdds == null
					|| awayOdds == null
					|| homeOdds.compareTo(BigDecimal.ONE) <= 0
					|| awayOdds.compareTo(BigDecimal.ONE) <= 0) {
				invalidOdds++;
				continue;
			}
			quotes.add(new HistoricalAhQuoteDraft(
					HistoricalSource.FOOTBALL_DATA_UK,
					mapping.quoteSource(),
					line,
					homeOdds,
					awayOdds,
					null,
					HistoricalObservationType.PRE_MATCH_SNAPSHOT,
					mapping.lineColumn(),
					mapping.homeOddsColumn(),
					mapping.awayOddsColumn(),
					rawLine,
					rawHomeOdds,
					rawAwayOdds));
		}
		return new QuoteParseResult(quotes, incomplete, invalidOdds, invalidLine);
	}

	private static LocalDate parseDate(String raw) {
		try {
			if (raw.length() > 8) {
				return LocalDate.parse(raw, DATE_FOUR_DIGIT);
			}
			return LocalDate.parse(raw, DATE_TWO_DIGIT);
		}
		catch (DateTimeException ex) {
			return null;
		}
	}

	private static LocalTime parseTime(String raw) {
		if (raw == null) {
			return null;
		}
		try {
			return LocalTime.parse(raw, TIME);
		}
		catch (DateTimeException ex) {
			return null;
		}
	}

	private static Integer parseGoals(String raw) {
		if (!INTEGER.matcher(raw).matches()) {
			return null;
		}
		return Integer.parseInt(raw);
	}

	private static BigDecimal parseDecimal(String raw) {
		try {
			return new BigDecimal(raw);
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record QuoteParseResult(
			List<HistoricalAhQuoteDraft> quotes, int incomplete, int invalidOdds, int invalidLine) {
	}

}
