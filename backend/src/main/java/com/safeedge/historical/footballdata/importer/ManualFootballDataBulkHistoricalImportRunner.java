package com.safeedge.historical.footballdata.importer;

import com.safeedge.historical.domain.BulkHistoricalImportResult;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalAhCoverageReport;
import com.safeedge.historical.footballdata.config.FootballDataProperties;
import com.safeedge.historical.service.HistoricalAhCoverageReportFormatter;
import com.safeedge.historical.service.HistoricalAhCoverageService;
import java.util.EnumSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-historical-bulk-import")
class ManualFootballDataBulkHistoricalImportRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualFootballDataBulkHistoricalImportRunner.class);

	private final FootballDataBulkHistoricalImportManager bulkImportManager;
	private final HistoricalAhCoverageService coverageService;
	private final FootballDataProperties properties;

	ManualFootballDataBulkHistoricalImportRunner(
			FootballDataBulkHistoricalImportManager bulkImportManager,
			HistoricalAhCoverageService coverageService,
			FootballDataProperties properties) {
		this.bulkImportManager = bulkImportManager;
		this.coverageService = coverageService;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		String startRaw = properties.bulkStartSeason();
		String endRaw = properties.bulkEndSeason();
		if (startRaw == null || startRaw.isBlank() || endRaw == null || endRaw.isBlank()) {
			log.warn(
					"manual-historical-bulk-import profile is active but SAFEEDGE_HISTORICAL_BULK_START_SEASON / SAFEEDGE_HISTORICAL_BULK_END_SEASON are not set; skipping");
			return;
		}
		int startSeason = Integer.parseInt(startRaw.trim());
		int endSeason = Integer.parseInt(endRaw.trim());
		Set<CanonicalCompetition> leagues = parseLeagues(properties.bulkLeagues());
		log.info(
				"Starting sequential football-data.co.uk bulk import: leagues={} startSeason={} endSeason={}",
				leagues,
				startSeason,
				endSeason);
		BulkHistoricalImportResult result = bulkImportManager.importRange(leagues, startSeason, endSeason);
		log.info(
				"Historical bulk import finished: requested={} succeeded={} failed={} matchesInserted={} quotesInserted={}",
				result.leagueSeasonPairsRequested(),
				result.leagueSeasonPairsSucceeded(),
				result.leagueSeasonPairsFailed(),
				result.matchesInserted(),
				result.quotesInserted());
		HistoricalAhCoverageReport coverage = coverageService.report();
		log.info("Historical AH coverage after bulk import:\n{}", HistoricalAhCoverageReportFormatter.format(coverage));
	}

	static Set<CanonicalCompetition> parseLeagues(String raw) {
		if (raw == null || raw.isBlank()) {
			return EnumSet.allOf(CanonicalCompetition.class);
		}
		EnumSet<CanonicalCompetition> selected = EnumSet.noneOf(CanonicalCompetition.class);
		for (String part : raw.split(",")) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				selected.add(CanonicalCompetition.valueOf(trimmed));
			}
		}
		if (selected.isEmpty()) {
			return EnumSet.allOf(CanonicalCompetition.class);
		}
		return selected;
	}
}
