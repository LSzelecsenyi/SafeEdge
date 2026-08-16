package com.safeedge.candidate;

import static com.safeedge.candidate.CandidateFixtures.score;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScoreProbabilityDistributionTest {

	@Test
	void negativeProbabilityIsRejected() {
		assertThatThrownBy(() -> new ScoreProbability(new MatchScore(1, 0), new BigDecimal("-0.01")))
				.isInstanceOf(CandidateException.class)
				.hasMessageContaining("probability");
	}

	@Test
	void probabilityAboveOneIsRejected() {
		assertThatThrownBy(() -> new ScoreProbability(new MatchScore(1, 0), new BigDecimal("1.01")))
				.isInstanceOf(CandidateException.class)
				.hasMessageContaining("probability");
	}

	@Test
	void nullScoreOrProbabilityIsRejected() {
		assertThatThrownBy(() -> new ScoreProbability(null, BigDecimal.ONE))
				.isInstanceOf(CandidateException.class)
				.hasMessageContaining("score");
		assertThatThrownBy(() -> new ScoreProbability(new MatchScore(1, 0), null))
				.isInstanceOf(CandidateException.class)
				.hasMessageContaining("probability");
	}

	@Test
	void emptyDistributionIsRejected() {
		assertThatThrownBy(() -> new ScoreProbabilityDistribution(List.of()))
				.isInstanceOf(CandidateException.class)
				.hasMessageContaining("empty");
	}

	@Test
	void nullEntryIsRejected() {
		List<ScoreProbability> entries = new ArrayList<>();
		entries.add(score(1, 0, "1"));
		entries.set(0, null);
		assertThatThrownBy(() -> new ScoreProbabilityDistribution(entries))
				.isInstanceOf(CandidateException.class)
				.hasMessageContaining("null");
	}

	@Test
	void sumBelowOneIsRejected() {
		assertThatThrownBy(() -> ScoreProbabilityDistribution.of(score(1, 0, "0.40"), score(0, 1, "0.50")))
				.isInstanceOf(CandidateException.class)
				.hasMessageContaining("sum to 1");
	}

	@Test
	void sumAboveOneIsRejected() {
		assertThatThrownBy(() -> ScoreProbabilityDistribution.of(score(1, 0, "0.60"), score(0, 1, "0.50")))
				.isInstanceOf(CandidateException.class)
				.hasMessageContaining("sum to 1");
	}

	@Test
	void duplicateScoreIsRejected() {
		assertThatThrownBy(() -> ScoreProbabilityDistribution.of(score(1, 0, "0.40"), score(1, 0, "0.60")))
				.isInstanceOf(CandidateException.class)
				.hasMessageContaining("Duplicate scoreline");
	}

	@Test
	void probabilitiesAreNotSilentlyNormalized() {
		assertThatThrownBy(() -> ScoreProbabilityDistribution.of(score(1, 0, "0.30"), score(0, 1, "0.30")))
				.isInstanceOf(CandidateException.class)
				.hasMessageContaining("sum to 1");
	}

}
