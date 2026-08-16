package com.safeedge.historical.footballdata.client;

public class FootballDataClientException extends RuntimeException {

	public enum FailureType {
		TRANSPORT,
		INVALID_RESPONSE
	}

	private final FailureType failureType;

	public FootballDataClientException(FailureType failureType, String message, Throwable cause) {
		super(message, cause);
		this.failureType = failureType;
	}

	public FailureType failureType() {
		return failureType;
	}

}
