package com.safeedge.historical.footballdata.client;

import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.footballdata.mapper.FootballDataLeague;

public interface FootballDataClient {

	String fetchSeason(FootballDataLeague league, FootballSeason season);

}
