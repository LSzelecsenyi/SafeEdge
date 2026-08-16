package com.safeedge.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.PostgresTestcontainersConfig;
import com.safeedge.event.domain.BettingOffer;
import com.safeedge.event.domain.BettingProviders;
import com.safeedge.event.domain.OfferSaveResult;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.event.repository.EventEntity;
import com.safeedge.event.repository.EventRepository;
import com.safeedge.event.repository.MarketEntity;
import com.safeedge.event.repository.MarketRepository;
import com.safeedge.event.repository.SelectionEntity;
import com.safeedge.event.repository.SelectionRepository;
import com.safeedge.odds.repository.OddsSnapshotEntity;
import com.safeedge.odds.repository.OddsSnapshotRepository;
import com.safeedge.tippmix.TippmixOfferFixtures;
import com.safeedge.tippmix.mapper.TippmixBettingOfferNormalizer;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
@Transactional
class BettingOfferPersistenceServiceTest {

	private static final Instant FIRST_CAPTURE = Instant.parse("2026-08-16T12:00:00Z");
	private static final Instant SECOND_CAPTURE = Instant.parse("2026-08-16T12:05:00Z");

	@Autowired
	private BettingOfferPersistenceService persistenceService;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private MarketRepository marketRepository;

	@Autowired
	private SelectionRepository selectionRepository;

	@Autowired
	private OddsSnapshotRepository oddsSnapshotRepository;

	private final TippmixBettingOfferNormalizer normalizer = new TippmixBettingOfferNormalizer();

	@Test
	void saveObservation_insertsEventMarketsSelectionsAndSnapshots() {
		BettingOffer offer = sampleOffer();

		OfferSaveResult result = persistenceService.saveObservation(offer, FIRST_CAPTURE);

		EventEntity event = eventRepository.findById(result.eventId()).orElseThrow();
		assertThat(event.getProvider()).isEqualTo(BettingProviders.TIPPMIX);
		assertThat(event.getExternalEventId()).isEqualTo("5311343");
		assertThat(event.getBetradarId()).isEqualTo(72409632L);
		assertThat(event.getStartTime()).isEqualTo(TippmixOfferFixtures.KICKOFF.toInstant());
		assertThat(event.getCreatedAt()).isEqualTo(FIRST_CAPTURE);

		List<MarketEntity> markets = marketRepository.findByEvent_Id(event.getId());
		assertThat(markets).hasSize(1);
		assertThat(markets.getFirst().getLine()).isEqualByComparingTo("-1");

		List<SelectionEntity> selections = selectionRepository.findByMarket_Id(markets.getFirst().getId());
		assertThat(selections).hasSize(2);
		SelectionEntity home = selections.stream()
				.filter(selection -> selection.getSelectionType() == SelectionType.HOME)
				.findFirst()
				.orElseThrow();
		List<OddsSnapshotEntity> snapshots =
				oddsSnapshotRepository.findBySelection_IdOrderByCapturedAtAscIdAsc(home.getId());
		assertThat(snapshots).hasSize(1);
		assertThat(snapshots.getFirst().getOdds()).isEqualByComparingTo("1.57");
		assertThat(snapshots.getFirst().getCapturedAt()).isEqualTo(FIRST_CAPTURE);
		assertThat(result.snapshotCount()).isEqualTo(2);
	}

	@Test
	void saveObservation_secondCallUpsertsIdentitiesAndAppendsSnapshots() {
		BettingOffer offer = sampleOffer();
		OfferSaveResult first = persistenceService.saveObservation(offer, FIRST_CAPTURE);
		OfferSaveResult second = persistenceService.saveObservation(offer, SECOND_CAPTURE);

		assertThat(second.eventId()).isEqualTo(first.eventId());
		assertThat(eventRepository.count()).isEqualTo(1);
		assertThat(marketRepository.count()).isEqualTo(1);
		assertThat(selectionRepository.count()).isEqualTo(2);
		assertThat(oddsSnapshotRepository.count()).isEqualTo(4);

		EventEntity event = eventRepository.findById(first.eventId()).orElseThrow();
		assertThat(event.getCreatedAt()).isEqualTo(FIRST_CAPTURE);
		assertThat(event.getUpdatedAt()).isEqualTo(SECOND_CAPTURE);

		MarketEntity market = marketRepository.findByEvent_Id(event.getId()).getFirst();
		SelectionEntity home = selectionRepository.findByMarket_Id(market.getId()).stream()
				.filter(selection -> selection.getSelectionType() == SelectionType.HOME)
				.findFirst()
				.orElseThrow();
		List<OddsSnapshotEntity> snapshots =
				oddsSnapshotRepository.findBySelection_IdOrderByCapturedAtAscIdAsc(home.getId());
		assertThat(snapshots).hasSize(2);
		assertThat(snapshots.get(0).getOdds()).isEqualByComparingTo(snapshots.get(1).getOdds());
		assertThat(snapshots.get(0).getCapturedAt()).isEqualTo(FIRST_CAPTURE);
		assertThat(snapshots.get(1).getCapturedAt()).isEqualTo(SECOND_CAPTURE);
	}

	private BettingOffer sampleOffer() {
		return normalizer.normalize(TippmixOfferFixtures.prematchFootball(
				TippmixOfferFixtures.asianHandicap("-1", "Djurgarden -1", "AIK Stockholm +1")));
	}

}
