package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixScoreResultDto(
		Integer scoreTypeNo,
		String scoreTypeName,
		BigDecimal scoreParticipant1,
		BigDecimal scoreParticipant2,
		Boolean isCancelled) {
}
