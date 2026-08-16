package com.safeedge.result.domain;

public class MatchResultIdentityException extends RuntimeException {

	private final String externalEventId;

	public MatchResultIdentityException(String externalEventId, String message) {
		super(message);
		this.externalEventId = externalEventId;
	}

	public String getExternalEventId() {
		return externalEventId;
	}

}
