package com.safeedge.tippmix.manager;

import com.safeedge.tippmix.client.TippmixClient;
import com.safeedge.tippmix.config.TippmixProperties;
import com.safeedge.tippmix.dto.TippmixEventDto;
import com.safeedge.tippmix.dto.TippmixEventsRequest;
import com.safeedge.tippmix.dto.TippmixEventsResponse;
import com.safeedge.tippmix.dto.TippmixPaginationDto;
import com.safeedge.tippmix.mapper.TippmixBettingOfferNormalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TippmixPrematchCollector {

	private static final Logger log = LoggerFactory.getLogger(TippmixPrematchCollector.class);

	private final TippmixClient tippmixClient;
	private final TippmixIngestionManager ingestionManager;
	private final TippmixProperties properties;
	private final Clock clock;
	private final AtomicBoolean inProgress = new AtomicBoolean(false);

	public TippmixPrematchCollector(
			TippmixClient tippmixClient,
			TippmixIngestionManager ingestionManager,
			TippmixProperties properties,
			Clock clock) {
		this.tippmixClient = tippmixClient;
		this.ingestionManager = ingestionManager;
		this.properties = properties;
		this.clock = clock;
	}

	public CollectionRunResult collectPrematchFootball() {
		if (!inProgress.compareAndSet(false, true)) {
			Instant now = clock.instant();
			log.warn("Tippmix pre-match collection already in progress; skipping this trigger");
			return new CollectionRunResult(now, now, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of());
		}
		Instant startedAt = clock.instant();
		try {
			return run(startedAt);
		}
		finally {
			inProgress.set(false);
		}
	}

	private CollectionRunResult run(Instant startedAt) {
		int pageSize = properties.collector().pageSize();
		int maxPages = properties.collector().maxPages();
		if (pageSize < 1) {
			throw new TippmixCollectionException(0, "Tippmix collector page-size must be at least 1");
		}
		if (maxPages < 1) {
			throw new TippmixCollectionException(0, "Tippmix collector max-pages must be at least 1");
		}

		int pagesFetched = 0;
		Map<Long, TippmixEventDto> discovered = new LinkedHashMap<>();
		try {
			TippmixEventsResponse firstPage = fetchPage(1, pageSize, pagesFetched);
			pagesFetched = 1;
			collectEvents(firstPage, discovered);
			int pageCount = requirePageCount(firstPage.meta(), maxPages, pagesFetched);
			for (int page = 2; page <= pageCount; page++) {
				TippmixEventsResponse response = fetchPage(page, pageSize, pagesFetched);
				pagesFetched = page;
				collectEvents(response, discovered);
			}
		}
		catch (TippmixCollectionException ex) {
			log.error(
					"Tippmix pre-match collection aborted: discovery incomplete pagesFetched={} reason={}",
					ex.getPagesFetched(),
					ex.getMessage());
			throw ex;
		}
		catch (RuntimeException ex) {
			log.error(
					"Tippmix pre-match collection aborted: discovery incomplete pagesFetched={}",
					pagesFetched,
					ex);
			throw new TippmixCollectionException(pagesFetched, "Tippmix search page failed; discovery incomplete", ex);
		}

		List<Long> eligibleIds = discovered.values().stream()
				.filter(TippmixPrematchCollector::isEligiblePrematchFootball)
				.map(TippmixEventDto::eventId)
				.toList();

		int ingested = 0;
		int skipped = 0;
		int failed = 0;
		int markets = 0;
		int selections = 0;
		int snapshots = 0;
		List<CollectionRunResult.EventFailure> failures = new ArrayList<>();

		for (Long eventId : eligibleIds) {
			try {
				IngestionResult result = ingestionManager.ingestPrematchEvent(eventId);
				ingested++;
				markets += result.supportedMarketCount();
				selections += result.selectionCount();
				snapshots += result.snapshotCount();
				log.debug("Tippmix pre-match collection ingested eventId={}", eventId);
			}
			catch (TippmixIngestionException ex) {
				if (ex.isNoLongerPrematch()) {
					skipped++;
					log.warn(
							"Tippmix pre-match collection skipped event: provider=Tippmix eventId={} reason={}",
							eventId,
							ex.getMessage());
				}
				else {
					failed++;
					failures.add(new CollectionRunResult.EventFailure(eventId, ex.getMessage()));
					log.error(
							"Tippmix pre-match collection failed event: provider=Tippmix eventId={} reason={}",
							eventId,
							ex.getMessage());
				}
			}
			catch (RuntimeException ex) {
				failed++;
				failures.add(new CollectionRunResult.EventFailure(eventId, ex.getMessage()));
				log.error(
						"Tippmix pre-match collection failed event: provider=Tippmix eventId={} reason={}",
						eventId,
						ex.getMessage());
			}
		}

		Instant completedAt = clock.instant();
		CollectionRunResult result = new CollectionRunResult(
				startedAt,
				completedAt,
				pagesFetched,
				discovered.size(),
				eligibleIds.size(),
				ingested,
				skipped,
				failed,
				markets,
				selections,
				snapshots,
				List.copyOf(failures));
		log.info(
				"Tippmix pre-match collection completed: pages={} discovered={} eligible={} ingested={} skipped={} failed={} snapshots={} duration={}",
				result.pagesFetched(),
				result.eventsDiscovered(),
				result.eventsEligible(),
				result.eventsIngested(),
				result.eventsSkipped(),
				result.eventsFailed(),
				result.snapshotsCreated(),
				Duration.between(startedAt, completedAt));
		return result;
	}

	private TippmixEventsResponse fetchPage(int page, int pageSize, int pagesFetched) {
		TippmixEventsResponse response = tippmixClient.searchEvents(footballSearchRequest(page, pageSize));
		if (response == null) {
			throw new TippmixCollectionException(pagesFetched, "Tippmix search response is missing for page " + page);
		}
		TippmixPaginationDto meta = response.meta();
		if (meta == null) {
			throw new TippmixCollectionException(
					pagesFetched, "Tippmix search pagination metadata is missing for page " + page);
		}
		if (meta.currentPage() == null || meta.currentPage() != page) {
			throw new TippmixCollectionException(
					pagesFetched,
					"Tippmix search currentPage=" + meta.currentPage() + " does not match requested page " + page);
		}
		return response;
	}

	private int requirePageCount(TippmixPaginationDto meta, int maxPages, int pagesFetched) {
		Integer pageCount = meta.pageCount();
		if (pageCount == null || pageCount < 1) {
			throw new TippmixCollectionException(
					pagesFetched, "Tippmix search pageCount is missing or less than 1: " + pageCount);
		}
		if (pageCount > maxPages) {
			throw new TippmixCollectionException(
					pagesFetched,
					"Tippmix pageCount=" + pageCount + " exceeds configured max-pages=" + maxPages
							+ "; discovery incomplete");
		}
		return pageCount;
	}

	private void collectEvents(TippmixEventsResponse response, Map<Long, TippmixEventDto> discovered) {
		if (response.events() == null) {
			return;
		}
		for (TippmixEventDto event : response.events()) {
			if (event == null || event.eventId() == null) {
				log.warn("Tippmix search returned an event without eventId; ignoring");
				continue;
			}
			discovered.putIfAbsent(event.eventId(), event);
		}
	}

	static boolean isEligiblePrematchFootball(TippmixEventDto event) {
		return Integer.valueOf(TippmixBettingOfferNormalizer.FOOTBALL_SPORT_ID).equals(event.sportId())
				&& Integer.valueOf(0).equals(event.isLive())
				&& !Integer.valueOf(1).equals(event.isOutright())
				&& Boolean.TRUE.equals(event.hasVisiblePrematchMarket());
	}

	private static TippmixEventsRequest footballSearchRequest(int page, int pageSize) {
		return new TippmixEventsRequest(
				"",
				TippmixBettingOfferNormalizer.FOOTBALL_SPORT_ID,
				null,
				null,
				List.of(),
				List.of(),
				null,
				null,
				null,
				null,
				page,
				pageSize);
	}

}
