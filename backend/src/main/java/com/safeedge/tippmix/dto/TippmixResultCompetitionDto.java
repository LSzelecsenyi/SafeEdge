package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixResultCompetitionDto(
		Integer sportId,
		String sportName,
		Long competitionId,
		Integer competitionType,
		String competitionName,
		List<TippmixResultEventDto> events) {
}
