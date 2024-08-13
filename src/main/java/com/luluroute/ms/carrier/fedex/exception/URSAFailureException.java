package com.luluroute.ms.carrier.fedex.exception;

/**
 * 
 * @author MANDALAKARTHIK1
 *
 */
public class URSAFailureException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public URSAFailureException() {
		super();
	}

	public URSAFailureException(String message) {
		super(message);
	}

	public URSAFailureException(Throwable cause) {
		super(cause);
	}

	public URSAFailureException(String message, Throwable cause) {
		super(message, cause);
	}
}
