package com.safeedge.result.service;

import com.safeedge.event.repository.EventEntity;
import com.safeedge.event.repository.EventRepository;
import com.safeedge.result.domain.MatchResult;
import com.safeedge.result.domain.MatchResultIdentityException;
import com.safeedge.result.domain.MatchResultSaveKind;
import com.safeedge.result.domain.MatchResultSaveResult;
import com.safeedge.result.repository.MatchResultEntity;
import com.safeedge.result.repository.MatchResultRepository;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchResultPersistenceService {

	private static final Logger log = LoggerFactory.getLogger(MatchResultPersistenceService.class);

	private final EventRepository eventRepository;
	private final MatchResultRepository matchResultRepository;

	public MatchResultPersistenceService(
			EventRepository eventRepository, MatchResultRepository matchResultRepository) {
		this.eventRepository = eventRepository;
		this.matchResultRepository = matchResultRepository;
	}

	@Transactional
	public Optional<MatchResultSaveResult> saveIfEventKnown(MatchResult result, Instant observedAt) {
		Optional<EventEntity> event = eventRepository.findByProviderAndExternalEventId(
				result.provider(), result.externalEventId());
		if (event.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(upsert(event.get(), result, observedAt));
	}

	private MatchResultSaveResult upsert(EventEntity event, MatchResult result, Instant observedAt) {
		requireConsistentBetradarId(event, result);
		Optional<MatchResultEntity> existing = matchResultRepository.findByEvent_Id(event.getId());
		if (existing.isEmpty()) {
			MatchResultEntity entity = new MatchResultEntity();
			entity.setEvent(event);
			apply(entity, result, observedAt);
			entity.setCreatedAt(observedAt);
			entity.setUpdatedAt(observedAt);
			matchResultRepository.save(entity);
			return new MatchResultSaveResult(MatchResultSaveKind.INSERTED, event.getId());
		}
		MatchResultEntity entity = existing.get();
		boolean scoreChanged = entity.getHomeGoals() != result.finalScore().homeGoals()
				|| entity.getAwayGoals() != result.finalScore().awayGoals();
		if (scoreChanged) {
			log.warn(
					"Stored final score changed: provider={} eventId={} previous={}-{} new={}-{}",
					result.provider(),
					result.externalEventId(),
					entity.getHomeGoals(),
					entity.getAwayGoals(),
					result.finalScore().homeGoals(),
					result.finalScore().awayGoals());
		}
		apply(entity, result, observedAt);
		entity.setUpdatedAt(observedAt);
		matchResultRepository.save(entity);
		return new MatchResultSaveResult(
				scoreChanged ? MatchResultSaveKind.UPDATED : MatchResultSaveKind.UNCHANGED, event.getId());
	}

	private void apply(MatchResultEntity entity, MatchResult result, Instant observedAt) {
		entity.setProvider(result.provider());
		entity.setExternalEventId(result.externalEventId());
		entity.setBetradarId(result.betradarId());
		entity.setHomeGoals(result.finalScore().homeGoals());
		entity.setAwayGoals(result.finalScore().awayGoals());
		entity.setResultObservedAt(observedAt);
	}

	private void requireConsistentBetradarId(EventEntity event, MatchResult result) {
		if (event.getBetradarId() != null
				&& result.betradarId() != null
				&& !event.getBetradarId().equals(result.betradarId())) {
			log.warn(
					"betradarId conflict: provider={} eventId={} stored={} result={}",
					result.provider(),
					result.externalEventId(),
					event.getBetradarId(),
					result.betradarId());
			throw new MatchResultIdentityException(
					result.externalEventId(),
					"betradarId " + result.betradarId() + " does not match stored " + event.getBetradarId());
		}
	}

}
