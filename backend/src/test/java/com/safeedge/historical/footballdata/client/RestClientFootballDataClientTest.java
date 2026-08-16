package com.safeedge.historical.footballdata.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.footballdata.mapper.FootballDataLeague;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestClientFootballDataClientTest {

	private static final String BASE_URL = "https://www.football-data.co.uk";

	private MockRestServiceServer server;
	private FootballDataClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		client = new RestClientFootballDataClient(builder.build());
	}

	@Test
	void fetchSeasonUsesCentralizedPath() {
		server.expect(requestTo(BASE_URL + "/mmz4281/2324/E0.csv"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("Div,Date,HomeTeam,AwayTeam,FTHG,FTAG\n", MediaType.TEXT_PLAIN));
		String csv = client.fetchSeason(FootballDataLeague.E0, new FootballSeason(2023, 2024));
		server.verify();
		assertThat(csv).contains("Div");
	}

	@Test
	void fetchSeasonMapsHttp404ToNotFound() {
		server.expect(requestTo(BASE_URL + "/mmz4281/1011/E0.csv"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));
		assertThatThrownBy(() -> client.fetchSeason(FootballDataLeague.E0, new FootballSeason(2010, 2011)))
				.isInstanceOf(FootballDataClientException.class)
				.extracting(ex -> ((FootballDataClientException) ex).failureType())
				.isEqualTo(FootballDataClientException.FailureType.NOT_FOUND);
		server.verify();
	}

}
