package com.safeedge.tippmix.mapper;

import com.safeedge.event.domain.BettingEvent;
import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingOffer;
import com.safeedge.event.domain.BettingProviders;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.tippmix.dto.TippmixEventDto;
import com.safeedge.tippmix.dto.TippmixMarketDto;
import com.safeedge.tippmix.dto.TippmixOutcomeDto;
import com.safeedge.tippmix.dto.TippmixParticipantDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TippmixBettingOfferNormalizer {

	public static final int FOOTBALL_SPORT_ID = 1;
	static final int PRE_MATCH_MARKET_TYPE = 2;
	static final int ASIAN_HANDICAP_SUB_TYPE = 96;
	static final int EUROPEAN_HANDICAP_SUB_TYPE = 100;
	static final int DOUBLE_CHANCE_SUB_TYPE = 92;
	static final int HANDICAP_GROUP_ID = 5;
	static final int ASIAN_GROUP_ID = 6017;
	static final String DRAW_OUTCOME_NAME = "Döntetlen";
	static final String DOUBLE_CHANCE_CONJUNCTION = " vagy ";

	public BettingOffer normalize(TippmixEventDto event) {
		if (event == null) {
			throw new TippmixNormalizationException(null, null, "Tippmix event payload is missing");
		}
		Participants participants = requireParticipants(event);
		BettingEvent bettingEvent = toBettingEvent(event, participants);
		List<BettingMarket> markets = new ArrayList<>();
		if (event.markets() != null) {
			for (TippmixMarketDto market : event.markets()) {
				classify(market).ifPresent(type -> markets.add(normalizeMarket(event, participants, market, type)));
			}
		}
		return new BettingOffer(bettingEvent, List.copyOf(markets));
	}

	private BettingEvent toBettingEvent(TippmixEventDto event, Participants participants) {
		if (event.eventId() == null) {
			throw new TippmixNormalizationException(null, null, "Tippmix eventId is required");
		}
		if (event.eventName() == null || event.eventName().isBlank()) {
			throw fail(event, null, "Tippmix eventName is required");
		}
		if (event.eventDate() == null) {
			throw fail(event, null, "Tippmix eventDate is required");
		}
		if (event.competitionId() == null || event.competitionName() == null || event.competitionName().isBlank()) {
			throw fail(event, null, "Tippmix competition id and name are required");
		}
		return new BettingEvent(
				BettingProviders.TIPPMIX,
				String.valueOf(event.eventId()),
				event.betradarId(),
				event.eventName(),
				event.eventDate().toInstant(),
				String.valueOf(event.competitionId()),
				event.competitionName(),
				participants.homeExternalId(),
				participants.homeName(),
				participants.awayExternalId(),
				participants.awayName());
	}

	private BettingMarket normalizeMarket(
			TippmixEventDto event,
			Participants participants,
			TippmixMarketDto market,
			MarketType type) {
		return switch (type) {
			case ASIAN_HANDICAP -> normalizeAsianHandicap(event, participants, market);
			case EUROPEAN_HANDICAP -> normalizeEuropeanHandicap(event, participants, market);
			case DOUBLE_CHANCE -> normalizeDoubleChance(event, participants, market);
		};
	}

	private BettingMarket normalizeAsianHandicap(
			TippmixEventDto event,
			Participants participants,
			TippmixMarketDto market) {
		List<TippmixOutcomeDto> outcomes = requireOutcomes(event, market, 2);
		BigDecimal homeLine = parseLine(event, market);
		BigDecimal awayLine = homeLine.negate();
		TippmixOutcomeDto homeOutcome = outcomes.get(0);
		TippmixOutcomeDto awayOutcome = outcomes.get(1);
		requireNamePrefix(event, market, homeOutcome, participants.homeName(), "home");
		requireNamePrefix(event, market, awayOutcome, participants.awayName(), "away");
		return market(
				market,
				MarketType.ASIAN_HANDICAP,
				homeLine,
				List.of(
						selection(market, homeOutcome, SelectionType.HOME, homeLine),
						selection(market, awayOutcome, SelectionType.AWAY, awayLine)));
	}

	private BettingMarket normalizeEuropeanHandicap(
			TippmixEventDto event,
			Participants participants,
			TippmixMarketDto market) {
		List<TippmixOutcomeDto> outcomes = requireOutcomes(event, market, 3);
		BigDecimal line = parseLine(event, market);
		TippmixOutcomeDto home = requireOutcomeNo(event, market, outcomes, 1);
		TippmixOutcomeDto draw = requireOutcomeNo(event, market, outcomes, 2);
		TippmixOutcomeDto away = requireOutcomeNo(event, market, outcomes, 3);
		requireExactOutcomeName(event, market, home, participants.homeName(), "home");
		requireExactOutcomeName(event, market, draw, DRAW_OUTCOME_NAME, "draw");
		requireExactOutcomeName(event, market, away, participants.awayName(), "away");
		return market(
				market,
				MarketType.EUROPEAN_HANDICAP,
				line,
				List.of(
						selection(market, home, SelectionType.HOME, null),
						selection(market, draw, SelectionType.DRAW, null),
						selection(market, away, SelectionType.AWAY, null)));
	}

	private BettingMarket normalizeDoubleChance(
			TippmixEventDto event,
			Participants participants,
			TippmixMarketDto market) {
		List<TippmixOutcomeDto> outcomes = requireOutcomes(event, market, 3);
		TippmixOutcomeDto homeOrDraw = requireOutcomeNo(event, market, outcomes, 1);
		TippmixOutcomeDto homeOrAway = requireOutcomeNo(event, market, outcomes, 2);
		TippmixOutcomeDto drawOrAway = requireOutcomeNo(event, market, outcomes, 3);
		requireExactOutcomeName(
				event,
				market,
				homeOrDraw,
				doubleChanceName(participants.homeName(), DRAW_OUTCOME_NAME),
				"home or draw");
		requireExactOutcomeName(
				event,
				market,
				homeOrAway,
				doubleChanceName(participants.homeName(), participants.awayName()),
				"home or away");
		requireExactOutcomeName(
				event,
				market,
				drawOrAway,
				doubleChanceName(DRAW_OUTCOME_NAME, participants.awayName()),
				"draw or away");
		return market(
				market,
				MarketType.DOUBLE_CHANCE,
				null,
				List.of(
						selection(market, homeOrDraw, SelectionType.HOME_OR_DRAW, null),
						selection(market, homeOrAway, SelectionType.HOME_OR_AWAY, null),
						selection(market, drawOrAway, SelectionType.DRAW_OR_AWAY, null)));
	}

	private TippmixOutcomeDto requireOutcomeNo(
			TippmixEventDto event,
			TippmixMarketDto market,
			List<TippmixOutcomeDto> outcomes,
			int outcomeNo) {
		List<TippmixOutcomeDto> matches = new ArrayList<>();
		for (TippmixOutcomeDto outcome : outcomes) {
			if (Integer.valueOf(outcomeNo).equals(outcome.outcomeNo())) {
				matches.add(outcome);
			}
		}
		if (matches.size() != 1) {
			throw fail(event, market, "Tippmix market must contain exactly one outcomeNo=" + outcomeNo);
		}
		return matches.getFirst();
	}

	private void requireExactOutcomeName(
			TippmixEventDto event,
			TippmixMarketDto market,
			TippmixOutcomeDto outcome,
			String expectedName,
			String role) {
		if (outcome.outcomeName() == null || !outcome.outcomeName().equals(expectedName)) {
			throw fail(
					event,
					market,
					"Tippmix " + role + " outcome name does not match '" + expectedName + "'");
		}
	}

	private static String doubleChanceName(String left, String right) {
		return left + DOUBLE_CHANCE_CONJUNCTION + right;
	}

	private Optional<MarketType> classify(TippmixMarketDto market) {
		if (market == null || !Integer.valueOf(PRE_MATCH_MARKET_TYPE).equals(market.marketType())) {
			return Optional.empty();
		}
		Integer subType = market.marketSubType();
		List<Integer> groups = market.marketGroupIds() == null ? List.of() : market.marketGroupIds();
		if (Integer.valueOf(ASIAN_HANDICAP_SUB_TYPE).equals(subType)
				&& groups.contains(HANDICAP_GROUP_ID)
				&& groups.contains(ASIAN_GROUP_ID)) {
			return Optional.of(MarketType.ASIAN_HANDICAP);
		}
		if (Integer.valueOf(EUROPEAN_HANDICAP_SUB_TYPE).equals(subType)
				&& groups.contains(HANDICAP_GROUP_ID)
				&& !groups.contains(ASIAN_GROUP_ID)) {
			return Optional.of(MarketType.EUROPEAN_HANDICAP);
		}
		if (Integer.valueOf(DOUBLE_CHANCE_SUB_TYPE).equals(subType)) {
			return Optional.of(MarketType.DOUBLE_CHANCE);
		}
		return Optional.empty();
	}

	private List<TippmixOutcomeDto> requireOutcomes(TippmixEventDto event, TippmixMarketDto market, int expected) {
		if (market.outcomes() == null || market.outcomes().size() != expected) {
			throw fail(event, market, "Tippmix market must have exactly " + expected + " outcomes");
		}
		if (market.outcomeCount() != null && market.outcomeCount() != expected) {
			throw fail(event, market, "Tippmix outcomeCount does not match the required " + expected + " outcomes");
		}
		return market.outcomes();
	}

	private BigDecimal parseLine(TippmixEventDto event, TippmixMarketDto market) {
		if (market.specialOddsValue() == null || market.specialOddsValue().isBlank()) {
			throw fail(event, market, "Tippmix specialOddsValue is required");
		}
		try {
			return new BigDecimal(market.specialOddsValue().trim());
		}
		catch (NumberFormatException ex) {
			throw fail(event, market, "Tippmix specialOddsValue is not a valid decimal: " + market.specialOddsValue());
		}
	}

	private void requireNamePrefix(
			TippmixEventDto event,
			TippmixMarketDto market,
			TippmixOutcomeDto outcome,
			String participantName,
			String side) {
		if (outcome.outcomeName() == null || !outcome.outcomeName().startsWith(participantName)) {
			throw fail(
					event,
					market,
					"Tippmix " + side + " outcome name does not match participant '" + participantName + "'");
		}
	}

	private BettingMarket market(
			TippmixMarketDto source,
			MarketType type,
			BigDecimal line,
			List<BettingSelection> selections) {
		if (source.marketId() == null) {
			throw new TippmixNormalizationException(null, null, "Tippmix marketId is required");
		}
		return new BettingMarket(
				BettingProviders.TIPPMIX,
				String.valueOf(source.marketId()),
				source.marketRealNo(),
				source.marketName(),
				source.marketType(),
				source.marketSubType(),
				source.marketVersion(),
				type,
				line,
				List.copyOf(selections));
	}

	private BettingSelection selection(
			TippmixMarketDto market,
			TippmixOutcomeDto outcome,
			SelectionType type,
			BigDecimal line) {
		if (outcome.outcomeNo() == null) {
			throw new TippmixNormalizationException(null, market.marketId(), "Tippmix outcomeNo is required");
		}
		if (outcome.fixedOdds() == null) {
			throw new TippmixNormalizationException(null, market.marketId(), "Tippmix fixedOdds is required");
		}
		return new BettingSelection(
				BettingProviders.TIPPMIX,
				outcome.outcomeNo(),
				outcome.outcomeRealNo(),
				outcome.outcomeName(),
				type,
				line,
				outcome.fixedOdds());
	}

	private Participants requireParticipants(TippmixEventDto event) {
		if (event.eventParticipants() == null || event.eventParticipants().size() != 2) {
			throw fail(event, null, "Tippmix football event must have exactly two participants");
		}
		TippmixParticipantDto home = event.eventParticipants().get(0);
		TippmixParticipantDto away = event.eventParticipants().get(1);
		if (home == null || home.participantId() == null || home.participantName() == null || home.participantName().isBlank()) {
			throw fail(event, null, "Tippmix home participant is incomplete");
		}
		if (away == null || away.participantId() == null || away.participantName() == null || away.participantName().isBlank()) {
			throw fail(event, null, "Tippmix away participant is incomplete");
		}
		return new Participants(
				String.valueOf(home.participantId()),
				home.participantName(),
				String.valueOf(away.participantId()),
				away.participantName());
	}

	private RuntimeException fail(TippmixEventDto event, TippmixMarketDto market, String message) {
		return new TippmixNormalizationException(
				event == null ? null : event.eventId(),
				market == null ? null : market.marketId(),
				message);
	}

	private record Participants(
			String homeExternalId, String homeName, String awayExternalId, String awayName) {
	}

}
