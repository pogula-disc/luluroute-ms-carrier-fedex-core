package com.luluroute.ms.carrier.fedex.util;

public enum PaymentType {

    SENDER ("1"),
    RECIPIENT ("2");

    private final String value;

    PaymentType(String val) {
        this.value = val;
    }

    public String getValue() {
        return this.value;
    }
}
