package com.safeedge.tippmix.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.safeedge.result.domain.MatchResult;
import com.safeedge.result.domain.MatchResultSaveKind;
import com.safeedge.result.domain.MatchResultSaveResult;
import com.safeedge.result.service.MatchResultPersistenceService;
import com.safeedge.settlement.MatchScore;
import com.safeedge.tippmix.client.TippmixResultClient;
import com.safeedge.tippmix.dto.TippmixResultCompetitionDto;
import com.safeedge.tippmix.dto.TippmixResultDateDto;
import com.safeedge.tippmix.dto.TippmixResultEventDto;
import com.safeedge.tippmix.dto.TippmixResultRequest;
import com.safeedge.tippmix.dto.TippmixResultResponse;
import com.safeedge.tippmix.dto.TippmixScoreResultDto;
import com.safeedge.tippmix.mapper.TippmixResultNormalizer;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TippmixResultIngestionManagerTest {

	private static final Instant OBSERVED_AT = Instant.parse("2026-08-16T20:00:00Z");
	private static final OffsetDateTime EVENT_DATE = OffsetDateTime.parse("2026-08-16T14:00:00+02:00");

	@Mock
	private TippmixResultClient resultClient;

	@Mock
	private MatchResultPersistenceService persistenceService;

	@Mock
	private Clock clock;

	private TippmixResultIngestionManager manager;

	@BeforeEach
	void setUp() {
		manager = new TippmixResultIngestionManager(
				resultClient, new TippmixResultNormalizer(), persistenceService, clock);
	}

	@Test
	void collectResults_flattensNestedResponseAndPersistsKnownFinishedFootball() {
		when(clock.instant()).thenReturn(OBSERVED_AT);
		when(resultClient.fetchResults(any())).thenReturn(response(
				endedFootball(5306177L, ft("2.0", "1.0", false)),
				endedFootball(5306178L, ft("0.0", "0.0", false))));
		when(persistenceService.saveIfEventKnown(any(), eq(OBSERVED_AT)))
				.thenReturn(Optional.of(new MatchResultSaveResult(MatchResultSaveKind.INSERTED, 1L)))
				.thenReturn(Optional.empty());

		ResultCollectionRunResult result = manager.collectResults();

		ArgumentCaptor<TippmixResultRequest> requestCaptor = ArgumentCaptor.forClass(TippmixResultRequest.class);
		verify(resultClient).fetchResults(requestCaptor.capture());
		assertThat(requestCaptor.getValue()).isEqualTo(TippmixResultRequest.verifiedFootballResults());
		verify(clock).instant();

		ArgumentCaptor<MatchResult> matchCaptor = ArgumentCaptor.forClass(MatchResult.class);
		verify(persistenceService, org.mockito.Mockito.times(2)).saveIfEventKnown(matchCaptor.capture(), eq(OBSERVED_AT));
		assertThat(matchCaptor.getAllValues())
				.extracting(MatchResult::externalEventId)
				.containsExactly("5306177", "5306178");
		assertThat(matchCaptor.getAllValues().getFirst().finalScore()).isEqualTo(new MatchScore(2, 1));

		assertThat(result.eventsReceived()).isEqualTo(2);
		assertThat(result.finishedEvents()).isEqualTo(2);
		assertThat(result.knownEvents()).isEqualTo(1);
		assertThat(result.resultsInserted()).isEqualTo(1);
		assertThat(result.eventsSkipped()).isEqualTo(1);
		assertThat(result.eventsFailed()).isZero();
		assertThat(result.observedAt()).isEqualTo(OBSERVED_AT);
	}

	@Test
	void collectResults_skipsNonFinishedAndContinuesAfterMalformedEvent() {
		when(clock.instant()).thenReturn(OBSERVED_AT);
		when(resultClient.fetchResults(any())).thenReturn(response(
				endedFootball(11L, ft("2.0", "1.0", false)),
				liveFootball(12L),
				endedFootball(13L, ft("2.5", "1.0", false)),
				endedFootball(14L, ft("1.0", "0.0", false))));
		when(persistenceService.saveIfEventKnown(any(), eq(OBSERVED_AT)))
				.thenReturn(Optional.of(new MatchResultSaveResult(MatchResultSaveKind.INSERTED, 10L)))
				.thenReturn(Optional.of(new MatchResultSaveResult(MatchResultSaveKind.UPDATED, 11L)));

		ResultCollectionRunResult result = manager.collectResults();

		assertThat(result.eventsReceived()).isEqualTo(4);
		assertThat(result.finishedEvents()).isEqualTo(3);
		assertThat(result.knownEvents()).isEqualTo(2);
		assertThat(result.resultsInserted()).isEqualTo(1);
		assertThat(result.resultsUpdated()).isEqualTo(1);
		assertThat(result.eventsSkipped()).isEqualTo(1);
		assertThat(result.eventsFailed()).isEqualTo(1);
		assertThat(result.failures()).extracting(ResultCollectionRunResult.EventFailure::eventId).containsExactly(13L);
		verify(persistenceService, never())
				.saveIfEventKnown(org.mockito.ArgumentMatchers.argThat(match -> "12".equals(match.externalEventId())), any());
	}

	private static TippmixResultResponse response(TippmixResultEventDto... events) {
		return new TippmixResultResponse(
				"2026-08-16",
				List.of(new TippmixResultDateDto(
						"2026-08-16",
						List.of(new TippmixResultCompetitionDto(
								1, "Football", 20L, 1, "Iceland", List.of(events))))));
	}

	private static TippmixResultEventDto endedFootball(long eventId, TippmixScoreResultDto... scores) {
		return new TippmixResultEventDto(
				68306982L,
				eventId,
				"Grindavik - Throttur Reykjavik",
				EVENT_DATE,
				1,
				"Football",
				"ended",
				List.of(scores));
	}

	private static TippmixResultEventDto liveFootball(long eventId) {
		return new TippmixResultEventDto(
				68306982L, eventId, "Live", EVENT_DATE, 1, "Football", "live", List.of(ft("1.0", "0.0", false)));
	}

	private static TippmixScoreResultDto ft(String home, String away, boolean cancelled) {
		return new TippmixScoreResultDto(1, "FT", new BigDecimal(home), new BigDecimal(away), cancelled);
	}

}
