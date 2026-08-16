package com.safeedge.tippmix.mapper;

import com.safeedge.event.domain.BettingProviders;
import com.safeedge.result.domain.MatchResult;
import com.safeedge.settlement.MatchScore;
import com.safeedge.tippmix.dto.TippmixResultEventDto;
import com.safeedge.tippmix.dto.TippmixScoreResultDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TippmixResultNormalizer {

	public static final String ENDED_STATUS = "ended";
	static final String FULL_TIME_SCORE_TYPE = "FT";
	static final int FULL_TIME_SCORE_TYPE_NO = 1;

	public Optional<MatchResult> normalize(TippmixResultEventDto event) {
		if (event == null) {
			return Optional.empty();
		}
		if (!Integer.valueOf(TippmixBettingOfferNormalizer.FOOTBALL_SPORT_ID).equals(event.sportId())) {
			return Optional.empty();
		}
		if (!ENDED_STATUS.equals(event.matchStatus())) {
			return Optional.empty();
		}
		Optional<TippmixScoreResultDto> ft = selectFullTimeScore(event);
		if (ft.isEmpty()) {
			return Optional.empty();
		}
		if (event.eventId() == null) {
			throw new TippmixResultNormalizationException(null, "Tippmix result eventId is required");
		}
		MatchScore score = toMatchScore(event.eventId(), ft.get());
		Instant eventDate = event.eventDate() == null ? null : event.eventDate().toInstant();
		return Optional.of(new MatchResult(
				BettingProviders.TIPPMIX,
				String.valueOf(event.eventId()),
				event.betradarId(),
				score,
				eventDate));
	}

	private Optional<TippmixScoreResultDto> selectFullTimeScore(TippmixResultEventDto event) {
		List<TippmixScoreResultDto> fullTime = new ArrayList<>();
		if (event.scoreResults() != null) {
			for (TippmixScoreResultDto score : event.scoreResults()) {
				if (score != null && FULL_TIME_SCORE_TYPE.equals(score.scoreTypeName())) {
					fullTime.add(score);
				}
			}
		}
		if (fullTime.isEmpty()) {
			return Optional.empty();
		}
		if (fullTime.size() > 1) {
			throw new TippmixResultNormalizationException(
					event.eventId(), "Tippmix result contains multiple FT score entries");
		}
		TippmixScoreResultDto ft = fullTime.getFirst();
		if (ft.scoreTypeNo() != null && ft.scoreTypeNo() != FULL_TIME_SCORE_TYPE_NO) {
			throw new TippmixResultNormalizationException(
					event.eventId(),
					"Tippmix FT scoreTypeNo is " + ft.scoreTypeNo() + ", expected " + FULL_TIME_SCORE_TYPE_NO);
		}
		if (!Boolean.FALSE.equals(ft.isCancelled())) {
			return Optional.empty();
		}
		return Optional.of(ft);
	}

	private MatchScore toMatchScore(Long eventId, TippmixScoreResultDto ft) {
		int homeGoals = requireWholeGoal(eventId, ft.scoreParticipant1(), "home");
		int awayGoals = requireWholeGoal(eventId, ft.scoreParticipant2(), "away");
		return new MatchScore(homeGoals, awayGoals);
	}

	private int requireWholeGoal(Long eventId, BigDecimal value, String side) {
		if (value == null) {
			throw new TippmixResultNormalizationException(eventId, "Tippmix FT " + side + " score is missing");
		}
		if (value.signum() < 0) {
			throw new TippmixResultNormalizationException(eventId, "Tippmix FT " + side + " score is negative");
		}
		try {
			return value.intValueExact();
		}
		catch (ArithmeticException ex) {
			throw new TippmixResultNormalizationException(
					eventId, "Tippmix FT " + side + " score is not a whole number: " + value.toPlainString());
		}
	}

}
