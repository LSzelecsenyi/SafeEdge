package com.safeedge.tippmix.client;

import com.safeedge.tippmix.client.TippmixClientException.FailureType;
import com.safeedge.tippmix.dto.TippmixResultRequest;
import com.safeedge.tippmix.dto.TippmixResultResponse;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class RestClientTippmixResultClient implements TippmixResultClient {

	private static final Logger log = LoggerFactory.getLogger(RestClientTippmixResultClient.class);
	private static final String PROVIDER = "Tippmix";
	private static final String OPERATION = "fetchResults";

	private final RestClient restClient;

	public RestClientTippmixResultClient(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public TippmixResultResponse fetchResults(TippmixResultRequest request) {
		return execute(() -> restClient.post()
				.uri("/tippmix/result")
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(TippmixResultResponse.class));
	}

	private TippmixResultResponse execute(Supplier<TippmixResultResponse> call) {
		try {
			TippmixResultResponse body = call.get();
			if (body == null) {
				log.warn("Tippmix empty response: provider={} operation={}", PROVIDER, OPERATION);
				throw new TippmixClientException(
						FailureType.INVALID_RESPONSE,
						OPERATION,
						null,
						null,
						"Tippmix result response body was empty",
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
					"Tippmix HTTP failure: provider={} operation={} httpStatus={}",
					PROVIDER,
					OPERATION,
					status);
			throw new TippmixClientException(
					FailureType.TRANSPORT, OPERATION, null, status, "Tippmix HTTP failure", ex);
		}
		catch (ResourceAccessException ex) {
			log.warn("Tippmix transport failure: provider={} operation={}", PROVIDER, OPERATION);
			throw new TippmixClientException(
					FailureType.TRANSPORT, OPERATION, null, null, "Tippmix transport failure", ex);
		}
		catch (RestClientException ex) {
			log.warn("Tippmix invalid response: provider={} operation={}", PROVIDER, OPERATION);
			throw new TippmixClientException(
					FailureType.INVALID_RESPONSE, OPERATION, null, null, "Tippmix result response could not be read", ex);
		}
	}

}
