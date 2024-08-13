package com.luluroute.ms.carrier.fedex.exception;

/**
 * 
 * @author MANDALAKARTHIK1
 *
 */
public class DefaultTransitTimeFailureException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DefaultTransitTimeFailureException() {
		super();
	}

	public DefaultTransitTimeFailureException(String message) {
		super(message);
	}

	public DefaultTransitTimeFailureException(Throwable cause) {
		super(cause);
	}

	public DefaultTransitTimeFailureException(String message, Throwable cause) {
		super(message, cause);
	}
}
