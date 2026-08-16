package com.safeedge.tippmix.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.safeedge.tippmix.TippmixOfferFixtures;
import com.safeedge.tippmix.client.TippmixClient;
import com.safeedge.tippmix.client.TippmixClientException;
import com.safeedge.tippmix.config.TippmixProperties;
import com.safeedge.tippmix.dto.TippmixEventDto;
import com.safeedge.tippmix.dto.TippmixEventsRequest;
import com.safeedge.tippmix.dto.TippmixEventsResponse;
import com.safeedge.tippmix.dto.TippmixPaginationDto;
import com.safeedge.tippmix.mapper.TippmixNormalizationException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TippmixPrematchCollectorTest {

	private static final Instant STARTED_AT = Instant.parse("2026-08-16T12:00:00Z");
	private static final Instant COMPLETED_AT = Instant.parse("2026-08-16T12:01:00Z");

	@Mock
	private TippmixClient tippmixClient;

	@Mock
	private TippmixIngestionManager ingestionManager;

	@Mock
	private Clock clock;

	private TippmixPrematchCollector collector;

	@BeforeEach
	void setUp() {
		collector = new TippmixPrematchCollector(tippmixClient, ingestionManager, properties(), clock);
	}

	@Test
	void collectPrematchFootball_fetchesEveryReportedPageOnce() {
		when(clock.instant()).thenReturn(STARTED_AT, COMPLETED_AT);
		when(tippmixClient.searchEvents(any())).thenAnswer(invocation -> {
			int page = invocation.getArgument(0, TippmixEventsRequest.class).page();
			return searchPage(page, 3, eligible(10L + page));
		});
		when(ingestionManager.ingestPrematchEvent(anyLong()))
				.thenReturn(new IngestionResult(1L, 1, 2, 2, STARTED_AT));

		CollectionRunResult result = collector.collectPrematchFootball();

		ArgumentCaptor<TippmixEventsRequest> requestCaptor = ArgumentCaptor.forClass(TippmixEventsRequest.class);
		verify(tippmixClient, times(3)).searchEvents(requestCaptor.capture());
		assertThat(requestCaptor.getAllValues())
				.extracting(TippmixEventsRequest::page)
				.containsExactly(1, 2, 3);
		assertThat(requestCaptor.getAllValues()).allSatisfy(request -> {
			assertThat(request.search()).isEmpty();
			assertThat(request.sportId()).isEqualTo(1);
			assertThat(request.competitionGroupId()).isNull();
			assertThat(request.competitionOrAliasId()).isNull();
			assertThat(request.eventTypes()).isEmpty();
			assertThat(request.marketTypes()).isEmpty();
			assertThat(request.maxDate()).isNull();
			assertThat(request.minDate()).isNull();
			assertThat(request.maxOdds()).isNull();
			assertThat(request.minOdds()).isNull();
			assertThat(request.pageSize()).isEqualTo(20);
		});
		assertThat(result.pagesFetched()).isEqualTo(3);
		assertThat(result.eventsDiscovered()).isEqualTo(3);
		assertThat(result.eventsEligible()).isEqualTo(3);
		assertThat(result.eventsIngested()).isEqualTo(3);
		assertThat(result.startedAt()).isEqualTo(STARTED_AT);
		assertThat(result.completedAt()).isEqualTo(COMPLETED_AT);
	}

	@Test
	void collectPrematchFootball_ingestsOnlyEligiblePrematchFootball() {
		when(clock.instant()).thenReturn(STARTED_AT, COMPLETED_AT);
		when(tippmixClient.searchEvents(any())).thenReturn(searchPage(
				1,
				1,
				eligible(11L),
				TippmixOfferFixtures.event(12L, 1, 1, 0, true, List.of()),
				TippmixOfferFixtures.event(13L, 1, 0, 1, true, List.of()),
				TippmixOfferFixtures.event(14L, 1, 0, 0, false, List.of()),
				TippmixOfferFixtures.event(15L, 2, 0, 0, true, List.of())));
		when(ingestionManager.ingestPrematchEvent(11L)).thenReturn(new IngestionResult(1L, 3, 8, 8, STARTED_AT));

		CollectionRunResult result = collector.collectPrematchFootball();

		verify(ingestionManager).ingestPrematchEvent(11L);
		verify(ingestionManager, never()).ingestPrematchEvent(12L);
		verify(ingestionManager, never()).ingestPrematchEvent(13L);
		verify(ingestionManager, never()).ingestPrematchEvent(14L);
		verify(ingestionManager, never()).ingestPrematchEvent(15L);
		assertThat(result.eventsDiscovered()).isEqualTo(5);
		assertThat(result.eventsEligible()).isEqualTo(1);
		assertThat(result.eventsIngested()).isEqualTo(1);
		assertThat(result.marketsPersisted()).isEqualTo(3);
		assertThat(result.selectionsPersisted()).isEqualTo(8);
		assertThat(result.snapshotsCreated()).isEqualTo(8);
	}

	@Test
	void collectPrematchFootball_deduplicatesEventIdsAcrossPages() {
		when(clock.instant()).thenReturn(STARTED_AT, COMPLETED_AT);
		when(tippmixClient.searchEvents(any())).thenAnswer(invocation -> {
			int page = invocation.getArgument(0, TippmixEventsRequest.class).page();
			if (page == 1) {
				return searchPage(1, 2, eligible(21L), eligible(22L));
			}
			return searchPage(2, 2, eligible(22L), eligible(23L));
		});
		when(ingestionManager.ingestPrematchEvent(anyLong()))
				.thenReturn(new IngestionResult(1L, 0, 0, 0, STARTED_AT));

		CollectionRunResult result = collector.collectPrematchFootball();

		InOrder order = inOrder(ingestionManager);
		order.verify(ingestionManager).ingestPrematchEvent(21L);
		order.verify(ingestionManager).ingestPrematchEvent(22L);
		order.verify(ingestionManager).ingestPrematchEvent(23L);
		verify(ingestionManager, times(1)).ingestPrematchEvent(22L);
		assertThat(result.eventsDiscovered()).isEqualTo(3);
		assertThat(result.eventsEligible()).isEqualTo(3);
		assertThat(result.eventsIngested()).isEqualTo(3);
	}

	@Test
	void collectPrematchFootball_emptyCatalogCompletesSuccessfully() {
		when(clock.instant()).thenReturn(STARTED_AT, COMPLETED_AT);
		when(tippmixClient.searchEvents(any())).thenReturn(new TippmixEventsResponse(
				List.of(), new TippmixPaginationDto(0, 1, 1, 20)));

		CollectionRunResult result = collector.collectPrematchFootball();

		verify(ingestionManager, never()).ingestPrematchEvent(anyLong());
		assertThat(result.pagesFetched()).isEqualTo(1);
		assertThat(result.eventsDiscovered()).isZero();
		assertThat(result.eventsEligible()).isZero();
		assertThat(result.eventsIngested()).isZero();
		assertThat(result.eventsFailed()).isZero();
	}

	@Test
	void collectPrematchFootball_continuesAfterOneEventFailure() {
		when(clock.instant()).thenReturn(STARTED_AT, COMPLETED_AT);
		when(tippmixClient.searchEvents(any()))
				.thenReturn(searchPage(1, 1, eligible(31L), eligible(32L), eligible(33L)));
		when(ingestionManager.ingestPrematchEvent(31L)).thenReturn(new IngestionResult(1L, 1, 2, 2, STARTED_AT));
		when(ingestionManager.ingestPrematchEvent(32L))
				.thenThrow(new TippmixNormalizationException(32L, 99L, "malformed market"));
		when(ingestionManager.ingestPrematchEvent(33L)).thenReturn(new IngestionResult(3L, 2, 3, 3, STARTED_AT));

		CollectionRunResult result = collector.collectPrematchFootball();

		InOrder order = inOrder(ingestionManager);
		order.verify(ingestionManager).ingestPrematchEvent(31L);
		order.verify(ingestionManager).ingestPrematchEvent(32L);
		order.verify(ingestionManager).ingestPrematchEvent(33L);
		assertThat(result.eventsIngested()).isEqualTo(2);
		assertThat(result.eventsFailed()).isEqualTo(1);
		assertThat(result.eventsSkipped()).isZero();
		assertThat(result.snapshotsCreated()).isEqualTo(5);
		assertThat(result.failures()).containsExactly(new CollectionRunResult.EventFailure(32L, "malformed market"));
	}

	@Test
	void collectPrematchFootball_treatsEventBecomingLiveAsSkipped() {
		when(clock.instant()).thenReturn(STARTED_AT, COMPLETED_AT);
		when(tippmixClient.searchEvents(any())).thenReturn(searchPage(1, 1, eligible(41L), eligible(42L)));
		when(ingestionManager.ingestPrematchEvent(41L))
				.thenThrow(new TippmixIngestionException(
						41L,
						"Tippmix event is not pre-match (isLive must be 0)",
						TippmixIngestionException.Reason.NOT_PREMATCH));
		when(ingestionManager.ingestPrematchEvent(42L)).thenReturn(new IngestionResult(2L, 1, 2, 2, STARTED_AT));

		CollectionRunResult result = collector.collectPrematchFootball();

		verify(ingestionManager).ingestPrematchEvent(42L);
		assertThat(result.eventsSkipped()).isEqualTo(1);
		assertThat(result.eventsFailed()).isZero();
		assertThat(result.eventsIngested()).isEqualTo(1);
		assertThat(result.failures()).isEmpty();
	}

	@Test
	void collectPrematchFootball_abortsWhenASearchPageFails() {
		when(tippmixClient.searchEvents(any())).thenAnswer(invocation -> {
			int page = invocation.getArgument(0, TippmixEventsRequest.class).page();
			if (page == 1) {
				return searchPage(1, 2, eligible(51L));
			}
			throw new TippmixClientException(
					TippmixClientException.FailureType.TRANSPORT,
					"searchEvents",
					null,
					null,
					"connection reset",
					null);
		});

		assertThatThrownBy(() -> collector.collectPrematchFootball())
				.isInstanceOf(TippmixCollectionException.class)
				.hasMessageContaining("discovery incomplete")
				.extracting(ex -> ((TippmixCollectionException) ex).getPagesFetched())
				.isEqualTo(1);
		verify(ingestionManager, never()).ingestPrematchEvent(anyLong());
	}

	@Test
	void collectPrematchFootball_abortsWhenPaginationMetadataIsMissing() {
		when(tippmixClient.searchEvents(any())).thenReturn(new TippmixEventsResponse(List.of(eligible(61L)), null));

		assertThatThrownBy(() -> collector.collectPrematchFootball())
				.isInstanceOf(TippmixCollectionException.class)
				.hasMessageContaining("pagination metadata");
		verify(ingestionManager, never()).ingestPrematchEvent(anyLong());
	}

	private static TippmixProperties properties() {
		return new TippmixProperties(
				"https://api.tippmix.hu",
				null,
				new TippmixProperties.Collector(false, Duration.ofMinutes(5), 20, 50));
	}

	private static TippmixEventDto eligible(long eventId) {
		return TippmixOfferFixtures.event(eventId, 1, 0, 0, true, List.of());
	}

	private static TippmixEventsResponse searchPage(int currentPage, int pageCount, TippmixEventDto... events) {
		return new TippmixEventsResponse(
				List.of(events), new TippmixPaginationDto(events.length, pageCount, currentPage, 20));
	}

}
