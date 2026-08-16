package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixEventsRequest(
		String search,
		Integer sportId,
		Long competitionGroupId,
		Long competitionOrAliasId,
		List<Object> eventTypes,
		List<Object> marketTypes,
		OffsetDateTime maxDate,
		BigDecimal maxOdds,
		OffsetDateTime minDate,
		BigDecimal minOdds,
		Integer page,
		Integer pageSize) {
}
