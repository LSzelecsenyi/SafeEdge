package com.safeedge.tippmix.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.safeedge.tippmix.dto.TippmixResultEventDto;
import com.safeedge.tippmix.dto.TippmixResultRequest;
import com.safeedge.tippmix.dto.TippmixResultResponse;
import com.safeedge.tippmix.dto.TippmixScoreResultDto;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestClientTippmixResultClientTest {

	private static final String BASE_URL = "https://api.tippmix.hu";

	private MockRestServiceServer server;
	private TippmixResultClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		client = new RestClientTippmixResultClient(builder.build());
	}

	@Test
	void fetchResults_postsVerifiedRequestAndDeserializesNestedFtScore() {
		server.expect(requestTo(BASE_URL + "/tippmix/result"))
				.andExpect(method(POST))
				.andExpect(content().json(fixture("results-request.json"), JsonCompareMode.STRICT))
				.andRespond(withSuccess(fixture("results-response.json"), MediaType.APPLICATION_JSON));

		TippmixResultResponse response = client.fetchResults(TippmixResultRequest.verifiedFootballResults());

		server.verify();
		assertThat(response.date()).isEqualTo("2026-08-16");
		assertThat(response.data()).hasSize(1);
		assertThat(response.data().getFirst().date()).isEqualTo("2026-08-16");
		assertThat(response.data().getFirst().sportCompetitions()).hasSize(1);

		TippmixResultEventDto event = response.data().getFirst().sportCompetitions().getFirst().events().getFirst();
		assertThat(event.eventId()).isEqualTo(5306177L);
		assertThat(event.betradarId()).isEqualTo(68306982L);
		assertThat(event.eventName()).isEqualTo("Grindavik - Throttur Reykjavik");
		assertThat(event.eventDate()).isEqualTo(OffsetDateTime.parse("2026-08-16T14:00:00+02:00"));
		assertThat(event.sportId()).isEqualTo(1);
		assertThat(event.matchStatus()).isEqualTo("ended");
		assertThat(event.scoreResults()).hasSize(2);

		TippmixScoreResultDto ft = event.scoreResults().get(1);
		assertThat(ft.scoreTypeNo()).isEqualTo(1);
		assertThat(ft.scoreTypeName()).isEqualTo("FT");
		assertThat(ft.scoreParticipant1()).isEqualByComparingTo("2.0");
		assertThat(ft.scoreParticipant2()).isEqualByComparingTo("1.0");
		assertThat(ft.isCancelled()).isFalse();
	}

	private static String fixture(String name) {
		try {
			return new ClassPathResource("tippmix/" + name).getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
