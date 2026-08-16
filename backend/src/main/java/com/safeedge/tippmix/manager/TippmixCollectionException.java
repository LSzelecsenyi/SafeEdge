package com.safeedge.tippmix.manager;

public class TippmixCollectionException extends RuntimeException {

	private final int pagesFetched;

	public TippmixCollectionException(int pagesFetched, String message) {
		super(message);
		this.pagesFetched = pagesFetched;
	}

	public TippmixCollectionException(int pagesFetched, String message, Throwable cause) {
		super(message, cause);
		this.pagesFetched = pagesFetched;
	}

	public int getPagesFetched() {
		return pagesFetched;
	}

}
