package com.safeedge.historical.footballdata.importer;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalImportResult;
import com.safeedge.historical.footballdata.config.FootballDataProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-historical-import")
class ManualFootballDataHistoricalImportRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualFootballDataHistoricalImportRunner.class);

	private final FootballDataHistoricalImportManager importManager;
	private final FootballDataProperties properties;

	ManualFootballDataHistoricalImportRunner(
			FootballDataHistoricalImportManager importManager, FootballDataProperties properties) {
		this.importManager = importManager;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		String league = properties.manualLeague();
		String startYearRaw = properties.manualSeasonStart();
		if (league == null || league.isBlank() || startYearRaw == null || startYearRaw.isBlank()) {
			log.warn(
					"manual-historical-import profile is active but SAFEEDGE_HISTORICAL_LEAGUE / SAFEEDGE_HISTORICAL_SEASON_START are not set; skipping");
			return;
		}
		CanonicalCompetition competition = CanonicalCompetition.valueOf(league.trim());
		int startYear = Integer.parseInt(startYearRaw.trim());
		FootballSeason season = new FootballSeason(startYear, startYear + 1);
		log.info("Starting manual football-data.co.uk import for {} {}", competition, season.displayValue());
		HistoricalImportResult result = importManager.importSeason(competition, season);
		log.info(
				"Manual historical import finished: matchesInserted={} quotesInserted={} rowsRejected={}",
				result.matchesInserted(),
				result.quotesInserted(),
				result.rowsRejected());
	}

}
