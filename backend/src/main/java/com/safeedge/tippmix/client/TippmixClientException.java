package com.safeedge.tippmix.client;

public class TippmixClientException extends RuntimeException {

	public enum FailureType {
		TRANSPORT,
		INVALID_RESPONSE
	}

	private final FailureType failureType;
	private final String operation;
	private final Long eventId;
	private final Integer httpStatus;

	public TippmixClientException(
			FailureType failureType,
			String operation,
			Long eventId,
			Integer httpStatus,
			String message,
			Throwable cause) {
		super(message, cause);
		this.failureType = failureType;
		this.operation = operation;
		this.eventId = eventId;
		this.httpStatus = httpStatus;
	}

	public FailureType getFailureType() {
		return failureType;
	}

	public String getOperation() {
		return operation;
	}

	public Long getEventId() {
		return eventId;
	}

	public Integer getHttpStatus() {
		return httpStatus;
	}

}
