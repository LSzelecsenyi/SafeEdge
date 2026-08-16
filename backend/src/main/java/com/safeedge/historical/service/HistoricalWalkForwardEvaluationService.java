package com.safeedge.historical.service;

import com.safeedge.candidate.CandidateEngine;
import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.evaluation.HistoricalAhQuoteSnapshot;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;
import com.safeedge.historical.evaluation.HistoricalWalkForwardDataset;
import com.safeedge.historical.evaluation.HistoricalWalkForwardDatasetBuilder;
import com.safeedge.historical.evaluation.HistoricalWalkForwardIdentities;
import com.safeedge.historical.evaluation.WalkForwardEvaluationRequest;
import com.safeedge.historical.features.HistoricalMatchRecord;
import com.safeedge.historical.repository.HistoricalAhOfferEntity;
import com.safeedge.historical.repository.HistoricalAhOfferRepository;
import com.safeedge.historical.repository.HistoricalMatchEntity;
import com.safeedge.historical.repository.HistoricalMatchRepository;
import com.safeedge.probability.FootballProbabilityModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads persisted historical matches and the configured AH quote source, then
 * delegates walk-forward candidate construction to
 * {@link HistoricalWalkForwardDatasetBuilder}.
 */
@Service
public class HistoricalWalkForwardEvaluationService {

	private final HistoricalMatchRepository matchRepository;
	private final HistoricalAhOfferRepository offerRepository;
	private final HistoricalWalkForwardDatasetBuilder datasetBuilder = new HistoricalWalkForwardDatasetBuilder();

	public HistoricalWalkForwardEvaluationService(
			HistoricalMatchRepository matchRepository, HistoricalAhOfferRepository offerRepository) {
		this.matchRepository = matchRepository;
		this.offerRepository = offerRepository;
	}

	@Transactional(readOnly = true)
	public HistoricalWalkForwardDataset buildDataset(WalkForwardEvaluationRequest request) {
		return buildWithPredictions(request).dataset();
	}

	@Transactional(readOnly = true)
	public HistoricalWalkForwardBuildOutput buildWithPredictions(WalkForwardEvaluationRequest request) {
		return buildWithPredictions(request, datasetBuilder);
	}

	/**
	 * Same persisted walk-forward load as {@link #buildWithPredictions(WalkForwardEvaluationRequest)},
	 * using an explicit {@link FootballProbabilityModel}. Model choice belongs here, not in
	 * CandidateEngine or BacktestEngine. The no-model overload remains Probability Model v1.
	 */
	@Transactional(readOnly = true)
	public HistoricalWalkForwardBuildOutput buildWithPredictions(
			WalkForwardEvaluationRequest request, FootballProbabilityModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model is required");
		}
		return buildWithPredictions(request, new HistoricalWalkForwardDatasetBuilder(model, new CandidateEngine()));
	}

	private HistoricalWalkForwardBuildOutput buildWithPredictions(
			WalkForwardEvaluationRequest request, HistoricalWalkForwardDatasetBuilder builder) {
		if (request == null) {
			throw new IllegalArgumentException("request is required");
		}
		List<HistoricalMatchEntity> entities = matchRepository
				.findBySourceAndCanonicalCompetitionAndSeasonStartYearBetweenOrderByMatchDateAscSourceRowNumberAscIdAsc(
						HistoricalSource.FOOTBALL_DATA_UK,
						request.competition(),
						request.trainingFromSeason(),
						request.evaluationToSeason());
		List<HistoricalMatchRecord> records = new ArrayList<>(entities.size());
		Map<Long, HistoricalMatchRecord> recordsById = new HashMap<>();
		for (HistoricalMatchEntity entity : entities) {
			HistoricalMatchRecord record = HistoricalMatchRecordMapper.fromEntity(entity);
			records.add(record);
			recordsById.put(entity.getId(), record);
		}
		Map<String, HistoricalAhQuoteSnapshot> quotes = loadSelectedQuotes(request.quoteSource(), entities, recordsById);
		return builder.buildWithPredictions(records, quotes, request);
	}

	private Map<String, HistoricalAhQuoteSnapshot> loadSelectedQuotes(
			HistoricalQuoteSource quoteSource,
			List<HistoricalMatchEntity> matches,
			Map<Long, HistoricalMatchRecord> recordsById) {
		if (matches.isEmpty()) {
			return Map.of();
		}
		List<Long> matchIds = new ArrayList<>(matches.size());
		for (HistoricalMatchEntity match : matches) {
			matchIds.add(match.getId());
		}
		List<HistoricalAhOfferEntity> offers =
				offerRepository.findByQuoteSourceAndObservationTypeAndHistoricalMatch_IdIn(
						quoteSource, HistoricalObservationType.PRE_MATCH_SNAPSHOT, matchIds);
		Map<String, HistoricalAhQuoteSnapshot> quotes = new HashMap<>();
		for (HistoricalAhOfferEntity offer : offers) {
			HistoricalMatchRecord record = recordsById.get(offer.getHistoricalMatch().getId());
			if (record == null) {
				continue;
			}
			String eventId = HistoricalWalkForwardIdentities.eventId(record);
			quotes.put(
					eventId,
					new HistoricalAhQuoteSnapshot(
							eventId,
							offer.getQuoteSource(),
							offer.getHomeHandicapLine(),
							offer.getHomeOdds(),
							offer.getAwayOdds()));
		}
		return quotes;
	}
}
