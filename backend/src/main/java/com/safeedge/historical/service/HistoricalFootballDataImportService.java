package com.safeedge.historical.service;

import com.safeedge.historical.domain.HistoricalAhQuoteDraft;
import com.safeedge.historical.domain.HistoricalImportResult;
import com.safeedge.historical.domain.HistoricalMatchDraft;
import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.MappedHistoricalMatch;
import com.safeedge.historical.repository.HistoricalAhOfferEntity;
import com.safeedge.historical.repository.HistoricalAhOfferRepository;
import com.safeedge.historical.repository.HistoricalMatchEntity;
import com.safeedge.historical.repository.HistoricalMatchRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoricalFootballDataImportService {

	private static final Logger log = LoggerFactory.getLogger(HistoricalFootballDataImportService.class);

	private final HistoricalMatchRepository matchRepository;
	private final HistoricalAhOfferRepository offerRepository;

	public HistoricalFootballDataImportService(
			HistoricalMatchRepository matchRepository, HistoricalAhOfferRepository offerRepository) {
		this.matchRepository = matchRepository;
		this.offerRepository = offerRepository;
	}

	@Transactional
	public PersistCounts persist(List<MappedHistoricalMatch> rows, Instant now) {
		int matchesInserted = 0;
		int matchesUpdated = 0;
		int quotesInserted = 0;
		int quotesUpdated = 0;
		int rowsRejected = 0;
		int quotesSkippedIncomplete = 0;
		int quotesSkippedInvalidOdds = 0;
		int quotesSkippedInvalidLine = 0;
		Map<MatchKey, ScoreKey> seenInFile = new LinkedHashMap<>();
		for (MappedHistoricalMatch row : rows) {
			HistoricalMatchDraft match = row.match();
			MatchKey key = MatchKey.from(match);
			ScoreKey score = new ScoreKey(match.homeGoals(), match.awayGoals());
			ScoreKey previous = seenInFile.putIfAbsent(key, score);
			if (previous != null) {
				rowsRejected++;
				log.warn(
						"Duplicate historical match row rejected: source={} file={} row={} home={} away={} date={}",
						match.source(),
						match.sourceFile(),
						match.sourceRowNumber(),
						match.sourceHomeTeamName(),
						match.sourceAwayTeamName(),
						match.matchDate());
				continue;
			}
			quotesSkippedIncomplete += row.quotesSkippedIncomplete();
			quotesSkippedInvalidOdds += row.quotesSkippedInvalidOdds();
			quotesSkippedInvalidLine += row.quotesSkippedInvalidLine();
			MatchPersistResult matchResult = upsertMatch(match, now);
			if (matchResult.inserted()) {
				matchesInserted++;
			}
			else if (matchResult.updated()) {
				matchesUpdated++;
			}
			for (HistoricalAhQuoteDraft quote : row.quotes()) {
				QuotePersistResult quoteResult = upsertQuote(matchResult.entity(), quote, now);
				if (quoteResult.inserted()) {
					quotesInserted++;
				}
				else if (quoteResult.updated()) {
					quotesUpdated++;
				}
			}
		}
		return new PersistCounts(
				matchesInserted,
				matchesUpdated,
				quotesInserted,
				quotesUpdated,
				rowsRejected,
				quotesSkippedIncomplete,
				quotesSkippedInvalidOdds,
				quotesSkippedInvalidLine);
	}

	private MatchPersistResult upsertMatch(HistoricalMatchDraft draft, Instant now) {
		Optional<HistoricalMatchEntity> existing = matchRepository
				.findBySourceAndCanonicalCompetitionAndSeasonStartYearAndSeasonEndYearAndMatchDateAndSourceHomeTeamNameAndSourceAwayTeamName(
						draft.source(),
						draft.canonicalCompetition(),
						draft.season().startYear(),
						draft.season().endYear(),
						draft.matchDate(),
						draft.sourceHomeTeamName(),
						draft.sourceAwayTeamName());
		if (existing.isEmpty()) {
			HistoricalMatchEntity entity = new HistoricalMatchEntity();
			applyMatch(entity, draft);
			entity.setCreatedAt(now);
			entity.setUpdatedAt(now);
			matchRepository.save(entity);
			return new MatchPersistResult(entity, true, false);
		}
		HistoricalMatchEntity entity = existing.get();
		boolean scoreChanged = !Objects.equals(entity.getHomeGoals(), draft.homeGoals())
				|| !Objects.equals(entity.getAwayGoals(), draft.awayGoals());
		if (scoreChanged) {
			log.warn(
					"Historical match score corrected: source={} file={} row={} previous={}-{} new={}-{} home={} away={} date={}",
					draft.source(),
					draft.sourceFile(),
					draft.sourceRowNumber(),
					entity.getHomeGoals(),
					entity.getAwayGoals(),
					draft.homeGoals(),
					draft.awayGoals(),
					draft.sourceHomeTeamName(),
					draft.sourceAwayTeamName(),
					draft.matchDate());
		}
		applyMatch(entity, draft);
		entity.setUpdatedAt(now);
		matchRepository.save(entity);
		return new MatchPersistResult(entity, false, scoreChanged);
	}

	private QuotePersistResult upsertQuote(
			HistoricalMatchEntity match, HistoricalAhQuoteDraft draft, Instant now) {
		Optional<HistoricalAhOfferEntity> existing = offerRepository.findByHistoricalMatch_IdAndQuoteSourceAndObservationType(
				match.getId(), draft.quoteSource(), HistoricalObservationType.PRE_MATCH_SNAPSHOT);
		if (existing.isEmpty()) {
			HistoricalAhOfferEntity entity = new HistoricalAhOfferEntity();
			entity.setHistoricalMatch(match);
			entity.setCreatedAt(now);
			applyQuote(entity, draft);
			entity.setUpdatedAt(now);
			offerRepository.save(entity);
			return new QuotePersistResult(true, false);
		}
		HistoricalAhOfferEntity entity = existing.get();
		boolean changed = entity.getHomeHandicapLine().compareTo(draft.homeHandicapLine()) != 0
				|| entity.getHomeOdds().compareTo(draft.homeOdds()) != 0
				|| entity.getAwayOdds().compareTo(draft.awayOdds()) != 0;
		if (changed) {
			log.warn(
					"Historical AH quote corrected: source={} quoteSource={} matchId={} previousLine={} newLine={} previousHomeOdds={} newHomeOdds={}",
					draft.source(),
					draft.quoteSource(),
					match.getId(),
					entity.getHomeHandicapLine(),
					draft.homeHandicapLine(),
					entity.getHomeOdds(),
					draft.homeOdds());
		}
		applyQuote(entity, draft);
		entity.setUpdatedAt(now);
		offerRepository.save(entity);
		return new QuotePersistResult(false, changed);
	}

	private static void applyMatch(HistoricalMatchEntity entity, HistoricalMatchDraft draft) {
		entity.setSource(draft.source());
		entity.setSourceCompetitionCode(draft.sourceCompetitionCode());
		entity.setCanonicalCompetition(draft.canonicalCompetition());
		entity.setSeasonStartYear(draft.season().startYear());
		entity.setSeasonEndYear(draft.season().endYear());
		entity.setMatchDate(draft.matchDate());
		LocalTime kickoff = draft.sourceKickoffTime();
		entity.setSourceKickoffTime(kickoff == null ? null : kickoff.toString());
		entity.setKickoffUtc(draft.kickoffUtc());
		entity.setSourceHomeTeamName(draft.sourceHomeTeamName());
		entity.setSourceAwayTeamName(draft.sourceAwayTeamName());
		entity.setHomeGoals(draft.homeGoals());
		entity.setAwayGoals(draft.awayGoals());
		entity.setSourceFile(draft.sourceFile());
		entity.setSourceRowNumber(draft.sourceRowNumber());
	}

	private static void applyQuote(HistoricalAhOfferEntity entity, HistoricalAhQuoteDraft draft) {
		entity.setSource(draft.source());
		entity.setQuoteSource(draft.quoteSource());
		entity.setHomeHandicapLine(draft.homeHandicapLine());
		entity.setHomeOdds(draft.homeOdds());
		entity.setAwayOdds(draft.awayOdds());
		entity.setObservationType(draft.observationType());
		entity.setObservedAt(draft.observedAt());
		entity.setSourceLineColumn(draft.sourceLineColumn());
		entity.setSourceHomeOddsColumn(draft.sourceHomeOddsColumn());
		entity.setSourceAwayOddsColumn(draft.sourceAwayOddsColumn());
		entity.setRawLineValue(draft.rawLineValue());
		entity.setRawHomeOddsValue(draft.rawHomeOddsValue());
		entity.setRawAwayOddsValue(draft.rawAwayOddsValue());
	}

	public record PersistCounts(
			int matchesInserted,
			int matchesUpdated,
			int quotesInserted,
			int quotesUpdated,
			int rowsRejected,
			int quotesSkippedIncomplete,
			int quotesSkippedInvalidOdds,
			int quotesSkippedInvalidLine) {
	}

	private record MatchKey(
			String source,
			String competition,
			int startYear,
			int endYear,
			LocalDate date,
			String home,
			String away) {

		private static MatchKey from(HistoricalMatchDraft draft) {
			return new MatchKey(
					draft.source().name(),
					draft.canonicalCompetition().name(),
					draft.season().startYear(),
					draft.season().endYear(),
					draft.matchDate(),
					draft.sourceHomeTeamName(),
					draft.sourceAwayTeamName());
		}
	}

	private record ScoreKey(int homeGoals, int awayGoals) {
	}

	private record MatchPersistResult(HistoricalMatchEntity entity, boolean inserted, boolean updated) {
	}

	private record QuotePersistResult(boolean inserted, boolean updated) {
	}

	public static HistoricalImportResult toResult(
			com.safeedge.historical.domain.HistoricalSource source,
			com.safeedge.historical.domain.CanonicalCompetition league,
			com.safeedge.historical.domain.FootballSeason season,
			String sourceFile,
			int rowsRead,
			int mapperRejected,
			PersistCounts persisted) {
		return new HistoricalImportResult(
				source,
				league,
				season,
				sourceFile,
				rowsRead,
				persisted.matchesInserted(),
				persisted.matchesUpdated(),
				persisted.quotesInserted(),
				persisted.quotesUpdated(),
				mapperRejected + persisted.rowsRejected(),
				persisted.quotesSkippedIncomplete(),
				persisted.quotesSkippedInvalidOdds(),
				persisted.quotesSkippedInvalidLine());
	}

}
