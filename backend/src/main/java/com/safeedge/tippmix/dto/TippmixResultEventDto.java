package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixResultEventDto(
		Long betradarId,
		Long eventId,
		String eventName,
		OffsetDateTime eventDate,
		Integer sportId,
		String sportName,
		String matchStatus,
		List<TippmixScoreResultDto> scoreResults) {
}
