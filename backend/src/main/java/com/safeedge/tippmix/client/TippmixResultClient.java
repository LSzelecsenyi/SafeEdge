package com.safeedge.tippmix.client;

import com.safeedge.tippmix.dto.TippmixResultRequest;
import com.safeedge.tippmix.dto.TippmixResultResponse;

public interface TippmixResultClient {

	TippmixResultResponse fetchResults(TippmixResultRequest request);

}
