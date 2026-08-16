package com.safeedge.event.domain;

import java.time.Instant;

public record BettingEvent(
		String provider,
		String externalEventId,
		Long betradarId,
		String name,
		Instant startTime,
		String competitionExternalId,
		String competitionName,
		String homeParticipantExternalId,
		String homeParticipantName,
		String awayParticipantExternalId,
		String awayParticipantName) {
}
