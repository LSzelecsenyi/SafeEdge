package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixEventsResponse(
		List<TippmixEventDto> events,
		@JsonProperty("_meta") TippmixPaginationDto meta) {
}
