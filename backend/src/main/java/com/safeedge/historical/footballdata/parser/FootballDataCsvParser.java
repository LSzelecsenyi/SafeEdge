package com.safeedge.historical.footballdata.parser;

import com.safeedge.historical.footballdata.dto.FootballDataCsvRow;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public final class FootballDataCsvParser {

	static final List<String> REQUIRED_HEADERS = List.of("Div", "Date", "HomeTeam", "AwayTeam", "FTHG", "FTAG");
	static final List<String> OPTIONAL_AH_HEADERS = List.of(
			"AHh",
			"B365AH",
			"B365AHH",
			"B365AHA",
			"PAH",
			"PAHH",
			"PAHA",
			"MaxAHH",
			"MaxAHA",
			"AvgAHH",
			"AvgAHA");
	static final Set<String> OPTIONAL_KNOWN_HEADERS;

	static {
		java.util.LinkedHashSet<String> headers = new java.util.LinkedHashSet<>();
		headers.add("Time");
		headers.addAll(OPTIONAL_AH_HEADERS);
		OPTIONAL_KNOWN_HEADERS = Set.copyOf(headers);
	}

	public List<FootballDataCsvRow> parse(String csv) {
		if (csv == null || csv.isBlank()) {
			throw new FootballDataParseException("CSV content is required");
		}
		String sanitized = stripBom(csv);
		CSVFormat format = CSVFormat.RFC4180.builder()
				.setHeader()
				.setSkipHeaderRecord(true)
				.setIgnoreEmptyLines(true)
				.setTrim(true)
				.setIgnoreSurroundingSpaces(true)
				.get();
		try (CSVParser parser = CSVParser.parse(new StringReader(sanitized), format)) {
			Map<String, Integer> headers = normalizeHeaders(parser.getHeaderMap());
			requireHeaders(headers);
			List<FootballDataCsvRow> rows = new ArrayList<>();
			for (CSVRecord record : parser) {
				rows.add(toRow(record, headers));
			}
			return List.copyOf(rows);
		}
		catch (FootballDataParseException ex) {
			throw ex;
		}
		catch (IOException | RuntimeException ex) {
			throw new FootballDataParseException("Unable to parse football-data.co.uk CSV", ex);
		}
	}

	private static FootballDataCsvRow toRow(CSVRecord record, Map<String, Integer> headers) {
		Map<String, String> optional = new LinkedHashMap<>();
		for (String header : OPTIONAL_KNOWN_HEADERS) {
			if (headers.containsKey(header)) {
				String value = cell(record, headers.get(header));
				if (value != null) {
					optional.put(header, value);
				}
			}
		}
		return new FootballDataCsvRow(
				Math.toIntExact(record.getRecordNumber()),
				value(record, headers, "Div"),
				value(record, headers, "Date"),
				optional.get("Time"),
				value(record, headers, "HomeTeam"),
				value(record, headers, "AwayTeam"),
				value(record, headers, "FTHG"),
				value(record, headers, "FTAG"),
				optional);
	}

	private static String value(CSVRecord record, Map<String, Integer> headers, String header) {
		return cell(record, headers.get(header));
	}

	/**
	 * Source files (and test fixtures) may omit trailing optional columns.
	 * Missing cells are unavailable, not a parse failure of the whole file.
	 */
	private static String cell(CSVRecord record, Integer index) {
		if (index == null || index < 0 || index >= record.size()) {
			return null;
		}
		return blankToNull(record.get(index));
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}

	private static void requireHeaders(Map<String, Integer> headers) {
		for (String required : REQUIRED_HEADERS) {
			if (!headers.containsKey(required)) {
				throw new FootballDataParseException("Required CSV header missing: " + required);
			}
		}
	}

	private static Map<String, Integer> normalizeHeaders(Map<String, Integer> headerMap) {
		if (headerMap == null || headerMap.isEmpty()) {
			throw new FootballDataParseException("CSV header row is required");
		}
		Map<String, Integer> normalized = new LinkedHashMap<>();
		headerMap.forEach((name, index) -> {
			if (name != null && !name.isBlank()) {
				normalized.put(stripBom(name).trim(), index);
			}
		});
		return normalized;
	}

	private static String stripBom(String value) {
		if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
			return value.substring(1);
		}
		return value;
	}

}
