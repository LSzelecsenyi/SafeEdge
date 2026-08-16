package com.safeedge.historical.footballdata.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.safeedge.historical.domain.BulkHistoricalImportResult;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.domain.HistoricalImportFailureStage;
import com.safeedge.historical.domain.HistoricalImportResult;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.footballdata.client.FootballDataClientException;
import com.safeedge.historical.footballdata.parser.FootballDataParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FootballDataBulkHistoricalImportManagerTest {

	private static final Instant STARTED = Instant.parse("2026-08-16T12:00:00Z");
	private static final Instant COMPLETED = Instant.parse("2026-08-16T12:01:00Z");

	@Mock
	private FootballDataHistoricalImportManager importManager;

	@Mock
	private Clock clock;

	private FootballDataBulkHistoricalImportManager bulkManager;

	@BeforeEach
	void setUp() {
		bulkManager = new FootballDataBulkHistoricalImportManager(importManager, clock);
	}

	@Test
	void invokesEveryLeagueSeasonPairInDeterministicOrder() {
		when(clock.instant()).thenReturn(STARTED, COMPLETED);
		when(importManager.importSeason(any(), any())).thenAnswer(invocation -> ok(
				invocation.getArgument(0),
				invocation.getArgument(1),
				10,
				10,
				4));
		FootballSeason first = new FootballSeason(2010, 2011);
		FootballSeason second = new FootballSeason(2011, 2012);
		bulkManager.importRange(
				EnumSet.of(CanonicalCompetition.BUNDESLIGA, CanonicalCompetition.PREMIER_LEAGUE), 2010, 2011);
		InOrder order = inOrder(importManager);
		order.verify(importManager).importSeason(CanonicalCompetition.PREMIER_LEAGUE, first);
		order.verify(importManager).importSeason(CanonicalCompetition.PREMIER_LEAGUE, second);
		order.verify(importManager).importSeason(CanonicalCompetition.BUNDESLIGA, first);
		order.verify(importManager).importSeason(CanonicalCompetition.BUNDESLIGA, second);
		verify(importManager, times(4)).importSeason(any(), any());
	}

	@Test
	void oneFailureDoesNotStopLaterImportsAndRecordsStage() {
		when(clock.instant()).thenReturn(STARTED, COMPLETED);
		FootballSeason s2010 = new FootballSeason(2010, 2011);
		FootballSeason s2011 = new FootballSeason(2011, 2012);
		FootballSeason s2012 = new FootballSeason(2012, 2013);
		when(importManager.importSeason(CanonicalCompetition.PREMIER_LEAGUE, s2010))
				.thenReturn(ok(CanonicalCompetition.PREMIER_LEAGUE, s2010, 10, 9, 8));
		when(importManager.importSeason(CanonicalCompetition.PREMIER_LEAGUE, s2011))
				.thenThrow(new FootballDataClientException(
						FootballDataClientException.FailureType.NOT_FOUND, "missing 2011/12", null));
		when(importManager.importSeason(CanonicalCompetition.PREMIER_LEAGUE, s2012))
				.thenReturn(ok(CanonicalCompetition.PREMIER_LEAGUE, s2012, 5, 5, 2));
		BulkHistoricalImportResult result =
				bulkManager.importRange(Set.of(CanonicalCompetition.PREMIER_LEAGUE), 2010, 2012);
		verify(importManager).importSeason(CanonicalCompetition.PREMIER_LEAGUE, s2012);
		assertThat(result.leagueSeasonPairsRequested()).isEqualTo(3);
		assertThat(result.leagueSeasonPairsSucceeded()).isEqualTo(2);
		assertThat(result.leagueSeasonPairsFailed()).isEqualTo(1);
		assertThat(result.rowsRead()).isEqualTo(15);
		assertThat(result.matchesInserted()).isEqualTo(14);
		assertThat(result.quotesInserted()).isEqualTo(10);
		assertThat(result.failures()).hasSize(1);
		assertThat(result.failures().getFirst().competition()).isEqualTo(CanonicalCompetition.PREMIER_LEAGUE);
		assertThat(result.failures().getFirst().season()).isEqualTo(s2011);
		assertThat(result.failures().getFirst().stage()).isEqualTo(HistoricalImportFailureStage.SOURCE_NOT_FOUND);
		assertThat(result.failures().getFirst().message()).contains("missing 2011/12");
		assertThat(result.startedAt()).isEqualTo(STARTED);
		assertThat(result.completedAt()).isEqualTo(COMPLETED);
	}

	@Test
	void mapsFetchAndParseFailures() {
		when(clock.instant()).thenReturn(STARTED, COMPLETED);
		FootballSeason first = new FootballSeason(2023, 2024);
		FootballSeason second = new FootballSeason(2024, 2025);
		when(importManager.importSeason(CanonicalCompetition.SERIE_A, first))
				.thenThrow(new FootballDataClientException(
						FootballDataClientException.FailureType.TRANSPORT, "timeout", null));
		when(importManager.importSeason(CanonicalCompetition.SERIE_A, second))
				.thenThrow(new FootballDataParseException("bad csv"));
		BulkHistoricalImportResult result =
				bulkManager.importRange(Set.of(CanonicalCompetition.SERIE_A), 2023, 2024);
		assertThat(result.failures())
				.extracting(failure -> failure.stage())
				.containsExactly(
						HistoricalImportFailureStage.FETCH_FAILED, HistoricalImportFailureStage.PARSE_FAILED);
	}

	@Test
	void rejectsEmptyLeagueSetAndInvalidRangeWithoutCallingImporter() {
		assertThatThrownBy(() -> bulkManager.importRange(Set.of(), 2010, 2012))
				.isInstanceOf(HistoricalDataException.class)
				.hasMessageContaining("competition");
		assertThatThrownBy(() -> bulkManager.importRange(Set.of(CanonicalCompetition.LA_LIGA), 2012, 2010))
				.isInstanceOf(HistoricalDataException.class)
				.hasMessageContaining("Season range");
		assertThatThrownBy(() -> bulkManager.importRange(Set.of(CanonicalCompetition.LA_LIGA), 0, 2010))
				.isInstanceOf(HistoricalDataException.class)
				.hasMessageContaining("Season range");
		verify(importManager, never()).importSeason(any(), any());
	}

	private static HistoricalImportResult ok(
			CanonicalCompetition league, FootballSeason season, int rowsRead, int matchesInserted, int quotesInserted) {
		return new HistoricalImportResult(
				HistoricalSource.FOOTBALL_DATA_UK,
				league,
				season,
				"mmz4281/file.csv",
				rowsRead,
				matchesInserted,
				0,
				quotesInserted,
				0,
				0,
				0,
				0,
				0);
	}
}
