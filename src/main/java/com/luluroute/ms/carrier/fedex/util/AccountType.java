package com.luluroute.ms.carrier.fedex.util;

public enum AccountType {

    RETAIL("RETAIL"),
    RETAIL_PM("RETAIL PM"),
    ECOMM("ECOMM");

    private final String value;

    AccountType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
