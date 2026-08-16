package com.safeedge.event.service;

import com.safeedge.event.domain.BettingEvent;
import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingOffer;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.OfferSaveResult;
import com.safeedge.event.repository.EventEntity;
import com.safeedge.event.repository.EventRepository;
import com.safeedge.event.repository.MarketEntity;
import com.safeedge.event.repository.MarketRepository;
import com.safeedge.event.repository.SelectionEntity;
import com.safeedge.event.repository.SelectionRepository;
import com.safeedge.odds.repository.OddsSnapshotEntity;
import com.safeedge.odds.repository.OddsSnapshotRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BettingOfferPersistenceService {

	private final EventRepository eventRepository;
	private final MarketRepository marketRepository;
	private final SelectionRepository selectionRepository;
	private final OddsSnapshotRepository oddsSnapshotRepository;

	public BettingOfferPersistenceService(
			EventRepository eventRepository,
			MarketRepository marketRepository,
			SelectionRepository selectionRepository,
			OddsSnapshotRepository oddsSnapshotRepository) {
		this.eventRepository = eventRepository;
		this.marketRepository = marketRepository;
		this.selectionRepository = selectionRepository;
		this.oddsSnapshotRepository = oddsSnapshotRepository;
	}

	@Transactional
	public OfferSaveResult saveObservation(BettingOffer offer, Instant capturedAt) {
		EventEntity eventEntity = upsertEvent(offer.event(), capturedAt);
		int marketCount = 0;
		int selectionCount = 0;
		int snapshotCount = 0;
		for (BettingMarket market : offer.markets()) {
			MarketEntity marketEntity = upsertMarket(eventEntity, market, capturedAt);
			marketCount++;
			for (BettingSelection selection : market.selections()) {
				SelectionEntity selectionEntity = upsertSelection(marketEntity, selection, capturedAt);
				selectionCount++;
				OddsSnapshotEntity snapshot = new OddsSnapshotEntity();
				snapshot.setSelection(selectionEntity);
				snapshot.setOdds(selection.odds());
				snapshot.setCapturedAt(capturedAt);
				snapshot.setProviderMarketVersion(market.providerMarketVersion());
				oddsSnapshotRepository.save(snapshot);
				snapshotCount++;
			}
		}
		return new OfferSaveResult(eventEntity.getId(), marketCount, selectionCount, snapshotCount, capturedAt);
	}

	private EventEntity upsertEvent(BettingEvent event, Instant capturedAt) {
		EventEntity entity = eventRepository
				.findByProviderAndExternalEventId(event.provider(), event.externalEventId())
				.orElseGet(EventEntity::new);
		boolean created = entity.getId() == null;
		entity.setProvider(event.provider());
		entity.setExternalEventId(event.externalEventId());
		entity.setBetradarId(event.betradarId());
		entity.setName(event.name());
		entity.setStartTime(event.startTime());
		entity.setCompetitionExternalId(event.competitionExternalId());
		entity.setCompetitionName(event.competitionName());
		entity.setHomeParticipantExternalId(event.homeParticipantExternalId());
		entity.setHomeParticipantName(event.homeParticipantName());
		entity.setAwayParticipantExternalId(event.awayParticipantExternalId());
		entity.setAwayParticipantName(event.awayParticipantName());
		if (created) {
			entity.setCreatedAt(capturedAt);
		}
		entity.setUpdatedAt(capturedAt);
		return eventRepository.save(entity);
	}

	private MarketEntity upsertMarket(EventEntity eventEntity, BettingMarket market, Instant capturedAt) {
		MarketEntity entity = marketRepository
				.findByProviderAndExternalMarketId(market.provider(), market.externalMarketId())
				.orElseGet(MarketEntity::new);
		boolean created = entity.getId() == null;
		entity.setEvent(eventEntity);
		entity.setProvider(market.provider());
		entity.setExternalMarketId(market.externalMarketId());
		entity.setProviderMarketRealNo(market.providerMarketRealNo());
		entity.setProviderMarketName(market.providerMarketName());
		entity.setProviderMarketType(market.providerMarketType());
		entity.setProviderMarketSubType(market.providerMarketSubType());
		entity.setProviderMarketVersion(market.providerMarketVersion());
		entity.setMarketType(market.marketType());
		entity.setLine(market.line());
		if (created) {
			entity.setCreatedAt(capturedAt);
		}
		entity.setUpdatedAt(capturedAt);
		return marketRepository.save(entity);
	}

	private SelectionEntity upsertSelection(MarketEntity marketEntity, BettingSelection selection, Instant capturedAt) {
		SelectionEntity entity = selectionRepository
				.findByMarket_IdAndExternalOutcomeNo(marketEntity.getId(), selection.externalOutcomeNo())
				.orElseGet(SelectionEntity::new);
		boolean created = entity.getId() == null;
		entity.setMarket(marketEntity);
		entity.setProvider(selection.provider());
		entity.setExternalOutcomeNo(selection.externalOutcomeNo());
		entity.setExternalOutcomeRealNo(selection.externalOutcomeRealNo());
		entity.setProviderOutcomeName(selection.providerOutcomeName());
		entity.setSelectionType(selection.selectionType());
		entity.setLine(selection.line());
		if (created) {
			entity.setCreatedAt(capturedAt);
		}
		entity.setUpdatedAt(capturedAt);
		return selectionRepository.save(entity);
	}

}
