package com.luluroute.ms.carrier.fedex.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ShipmentMessageException extends Exception {

    String code;
    String description;
    String source;
}
