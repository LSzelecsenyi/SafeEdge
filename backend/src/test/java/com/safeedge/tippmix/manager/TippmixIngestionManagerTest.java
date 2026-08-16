package com.safeedge.tippmix.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.safeedge.event.domain.BettingOffer;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.OfferSaveResult;
import com.safeedge.event.service.BettingOfferPersistenceService;
import com.safeedge.tippmix.TippmixOfferFixtures;
import com.safeedge.tippmix.client.TippmixClient;
import com.safeedge.tippmix.dto.TippmixEventResponse;
import com.safeedge.tippmix.mapper.TippmixBettingOfferNormalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TippmixIngestionManagerTest {

	private static final Instant CAPTURED_AT = Instant.parse("2026-08-16T12:00:00Z");

	@Mock
	private TippmixClient tippmixClient;

	@Mock
	private BettingOfferPersistenceService persistenceService;

	@Mock
	private Clock clock;

	private TippmixIngestionManager manager;

	@BeforeEach
	void setUp() {
		manager = new TippmixIngestionManager(
				tippmixClient, new TippmixBettingOfferNormalizer(), persistenceService, clock);
	}

	@Test
	void ingestPrematchEvent_normalizesSupportedMarketsAndPersistsOnce() {
		when(clock.instant()).thenReturn(CAPTURED_AT);
		when(tippmixClient.getEvent(5311343L))
				.thenReturn(new TippmixEventResponse(TippmixOfferFixtures.prematchFootball(
						TippmixOfferFixtures.asianHandicap("-1", "Djurgarden -1", "AIK Stockholm +1"),
						TippmixOfferFixtures.europeanHandicap(),
						TippmixOfferFixtures.doubleChance(),
						TippmixOfferFixtures.exactScore())));
		when(persistenceService.saveObservation(any(), eq(CAPTURED_AT)))
				.thenReturn(new OfferSaveResult(42L, 3, 8, 8, CAPTURED_AT));

		IngestionResult result = manager.ingestPrematchEvent(5311343L);

		verify(clock, times(1)).instant();
		verify(tippmixClient).getEvent(5311343L);
		ArgumentCaptor<BettingOffer> offerCaptor = ArgumentCaptor.forClass(BettingOffer.class);
		verify(persistenceService).saveObservation(offerCaptor.capture(), eq(CAPTURED_AT));
		verify(persistenceService, times(1)).saveObservation(any(), any());

		BettingOffer offer = offerCaptor.getValue();
		assertThat(offer.markets())
				.extracting(market -> market.marketType())
				.containsExactly(
						MarketType.ASIAN_HANDICAP, MarketType.EUROPEAN_HANDICAP, MarketType.DOUBLE_CHANCE);
		assertThat(offer.markets())
				.noneMatch(market -> "Pontos végeredmény".equals(market.providerMarketName()));
		assertThat(result.eventId()).isEqualTo(42L);
		assertThat(result.capturedAt()).isEqualTo(CAPTURED_AT);
		assertThat(result.supportedMarketCount()).isEqualTo(3);
	}

	@Test
	void ingestPrematchEvent_rejectsLiveEvent() {
		when(tippmixClient.getEvent(5311343L))
				.thenReturn(new TippmixEventResponse(TippmixOfferFixtures.event(
						1, 1, true, List.of(TippmixOfferFixtures.europeanHandicap()))));

		assertThatThrownBy(() -> manager.ingestPrematchEvent(5311343L))
				.isInstanceOf(TippmixIngestionException.class)
				.hasMessageContaining("pre-match");
		verify(persistenceService, never()).saveObservation(any(), any());
	}

	@Test
	void ingestPrematchEvent_rejectsNonFootballEvent() {
		when(tippmixClient.getEvent(5311343L))
				.thenReturn(new TippmixEventResponse(TippmixOfferFixtures.event(
						2, 0, true, List.of(TippmixOfferFixtures.europeanHandicap()))));

		assertThatThrownBy(() -> manager.ingestPrematchEvent(5311343L))
				.isInstanceOf(TippmixIngestionException.class)
				.hasMessageContaining("football");
		verify(persistenceService, never()).saveObservation(any(), any());
	}

}
