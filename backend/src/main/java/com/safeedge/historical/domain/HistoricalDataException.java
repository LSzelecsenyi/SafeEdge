package com.safeedge.historical.domain;

public class HistoricalDataException extends RuntimeException {

	public HistoricalDataException(String message) {
		super(message);
	}

	public HistoricalDataException(String message, Throwable cause) {
		super(message, cause);
	}

}
