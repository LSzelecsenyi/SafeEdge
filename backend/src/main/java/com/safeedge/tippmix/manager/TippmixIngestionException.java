package com.safeedge.tippmix.manager;

public class TippmixIngestionException extends RuntimeException {

	public enum Reason {
		NOT_FOOTBALL,
		NOT_PREMATCH,
		NO_VISIBLE_PREMATCH_MARKET
	}

	private final Long tippmixEventId;
	private final Reason reason;

	public TippmixIngestionException(Long tippmixEventId, String message) {
		this(tippmixEventId, message, null);
	}

	public TippmixIngestionException(Long tippmixEventId, String message, Reason reason) {
		super(message);
		this.tippmixEventId = tippmixEventId;
		this.reason = reason;
	}

	public Long getTippmixEventId() {
		return tippmixEventId;
	}

	public Reason getReason() {
		return reason;
	}

	public boolean isNoLongerPrematch() {
		return reason == Reason.NOT_PREMATCH || reason == Reason.NO_VISIBLE_PREMATCH_MARKET;
	}

}
