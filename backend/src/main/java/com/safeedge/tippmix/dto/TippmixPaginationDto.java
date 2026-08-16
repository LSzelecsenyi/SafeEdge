package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixPaginationDto(
		Integer totalCount,
		Integer pageCount,
		Integer currentPage,
		Integer pageSize) {
}
