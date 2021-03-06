package com.github.obase;

public class WrappedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public WrappedException(Throwable t) {
		super(t);
	}

	public WrappedException(String msg, Throwable t) {
		super(msg, t);
	}

}
