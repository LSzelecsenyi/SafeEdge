package com.safeedge.tippmix.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.safeedge.tippmix.dto.TippmixEventDto;
import com.safeedge.tippmix.dto.TippmixEventResponse;
import com.safeedge.tippmix.dto.TippmixEventsRequest;
import com.safeedge.tippmix.dto.TippmixEventsResponse;
import com.safeedge.tippmix.dto.TippmixMarketDto;
import com.safeedge.tippmix.dto.TippmixMarketGroupDto;
import com.safeedge.tippmix.dto.TippmixOutcomeDto;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestClientTippmixClientTest {

	private static final String BASE_URL = "https://api.tippmix.hu";

	private MockRestServiceServer server;
	private TippmixClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		client = new RestClientTippmixClient(builder.build());
	}

	@Test
	void searchEvents_postsExpectedRequestAndDeserializesEventAndPagination() {
		server.expect(requestTo(BASE_URL + "/v2/tippmix/events?compatibility=v1"))
				.andExpect(method(POST))
				.andExpect(queryParam("compatibility", "v1"))
				.andExpect(content().json(fixture("events-search-request.json"), JsonCompareMode.STRICT))
				.andRespond(withSuccess(fixture("events-search-response.json"), MediaType.APPLICATION_JSON));

		TippmixEventsResponse response = client.searchEvents(footballSearchRequest());

		server.verify();
		assertThat(response.meta().totalCount()).isEqualTo(1);
		assertThat(response.meta().pageCount()).isEqualTo(1);
		assertThat(response.meta().currentPage()).isEqualTo(1);
		assertThat(response.meta().pageSize()).isEqualTo(20);

		TippmixEventDto event = response.events().getFirst();
		assertThat(event.eventId()).isEqualTo(5311343L);
		assertThat(event.betradarId()).isEqualTo(72409632L);
		assertThat(event.eventDate()).isEqualTo(OffsetDateTime.parse("2026-08-16T14:00:00+02:00"));
		assertThat(event.eventName()).isEqualTo("Djurgarden - AIK Stockholm");
		assertThat(event.eventParticipants()).hasSize(2);
		assertThat(event.eventParticipants().getFirst().participantName()).isEqualTo("Djurgarden");
		assertThat(event.competitionGroupId()).isEqualTo(10L);
		assertThat(event.competitionGroupRefId()).isEqualTo(11L);
		assertThat(event.competitionId()).isEqualTo(20L);
		assertThat(event.sportId()).isEqualTo(1);
		assertThat(event.isLive()).isZero();
		assertThat(event.hasVisiblePrematchMarket()).isTrue();
		assertThat(event.remainingPrematchMarketCount()).isEqualTo(87);
		assertThat(event.totalMarketCount()).isEqualTo(87);

		TippmixMarketDto market = event.markets().getFirst();
		assertThat(market.marketName()).isEqualTo("1X2");
		assertThat(market.outcomes()).hasSize(1);
		assertThat(market.outcomes().getFirst().outcomeName()).isEqualTo("home");
		assertThat(market.outcomes().getFirst().fixedOdds()).isEqualByComparingTo("2.10");
	}

	@Test
	void searchEvents_ignoresUnknownJsonFields() {
		server.expect(requestTo(BASE_URL + "/v2/tippmix/events?compatibility=v1"))
				.andExpect(method(POST))
				.andRespond(withSuccess(fixture("events-search-response.json"), MediaType.APPLICATION_JSON));

		TippmixEventsResponse response = client.searchEvents(footballSearchRequest());

		assertThat(response.events()).hasSize(1);
		assertThat(response.events().getFirst().eventId()).isEqualTo(5311343L);
		assertThat(response.events().getFirst().markets().getFirst().outcomes().getFirst().outcomeName())
				.isEqualTo("home");
	}

	@Test
	void getEvent_usesUngroupedPathAndDeserializesMarketsAndGroups() {
		server.expect(requestTo(BASE_URL + "/v2/tippmix/event/5311343/ungrouped?compatibility=v1"))
				.andExpect(method(GET))
				.andExpect(queryParam("compatibility", "v1"))
				.andRespond(withSuccess(fixture("event-ungrouped-response.json"), MediaType.APPLICATION_JSON));

		TippmixEventResponse response = client.getEvent(5311343L);

		server.verify();
		TippmixEventDto event = response.event();
		assertThat(event.eventId()).isEqualTo(5311343L);
		assertThat(event.markets()).hasSize(2);

		TippmixMarketDto asianHandicap = event.markets().getFirst();
		assertThat(asianHandicap.marketName()).isEqualTo("Ázsiai Hendikep -1");
		assertThat(asianHandicap.marketSubType()).isEqualTo(96);
		assertThat(asianHandicap.specialOddsValue()).isEqualTo("-1");
		assertThat(asianHandicap.marketGroupIds()).containsExactly(5, 6017);
		assertThat(asianHandicap.outcomes())
				.extracting(TippmixOutcomeDto::outcomeName)
				.containsExactly("Djurgarden -1", "AIK Stockholm +1");
		assertThat(asianHandicap.outcomes().getFirst().fixedOdds()).isEqualByComparingTo("1.57");

		assertThat(event.marketGroups())
				.extracting(TippmixMarketGroupDto::type)
				.containsExactly("all", "group", "group");
		assertThat(event.marketGroups().getFirst().id()).isNull();
		assertThat(event.marketGroups().get(1).id()).isEqualTo(5L);
		assertThat(event.marketGroups().get(1).name()).isEqualTo("Hendikep");
		assertThat(event.marketGroups().get(2).id()).isEqualTo(6017L);
		assertThat(event.marketGroups().get(2).name()).isEqualTo("Ázsiai");
	}

	@Test
	void getEvent_preservesQuarterLineSpecialOddsValueAndBigDecimalOdds() {
		server.expect(requestTo(BASE_URL + "/v2/tippmix/event/5311343/ungrouped?compatibility=v1"))
				.andExpect(method(GET))
				.andRespond(withSuccess(fixture("event-ungrouped-response.json"), MediaType.APPLICATION_JSON));

		TippmixMarketDto quarterLine = client.getEvent(5311343L).event().markets().get(1);

		assertThat(quarterLine.marketName()).isEqualTo("Ázsiai Hendikep -1,25");
		assertThat(quarterLine.specialOddsValue()).isEqualTo("-1.25");
		assertThat(quarterLine.outcomes().getFirst().fixedOdds()).isExactlyInstanceOf(BigDecimal.class);
		assertThat(quarterLine.outcomes().getFirst().fixedOdds()).isEqualByComparingTo("1.80");
		assertThat(quarterLine.outcomes().get(1).fixedOdds()).isEqualByComparingTo("1.90");
	}

	private static TippmixEventsRequest footballSearchRequest() {
		return new TippmixEventsRequest(
				"",
				1,
				null,
				null,
				List.of(),
				List.of(),
				null,
				null,
				null,
				null,
				1,
				20);
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
