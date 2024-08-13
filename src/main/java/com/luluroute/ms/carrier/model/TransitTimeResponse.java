package com.luluroute.ms.carrier.model;

import lombok.Builder;
import lombok.Data;
/**
 * 
 * @author MANDALAKARTHIK1
 *
 */
@Data
@Builder
public class TransitTimeResponse {
	private long responseDeliveryDate;
	private String carrierMode;
	private String failureMessage;
}
