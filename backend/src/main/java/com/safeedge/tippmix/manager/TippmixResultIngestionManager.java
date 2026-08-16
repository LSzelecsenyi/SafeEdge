package com.safeedge.tippmix.manager;

import com.safeedge.result.domain.MatchResult;
import com.safeedge.result.domain.MatchResultIdentityException;
import com.safeedge.result.domain.MatchResultSaveKind;
import com.safeedge.result.domain.MatchResultSaveResult;
import com.safeedge.result.service.MatchResultPersistenceService;
import com.safeedge.tippmix.client.TippmixResultClient;
import com.safeedge.tippmix.dto.TippmixResultCompetitionDto;
import com.safeedge.tippmix.dto.TippmixResultDateDto;
import com.safeedge.tippmix.dto.TippmixResultEventDto;
import com.safeedge.tippmix.dto.TippmixResultRequest;
import com.safeedge.tippmix.dto.TippmixResultResponse;
import com.safeedge.tippmix.mapper.TippmixBettingOfferNormalizer;
import com.safeedge.tippmix.mapper.TippmixResultNormalizer;
import com.safeedge.tippmix.mapper.TippmixResultNormalizationException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TippmixResultIngestionManager {

	private static final Logger log = LoggerFactory.getLogger(TippmixResultIngestionManager.class);

	private final TippmixResultClient resultClient;
	private final TippmixResultNormalizer normalizer;
	private final MatchResultPersistenceService persistenceService;
	private final Clock clock;
	private final AtomicBoolean inProgress = new AtomicBoolean(false);

	public TippmixResultIngestionManager(
			TippmixResultClient resultClient,
			TippmixResultNormalizer normalizer,
			MatchResultPersistenceService persistenceService,
			Clock clock) {
		this.resultClient = resultClient;
		this.normalizer = normalizer;
		this.persistenceService = persistenceService;
		this.clock = clock;
	}

	public ResultCollectionRunResult collectResults() {
		if (!inProgress.compareAndSet(false, true)) {
			Instant now = clock.instant();
			log.warn("Tippmix result collection already in progress; skipping this trigger");
			return new ResultCollectionRunResult(0, 0, 0, 0, 0, 0, 0, 0, now, List.of());
		}
		try {
			return run();
		}
		finally {
			inProgress.set(false);
		}
	}

	private ResultCollectionRunResult run() {
		Instant observedAt = clock.instant();
		TippmixResultResponse response = resultClient.fetchResults(TippmixResultRequest.verifiedFootballResults());
		int received = 0;
		int finished = 0;
		int known = 0;
		int inserted = 0;
		int updated = 0;
		int unchanged = 0;
		int skipped = 0;
		int failed = 0;
		List<ResultCollectionRunResult.EventFailure> failures = new ArrayList<>();

		for (TippmixResultEventDto event : flatten(response)) {
			received++;
			Long eventId = event == null ? null : event.eventId();
			try {
				if (!isFinishedFootball(event)) {
					skipped++;
					continue;
				}
				finished++;
				Optional<MatchResult> normalized = normalizer.normalize(event);
				if (normalized.isEmpty()) {
					skipped++;
					continue;
				}
				Optional<MatchResultSaveResult> saved =
						persistenceService.saveIfEventKnown(normalized.get(), observedAt);
				if (saved.isEmpty()) {
					skipped++;
					continue;
				}
				known++;
				switch (saved.get().kind()) {
					case INSERTED -> inserted++;
					case UPDATED -> updated++;
					case UNCHANGED -> unchanged++;
				}
			}
			catch (TippmixResultNormalizationException | MatchResultIdentityException ex) {
				failed++;
				failures.add(new ResultCollectionRunResult.EventFailure(eventId, ex.getMessage()));
				log.warn(
						"Tippmix result event failed: provider=Tippmix eventId={} reason={}",
						eventId,
						ex.getMessage());
			}
			catch (RuntimeException ex) {
				failed++;
				failures.add(new ResultCollectionRunResult.EventFailure(eventId, ex.getMessage()));
				log.error(
						"Tippmix result event failed: provider=Tippmix eventId={} reason={}",
						eventId,
						ex.getMessage());
			}
		}

		ResultCollectionRunResult result = new ResultCollectionRunResult(
				received,
				finished,
				known,
				inserted,
				updated,
				unchanged,
				skipped,
				failed,
				observedAt,
				List.copyOf(failures));
		log.info(
				"Tippmix result collection completed: received={} finished={} known={} inserted={} updated={} unchanged={} skipped={} failed={}",
				result.eventsReceived(),
				result.finishedEvents(),
				result.knownEvents(),
				result.resultsInserted(),
				result.resultsUpdated(),
				result.resultsUnchanged(),
				result.eventsSkipped(),
				result.eventsFailed());
		return result;
	}

	private static boolean isFinishedFootball(TippmixResultEventDto event) {
		return event != null
				&& Integer.valueOf(TippmixBettingOfferNormalizer.FOOTBALL_SPORT_ID).equals(event.sportId())
				&& TippmixResultNormalizer.ENDED_STATUS.equals(event.matchStatus());
	}

	private static List<TippmixResultEventDto> flatten(TippmixResultResponse response) {
		List<TippmixResultEventDto> events = new ArrayList<>();
		if (response == null || response.data() == null) {
			return events;
		}
		for (TippmixResultDateDto date : response.data()) {
			if (date == null || date.sportCompetitions() == null) {
				continue;
			}
			for (TippmixResultCompetitionDto competition : date.sportCompetitions()) {
				if (competition == null || competition.events() == null) {
					continue;
				}
				events.addAll(competition.events());
			}
		}
		return events;
	}

}
