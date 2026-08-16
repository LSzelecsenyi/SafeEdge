package com.safeedge.historical.service;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.features.HistoricalFeatureBuilder;
import com.safeedge.historical.features.HistoricalFeatureDataset;
import com.safeedge.historical.features.HistoricalMatchRecord;
import com.safeedge.historical.repository.HistoricalMatchEntity;
import com.safeedge.historical.repository.HistoricalMatchRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads historical match facts and delegates feature calculation to
 * {@link HistoricalFeatureBuilder}. No feature math lives here.
 */
@Service
public class HistoricalFeatureDatasetService {

	private final HistoricalMatchRepository matchRepository;
	private final HistoricalFeatureBuilder featureBuilder = new HistoricalFeatureBuilder();

	public HistoricalFeatureDatasetService(HistoricalMatchRepository matchRepository) {
		this.matchRepository = matchRepository;
	}

	@Transactional(readOnly = true)
	public HistoricalFeatureDataset buildDataset(CanonicalCompetition competition, int fromSeason, int toSeason) {
		return buildDataset(HistoricalSource.FOOTBALL_DATA_UK, competition, fromSeason, toSeason);
	}

	@Transactional(readOnly = true)
	public HistoricalFeatureDataset buildDataset(
			HistoricalSource source, CanonicalCompetition competition, int fromSeason, int toSeason) {
		if (source == null) {
			throw new HistoricalDataException("source is required");
		}
		if (competition == null) {
			throw new HistoricalDataException("competition is required");
		}
		if (fromSeason <= 0 || toSeason < fromSeason) {
			throw new HistoricalDataException(
					"Season range is invalid: fromSeason=" + fromSeason + " toSeason=" + toSeason);
		}
		List<HistoricalMatchEntity> entities = matchRepository
				.findBySourceAndCanonicalCompetitionAndSeasonStartYearBetweenOrderByMatchDateAscSourceRowNumberAscIdAsc(
						source, competition, fromSeason, toSeason);
		List<HistoricalMatchRecord> records = new ArrayList<>(entities.size());
		for (HistoricalMatchEntity entity : entities) {
			records.add(HistoricalMatchRecordMapper.fromEntity(entity));
		}
		return featureBuilder.build(records);
	}
}
