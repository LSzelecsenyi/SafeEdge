package com.safeedge.tippmix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TippmixEventDto(
		Long eventId,
		Long betradarId,
		OffsetDateTime eventDate,
		String eventName,
		List<TippmixParticipantDto> eventParticipants,
		Long competitionGroupId,
		Long competitionGroupRefId,
		String competitionGroupName,
		Long competitionId,
		Long competitionRefId,
		String competitionName,
		Integer sportId,
		String sportName,
		Integer isLive,
		Integer isOutright,
		Boolean hasVisibleLiveMarket,
		Boolean hasVisiblePrematchMarket,
		Integer remainingLiveMarketCount,
		Integer remainingPrematchMarketCount,
		Integer totalMarketCount,
		Integer bettingPhase,
		Integer bettingStatus,
		Integer eventVersion,
		List<TippmixMarketDto> markets,
		List<TippmixMarketGroupDto> marketGroups) {
}
