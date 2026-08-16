package com.safeedge.tippmix.manager;

import com.safeedge.event.domain.BettingOffer;
import com.safeedge.event.domain.OfferSaveResult;
import com.safeedge.event.service.BettingOfferPersistenceService;
import com.safeedge.tippmix.client.TippmixClient;
import com.safeedge.tippmix.dto.TippmixEventDto;
import com.safeedge.tippmix.dto.TippmixEventResponse;
import com.safeedge.tippmix.mapper.TippmixBettingOfferNormalizer;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TippmixIngestionManager {

	private static final Logger log = LoggerFactory.getLogger(TippmixIngestionManager.class);

	private final TippmixClient tippmixClient;
	private final TippmixBettingOfferNormalizer normalizer;
	private final BettingOfferPersistenceService persistenceService;
	private final Clock clock;

	public TippmixIngestionManager(
			TippmixClient tippmixClient,
			TippmixBettingOfferNormalizer normalizer,
			BettingOfferPersistenceService persistenceService,
			Clock clock) {
		this.tippmixClient = tippmixClient;
		this.normalizer = normalizer;
		this.persistenceService = persistenceService;
		this.clock = clock;
	}

	public IngestionResult ingestPrematchEvent(long tippmixEventId) {
		TippmixEventResponse response = tippmixClient.getEvent(tippmixEventId);
		if (response == null || response.event() == null) {
			throw new TippmixIngestionException(tippmixEventId, "Tippmix event response is missing");
		}
		TippmixEventDto event = response.event();
		requirePrematchFootball(event, tippmixEventId);
		BettingOffer offer = normalizer.normalize(event);
		Instant capturedAt = clock.instant();
		OfferSaveResult saved = persistenceService.saveObservation(offer, capturedAt);
		log.info(
				"Tippmix ingestion complete: provider=Tippmix eventId={} supportedMarkets={} selections={} snapshots={} capturedAt={}",
				tippmixEventId,
				saved.supportedMarketCount(),
				saved.selectionCount(),
				saved.snapshotCount(),
				capturedAt);
		return IngestionResult.from(saved);
	}

	private void requirePrematchFootball(TippmixEventDto event, long tippmixEventId) {
		if (!Integer.valueOf(TippmixBettingOfferNormalizer.FOOTBALL_SPORT_ID).equals(event.sportId())) {
			throw new TippmixIngestionException(tippmixEventId, "Tippmix event is not football");
		}
		if (!Integer.valueOf(0).equals(event.isLive())) {
			throw new TippmixIngestionException(tippmixEventId, "Tippmix event is not pre-match (isLive must be 0)");
		}
		if (!Boolean.TRUE.equals(event.hasVisiblePrematchMarket())) {
			throw new TippmixIngestionException(
					tippmixEventId, "Tippmix event has no visible pre-match market");
		}
	}

}
