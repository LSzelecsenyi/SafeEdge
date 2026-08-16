package com.safeedge.result.domain;

import com.safeedge.settlement.MatchScore;
import java.time.Instant;

public record MatchResult(
		String provider,
		String externalEventId,
		Long betradarId,
		MatchScore finalScore,
		Instant eventDate) {
}
