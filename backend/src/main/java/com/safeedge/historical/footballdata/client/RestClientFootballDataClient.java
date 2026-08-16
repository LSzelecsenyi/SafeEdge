package com.safeedge.historical.footballdata.client;

import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.footballdata.mapper.FootballDataLeague;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class RestClientFootballDataClient implements FootballDataClient {

	private static final Logger log = LoggerFactory.getLogger(RestClientFootballDataClient.class);

	private final RestClient restClient;

	public RestClientFootballDataClient(RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public String fetchSeason(FootballDataLeague league, FootballSeason season) {
		String path = FootballDataPaths.csvPath(league, season);
		try {
			String body = restClient.get()
					.uri(path)
					.accept(MediaType.parseMediaType("text/csv"), MediaType.TEXT_PLAIN, MediaType.ALL)
					.retrieve()
					.body(String.class);
			if (body == null || body.isBlank()) {
				throw new FootballDataClientException(
						FootballDataClientException.FailureType.INVALID_RESPONSE,
						"football-data.co.uk CSV body was empty for " + path,
						null);
			}
			return body;
		}
		catch (FootballDataClientException ex) {
			throw ex;
		}
		catch (RestClientResponseException ex) {
			int status = ex.getStatusCode().value();
			log.warn("football-data.co.uk HTTP failure: path={} httpStatus={}", path, status);
			FootballDataClientException.FailureType type = status == 404
					? FootballDataClientException.FailureType.NOT_FOUND
					: FootballDataClientException.FailureType.TRANSPORT;
			throw new FootballDataClientException(type, "football-data.co.uk HTTP failure for " + path, ex);
		}
		catch (ResourceAccessException ex) {
			log.warn("football-data.co.uk transport failure: path={}", path);
			throw new FootballDataClientException(
					FootballDataClientException.FailureType.TRANSPORT,
					"football-data.co.uk transport failure for " + path,
					ex);
		}
		catch (RestClientException ex) {
			log.warn("football-data.co.uk invalid response: path={}", path);
			throw new FootballDataClientException(
					FootballDataClientException.FailureType.INVALID_RESPONSE,
					"football-data.co.uk response could not be read for " + path,
					ex);
		}
	}

}
