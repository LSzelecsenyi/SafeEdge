package com.safeedge.tippmix.client;

import com.safeedge.tippmix.dto.TippmixEventResponse;
import com.safeedge.tippmix.dto.TippmixEventsRequest;
import com.safeedge.tippmix.dto.TippmixEventsResponse;

public interface TippmixClient {

	TippmixEventsResponse searchEvents(TippmixEventsRequest request);

	TippmixEventResponse getEvent(long eventId);

}
