package com.safeedge.historical.footballdata.importer;

import com.safeedge.historical.domain.BulkHistoricalImportResult;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.domain.HistoricalImportFailure;
import com.safeedge.historical.domain.HistoricalImportFailureStage;
import com.safeedge.historical.domain.HistoricalImportResult;
import com.safeedge.historical.footballdata.client.FootballDataClientException;
import com.safeedge.historical.footballdata.parser.FootballDataParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sequential football-data.co.uk bulk import. Calls
 * {@link FootballDataHistoricalImportManager} once per league-season; does not
 * fetch, parse, or persist itself.
 */
@Component
public class FootballDataBulkHistoricalImportManager {

	private static final Logger log = LoggerFactory.getLogger(FootballDataBulkHistoricalImportManager.class);

	private final FootballDataHistoricalImportManager importManager;
	private final Clock clock;

	public FootballDataBulkHistoricalImportManager(
			FootballDataHistoricalImportManager importManager, Clock clock) {
		this.importManager = importManager;
		this.clock = clock;
	}

	public BulkHistoricalImportResult importRange(
			Set<CanonicalCompetition> competitions, int startSeason, int endSeason) {
		if (competitions == null || competitions.isEmpty()) {
			throw new HistoricalDataException("At least one canonical competition is required");
		}
		if (startSeason <= 0 || endSeason < startSeason) {
			throw new HistoricalDataException(
					"Season range is invalid: startSeason=" + startSeason + " endSeason=" + endSeason);
		}
		List<CanonicalCompetition> leagues = competitions.stream()
				.sorted(Comparator.naturalOrder())
				.toList();
		List<FootballSeason> seasons = seasonsInRange(startSeason, endSeason);
		Instant startedAt = clock.instant();
		int rowsRead = 0;
		int matchesInserted = 0;
		int matchesUpdated = 0;
		int quotesInserted = 0;
		int quotesUpdated = 0;
		int rowsRejected = 0;
		int quotesSkippedIncomplete = 0;
		int quotesSkippedInvalidOdds = 0;
		int quotesSkippedInvalidLine = 0;
		int succeeded = 0;
		List<HistoricalImportFailure> failures = new ArrayList<>();
		int requested = leagues.size() * seasons.size();
		for (CanonicalCompetition competition : leagues) {
			for (FootballSeason season : seasons) {
				try {
					HistoricalImportResult result = importManager.importSeason(competition, season);
					succeeded++;
					rowsRead += result.rowsRead();
					matchesInserted += result.matchesInserted();
					matchesUpdated += result.matchesUpdated();
					quotesInserted += result.quotesInserted();
					quotesUpdated += result.quotesUpdated();
					rowsRejected += result.rowsRejected();
					quotesSkippedIncomplete += result.quotesSkippedIncomplete();
					quotesSkippedInvalidOdds += result.quotesSkippedInvalidOdds();
					quotesSkippedInvalidLine += result.quotesSkippedInvalidLine();
					log.debug(
							"Historical bulk season imported: league={} season={}",
							competition,
							season.displayValue());
				}
				catch (RuntimeException ex) {
					HistoricalImportFailureStage stage = stageOf(ex);
					String message = ex.getMessage() == null || ex.getMessage().isBlank()
							? ex.getClass().getSimpleName()
							: ex.getMessage();
					failures.add(new HistoricalImportFailure(competition, season, stage, message));
					log.warn(
							"Historical bulk season failed: league={} season={} stage={} reason={}",
							competition,
							season.displayValue(),
							stage,
							message,
							ex);
				}
			}
		}
		Instant completedAt = clock.instant();
		BulkHistoricalImportResult result = new BulkHistoricalImportResult(
				startedAt,
				completedAt,
				requested,
				succeeded,
				failures.size(),
				rowsRead,
				matchesInserted,
				matchesUpdated,
				quotesInserted,
				quotesUpdated,
				rowsRejected,
				quotesSkippedIncomplete,
				quotesSkippedInvalidOdds,
				quotesSkippedInvalidLine,
				failures);
		log.info(
				"Historical bulk import completed: requested={} succeeded={} failed={} matchesInserted={} matchesUpdated={} quotesInserted={} quotesUpdated={} duration={}",
				result.leagueSeasonPairsRequested(),
				result.leagueSeasonPairsSucceeded(),
				result.leagueSeasonPairsFailed(),
				result.matchesInserted(),
				result.matchesUpdated(),
				result.quotesInserted(),
				result.quotesUpdated(),
				Duration.between(startedAt, completedAt));
		return result;
	}

	private static List<FootballSeason> seasonsInRange(int startSeason, int endSeason) {
		List<FootballSeason> seasons = new ArrayList<>();
		for (int year = startSeason; year <= endSeason; year++) {
			seasons.add(new FootballSeason(year, year + 1));
		}
		return List.copyOf(seasons);
	}

	private static HistoricalImportFailureStage stageOf(RuntimeException ex) {
		if (ex instanceof FootballDataClientException clientEx) {
			if (clientEx.failureType() == FootballDataClientException.FailureType.NOT_FOUND) {
				return HistoricalImportFailureStage.SOURCE_NOT_FOUND;
			}
			return HistoricalImportFailureStage.FETCH_FAILED;
		}
		if (ex instanceof FootballDataParseException) {
			return HistoricalImportFailureStage.PARSE_FAILED;
		}
		return HistoricalImportFailureStage.IMPORT_FAILED;
	}
}
