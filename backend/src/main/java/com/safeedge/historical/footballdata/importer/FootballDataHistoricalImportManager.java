package com.safeedge.historical.footballdata.importer;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalImportResult;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.domain.MappedHistoricalMatch;
import com.safeedge.historical.footballdata.client.FootballDataClient;
import com.safeedge.historical.footballdata.client.FootballDataPaths;
import com.safeedge.historical.footballdata.dto.FootballDataCsvRow;
import com.safeedge.historical.footballdata.mapper.FootballDataHistoricalMapper;
import com.safeedge.historical.footballdata.mapper.FootballDataLeague;
import com.safeedge.historical.footballdata.parser.FootballDataCsvParser;
import com.safeedge.historical.service.HistoricalFootballDataImportService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FootballDataHistoricalImportManager {

	private static final Logger log = LoggerFactory.getLogger(FootballDataHistoricalImportManager.class);

	private final FootballDataClient client;
	private final FootballDataCsvParser parser;
	private final FootballDataHistoricalMapper mapper;
	private final HistoricalFootballDataImportService importService;
	private final Clock clock;

	public FootballDataHistoricalImportManager(
			FootballDataClient client,
			FootballDataCsvParser parser,
			FootballDataHistoricalMapper mapper,
			HistoricalFootballDataImportService importService,
			Clock clock) {
		this.client = client;
		this.parser = parser;
		this.mapper = mapper;
		this.importService = importService;
		this.clock = clock;
	}

	public HistoricalImportResult importSeason(CanonicalCompetition competition, FootballSeason season) {
		FootballDataLeague league = FootballDataLeague.fromCanonical(competition);
		String sourceFile = FootballDataPaths.csvPath(league, season).substring(1);
		String csv = client.fetchSeason(league, season);
		return importCsv(competition, season, csv, sourceFile);
	}

	public HistoricalImportResult importCsv(
			CanonicalCompetition competition, FootballSeason season, String csv, String sourceFile) {
		FootballDataLeague league = FootballDataLeague.fromCanonical(competition);
		List<FootballDataCsvRow> rows = parser.parse(csv);
		List<MappedHistoricalMatch> accepted = new ArrayList<>();
		int rejected = 0;
		for (FootballDataCsvRow row : rows) {
			Optional<MappedHistoricalMatch> mapped = mapper.map(row, league, season, sourceFile);
			if (mapped.isPresent()) {
				accepted.add(mapped.get());
			}
			else {
				rejected++;
			}
		}
		HistoricalFootballDataImportService.PersistCounts persisted =
				importService.persist(accepted, clock.instant());
		HistoricalImportResult result = HistoricalFootballDataImportService.toResult(
				HistoricalSource.FOOTBALL_DATA_UK,
				competition,
				season,
				sourceFile,
				rows.size(),
				rejected,
				persisted);
		log.info(
				"football-data.co.uk import complete: league={} season={} file={} rowsRead={} inserted={} updated={} quotesInserted={} rejected={}",
				competition,
				season.displayValue(),
				sourceFile,
				result.rowsRead(),
				result.matchesInserted(),
				result.matchesUpdated(),
				result.quotesInserted(),
				result.rowsRejected());
		return result;
	}

}
