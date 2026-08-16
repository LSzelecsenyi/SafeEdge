package com.safeedge.result.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.PostgresTestcontainersConfig;
import com.safeedge.event.domain.BettingOffer;
import com.safeedge.event.domain.BettingProviders;
import com.safeedge.event.service.BettingOfferPersistenceService;
import com.safeedge.result.domain.MatchResult;
import com.safeedge.result.domain.MatchResultIdentityException;
import com.safeedge.result.domain.MatchResultSaveKind;
import com.safeedge.result.domain.MatchResultSaveResult;
import com.safeedge.result.repository.MatchResultEntity;
import com.safeedge.result.repository.MatchResultRepository;
import com.safeedge.settlement.MatchScore;
import com.safeedge.tippmix.TippmixOfferFixtures;
import com.safeedge.tippmix.mapper.TippmixBettingOfferNormalizer;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
@Transactional
class MatchResultPersistenceServiceTest {

	private static final Instant FIRST = Instant.parse("2026-08-16T18:00:00Z");
	private static final Instant SECOND = Instant.parse("2026-08-16T19:00:00Z");

	@Autowired
	private MatchResultPersistenceService persistenceService;

	@Autowired
	private BettingOfferPersistenceService offerPersistenceService;

	@Autowired
	private MatchResultRepository matchResultRepository;

	private final TippmixBettingOfferNormalizer offerNormalizer = new TippmixBettingOfferNormalizer();

	@Test
	void saveIfEventKnown_insertsResultForKnownEvent() {
		Long eventId = persistKnownEvent();

		Optional<MatchResultSaveResult> saved = persistenceService.saveIfEventKnown(matchResult(2, 1), FIRST);

		assertThat(saved).isPresent();
		assertThat(saved.get().kind()).isEqualTo(MatchResultSaveKind.INSERTED);
		assertThat(saved.get().eventId()).isEqualTo(eventId);
		assertThat(matchResultRepository.count()).isEqualTo(1);
		MatchResultEntity entity = matchResultRepository.findByEvent_Id(eventId).orElseThrow();
		assertThat(entity.getProvider()).isEqualTo(BettingProviders.TIPPMIX);
		assertThat(entity.getExternalEventId()).isEqualTo("5311343");
		assertThat(entity.getBetradarId()).isEqualTo(72409632L);
		assertThat(entity.getHomeGoals()).isEqualTo(2);
		assertThat(entity.getAwayGoals()).isEqualTo(1);
		assertThat(entity.getResultObservedAt()).isEqualTo(FIRST);
		assertThat(entity.getCreatedAt()).isEqualTo(FIRST);
	}

	@Test
	void saveIfEventKnown_sameScoreDoesNotDuplicate() {
		Long eventId = persistKnownEvent();
		persistenceService.saveIfEventKnown(matchResult(2, 1), FIRST);

		Optional<MatchResultSaveResult> second = persistenceService.saveIfEventKnown(matchResult(2, 1), SECOND);

		assertThat(second).isPresent();
		assertThat(second.get().kind()).isEqualTo(MatchResultSaveKind.UNCHANGED);
		assertThat(matchResultRepository.count()).isEqualTo(1);
		MatchResultEntity entity = matchResultRepository.findByEvent_Id(eventId).orElseThrow();
		assertThat(entity.getHomeGoals()).isEqualTo(2);
		assertThat(entity.getAwayGoals()).isEqualTo(1);
		assertThat(entity.getCreatedAt()).isEqualTo(FIRST);
		assertThat(entity.getUpdatedAt()).isEqualTo(SECOND);
		assertThat(entity.getResultObservedAt()).isEqualTo(SECOND);
	}

	@Test
	void saveIfEventKnown_changedScoreUpdatesSameRow() {
		Long eventId = persistKnownEvent();
		persistenceService.saveIfEventKnown(matchResult(2, 1), FIRST);

		Optional<MatchResultSaveResult> second = persistenceService.saveIfEventKnown(matchResult(3, 1), SECOND);

		assertThat(second).isPresent();
		assertThat(second.get().kind()).isEqualTo(MatchResultSaveKind.UPDATED);
		assertThat(matchResultRepository.count()).isEqualTo(1);
		MatchResultEntity entity = matchResultRepository.findByEvent_Id(eventId).orElseThrow();
		assertThat(entity.getHomeGoals()).isEqualTo(3);
		assertThat(entity.getAwayGoals()).isEqualTo(1);
		assertThat(entity.getCreatedAt()).isEqualTo(FIRST);
		assertThat(entity.getUpdatedAt()).isEqualTo(SECOND);
	}

	@Test
	void saveIfEventKnown_unknownEventIsNotInserted() {
		Optional<MatchResultSaveResult> saved = persistenceService.saveIfEventKnown(matchResult(2, 1), FIRST);

		assertThat(saved).isEmpty();
		assertThat(matchResultRepository.count()).isZero();
	}

	@Test
	void saveIfEventKnown_betradarMismatchIsRejected() {
		persistKnownEvent();
		MatchResult conflicting = new MatchResult(
				BettingProviders.TIPPMIX,
				"5311343",
				999L,
				new MatchScore(2, 1),
				TippmixOfferFixtures.KICKOFF.toInstant());

		assertThatThrownBy(() -> persistenceService.saveIfEventKnown(conflicting, FIRST))
				.isInstanceOf(MatchResultIdentityException.class)
				.hasMessageContaining("betradarId");
		assertThat(matchResultRepository.count()).isZero();
	}

	private Long persistKnownEvent() {
		BettingOffer offer = offerNormalizer.normalize(TippmixOfferFixtures.prematchFootball(
				TippmixOfferFixtures.asianHandicap("-1", "Djurgarden -1", "AIK Stockholm +1")));
		return offerPersistenceService.saveObservation(offer, FIRST).eventId();
	}

	private static MatchResult matchResult(int home, int away) {
		return new MatchResult(
				BettingProviders.TIPPMIX,
				"5311343",
				72409632L,
				new MatchScore(home, away),
				TippmixOfferFixtures.KICKOFF.toInstant());
	}

}
