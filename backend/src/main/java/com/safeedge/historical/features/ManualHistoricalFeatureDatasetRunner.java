package com.safeedge.historical.features;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.service.HistoricalFeatureDatasetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("manual-historical-features")
class ManualHistoricalFeatureDatasetRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ManualHistoricalFeatureDatasetRunner.class);
	private static final int SAMPLE_ROWS = 5;

	private final HistoricalFeatureDatasetService datasetService;
	private final HistoricalFeaturesProperties properties;

	ManualHistoricalFeatureDatasetRunner(
			HistoricalFeatureDatasetService datasetService, HistoricalFeaturesProperties properties) {
		this.datasetService = datasetService;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		String competitionRaw = properties.competition();
		String fromRaw = properties.fromSeason();
		String toRaw = properties.toSeason();
		if (competitionRaw == null
				|| competitionRaw.isBlank()
				|| fromRaw == null
				|| fromRaw.isBlank()
				|| toRaw == null
				|| toRaw.isBlank()) {
			log.warn(
					"manual-historical-features profile is active but SAFEEDGE_HISTORICAL_FEATURES_COMPETITION / FROM_SEASON / TO_SEASON are not set; skipping");
			return;
		}
		CanonicalCompetition competition = CanonicalCompetition.valueOf(competitionRaw.trim());
		int fromSeason = Integer.parseInt(fromRaw.trim());
		int toSeason = Integer.parseInt(toRaw.trim());
		HistoricalFeatureDataset dataset = datasetService.buildDataset(competition, fromSeason, toSeason);
		log.info(
				"Historical feature dataset: competition={} from={} to={} totalRows={} fullLast5={} fullLast10={} missingTeamHistory={}",
				competition,
				fromSeason,
				toSeason,
				dataset.totalRows(),
				dataset.rowsWithFullLast5History(),
				dataset.rowsWithFullLast10History(),
				dataset.rowsWithMissingTeamHistory());
		int sample = Math.min(SAMPLE_ROWS, dataset.rows().size());
		for (int i = 0; i < sample; i++) {
			HistoricalModelRow row = dataset.rows().get(i);
			PreMatchFeatures features = row.features();
			log.info(
					"Sample row {}: {} {} vs {} target={}-{} homePlayed={} awayPlayed={} leagueMatches={} homeLast5GF={}",
					i + 1,
					row.matchDate(),
					row.homeTeam(),
					row.awayTeam(),
					row.target().homeGoals(),
					row.target().awayGoals(),
					features.homeTeamMatchesPlayed(),
					features.awayTeamMatchesPlayed(),
					features.leagueMatchesObserved(),
					features.homeLast5GoalsForPerMatch());
		}
	}
}
