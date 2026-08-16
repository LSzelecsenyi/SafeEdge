package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixResultDateDto(String date, List<TippmixResultCompetitionDto> sportCompetitions) {
}
