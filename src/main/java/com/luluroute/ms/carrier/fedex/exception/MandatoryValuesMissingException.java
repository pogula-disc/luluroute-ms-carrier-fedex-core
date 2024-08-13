package com.luluroute.ms.carrier.fedex.exception;
/**
 * 
 * @author MANDALAKARTHIK1
 *
 */
public class MandatoryValuesMissingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MandatoryValuesMissingException() {
	        super();
	    }

	public MandatoryValuesMissingException(String message) {
	        super(message);
	    }

	public MandatoryValuesMissingException(Throwable cause) {
	        super(cause);
	    }

	public MandatoryValuesMissingException(String message, Throwable cause) {
	        super(message, cause);
	    }

}
