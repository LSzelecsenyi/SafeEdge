package com.safeedge.historical.footballdata.parser;

import com.safeedge.historical.domain.HistoricalDataException;

public class FootballDataParseException extends HistoricalDataException {

	public FootballDataParseException(String message) {
		super(message);
	}

	public FootballDataParseException(String message, Throwable cause) {
		super(message, cause);
	}

}
