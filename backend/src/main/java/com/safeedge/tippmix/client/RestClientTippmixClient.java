package com.safeedge.tippmix.client;

import com.safeedge.tippmix.client.TippmixClientException.FailureType;
import com.safeedge.tippmix.dto.TippmixEventResponse;
import com.safeedge.tippmix.dto.TippmixEventsRequest;
import com.safeedge.tippmix.dto.TippmixEventsResponse;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class RestClientTippmixClient implements TippmixClient {

	private static final Logger log = LoggerFactory.getLogger(RestClientTippmixClient.class);
	private static final String PROVIDER = "Tippmix";
	private static final String COMPATIBILITY = "v1";

	private final RestClient restClient;

	public RestClientTippmixClient(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public TippmixEventsResponse searchEvents(TippmixEventsRequest request) {
		return execute("searchEvents", null, () -> restClient.post()
				.uri(uriBuilder -> uriBuilder
						.path("/v2/tippmix/events")
						.queryParam("compatibility", COMPATIBILITY)
						.build())
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(TippmixEventsResponse.class));
	}

	@Override
	public TippmixEventResponse getEvent(long eventId) {
		return execute("getEvent", eventId, () -> restClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/v2/tippmix/event/{eventId}/ungrouped")
						.queryParam("compatibility", COMPATIBILITY)
						.build(eventId))
				.retrieve()
				.body(TippmixEventResponse.class));
	}

	private <T> T execute(String operation, Long eventId, Supplier<T> call) {
		try {
			T body = call.get();
			if (body == null) {
				log.warn(
						"Tippmix empty response: provider={} operation={} eventId={}",
						PROVIDER,
						operation,
						eventId);
				throw new TippmixClientException(
						FailureType.INVALID_RESPONSE,
						operation,
						eventId,
						null,
						"Tippmix response body was empty",
						null);
			}
			return body;
		}
		catch (TippmixClientException ex) {
			throw ex;
		}
		catch (RestClientResponseException ex) {
			int status = ex.getStatusCode().value();
			log.warn(
					"Tippmix HTTP failure: provider={} operation={} eventId={} httpStatus={}",
					PROVIDER,
					operation,
					eventId,
					status);
			throw new TippmixClientException(
					FailureType.TRANSPORT,
					operation,
					eventId,
					status,
					"Tippmix HTTP failure",
					ex);
		}
		catch (ResourceAccessException ex) {
			log.warn(
					"Tippmix transport failure: provider={} operation={} eventId={}",
					PROVIDER,
					operation,
					eventId);
			throw new TippmixClientException(
					FailureType.TRANSPORT,
					operation,
					eventId,
					null,
					"Tippmix transport failure",
					ex);
		}
		catch (RestClientException ex) {
			log.warn(
					"Tippmix invalid response: provider={} operation={} eventId={}",
					PROVIDER,
					operation,
					eventId);
			throw new TippmixClientException(
					FailureType.INVALID_RESPONSE,
					operation,
					eventId,
					null,
					"Tippmix response could not be read",
					ex);
		}
	}

}
