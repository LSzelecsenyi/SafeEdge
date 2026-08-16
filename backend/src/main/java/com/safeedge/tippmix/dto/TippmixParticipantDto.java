package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixParticipantDto(Long participantId, String participantName) {
}
