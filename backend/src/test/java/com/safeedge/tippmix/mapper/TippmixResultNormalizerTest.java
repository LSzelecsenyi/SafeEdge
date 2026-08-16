package com.safeedge.tippmix.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.event.domain.BettingProviders;
import com.safeedge.result.domain.MatchResult;
import com.safeedge.tippmix.dto.TippmixResultEventDto;
import com.safeedge.tippmix.dto.TippmixScoreResultDto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TippmixResultNormalizerTest {

	private static final OffsetDateTime EVENT_DATE = OffsetDateTime.parse("2026-08-16T14:00:00+02:00");

	private final TippmixResultNormalizer normalizer = new TippmixResultNormalizer();

	@Test
	void validFtScore_normalizesToMatchScoreTwoOne() {
		Optional<MatchResult> result = normalizer.normalize(event(
				"ended",
				List.of(score("HT", 2, "1.0", "0.0", false), score("FT", 1, "2.0", "1.0", false))));

		assertThat(result).isPresent();
		assertThat(result.get().provider()).isEqualTo(BettingProviders.TIPPMIX);
		assertThat(result.get().externalEventId()).isEqualTo("5306177");
		assertThat(result.get().betradarId()).isEqualTo(68306982L);
		assertThat(result.get().finalScore().homeGoals()).isEqualTo(2);
		assertThat(result.get().finalScore().awayGoals()).isEqualTo(1);
		assertThat(result.get().eventDate()).isEqualTo(EVENT_DATE.toInstant());
	}

	@Test
	void nonEnded_isSkipped() {
		assertThat(normalizer.normalize(event("live", List.of(ft("2.0", "1.0", false))))).isEmpty();
	}

	@Test
	void missingFt_isSkipped() {
		assertThat(normalizer.normalize(event("ended", List.of(score("HT", 2, "1.0", "0.0", false))))).isEmpty();
	}

	@Test
	void cancelledFt_isSkipped() {
		assertThat(normalizer.normalize(event("ended", List.of(ft("2.0", "1.0", true))))).isEmpty();
	}

	@Test
	void nonFootball_isSkipped() {
		TippmixResultEventDto event = new TippmixResultEventDto(
				68306982L,
				5306177L,
				"Some match",
				EVENT_DATE,
				2,
				"Tennis",
				"ended",
				List.of(ft("2.0", "1.0", false)));
		assertThat(normalizer.normalize(event)).isEmpty();
	}

	@Test
	void fractionalScore_isInvalid() {
		assertThatThrownBy(() -> normalizer.normalize(event("ended", List.of(ft("2.5", "1.0", false)))))
				.isInstanceOf(TippmixResultNormalizationException.class)
				.hasMessageContaining("whole number");
	}

	@Test
	void negativeScore_isInvalid() {
		assertThatThrownBy(() -> normalizer.normalize(event("ended", List.of(ft("-1", "1.0", false)))))
				.isInstanceOf(TippmixResultNormalizationException.class)
				.hasMessageContaining("negative");
	}

	@Test
	void nullFtScore_isInvalid() {
		assertThatThrownBy(() -> normalizer.normalize(event(
						"ended",
						List.of(new TippmixScoreResultDto(1, "FT", null, new BigDecimal("1.0"), false)))))
				.isInstanceOf(TippmixResultNormalizationException.class)
				.hasMessageContaining("missing");
	}

	@Test
	void multipleConflictingFt_fails() {
		assertThatThrownBy(() -> normalizer.normalize(event(
						"ended",
						List.of(ft("2.0", "1.0", false), score("FT", 1, "1.0", "1.0", false)))))
				.isInstanceOf(TippmixResultNormalizationException.class)
				.hasMessageContaining("multiple FT");
	}

	@Test
	void multipleConsistentFt_fails() {
		assertThatThrownBy(() -> normalizer.normalize(event(
						"ended",
						List.of(ft("2.0", "1.0", false), score("FT", 1, "2.0", "1.0", false)))))
				.isInstanceOf(TippmixResultNormalizationException.class)
				.hasMessageContaining("multiple FT");
	}

	@Test
	void ftWithUnexpectedScoreTypeNo_fails() {
		assertThatThrownBy(() -> normalizer.normalize(event("ended", List.of(score("FT", 9, "2.0", "1.0", false)))))
				.isInstanceOf(TippmixResultNormalizationException.class)
				.hasMessageContaining("scoreTypeNo");
	}

	private static TippmixResultEventDto event(String matchStatus, List<TippmixScoreResultDto> scores) {
		return new TippmixResultEventDto(
				68306982L,
				5306177L,
				"Grindavik - Throttur Reykjavik",
				EVENT_DATE,
				1,
				"Football",
				matchStatus,
				scores);
	}

	private static TippmixScoreResultDto ft(String home, String away, boolean cancelled) {
		return score("FT", 1, home, away, cancelled);
	}

	private static TippmixScoreResultDto score(
			String typeName, Integer typeNo, String home, String away, boolean cancelled) {
		return new TippmixScoreResultDto(
				typeNo, typeName, new BigDecimal(home), new BigDecimal(away), cancelled);
	}

}
