package com.luluroute.ms.carrier.fedex.util;

import java.util.Arrays;
import java.util.List;

public enum OrderType {

    ALLOC,
    REPLEN,
    SPECIAL,
    TRUNK,
    ECOMM,
    STRAT;

    public static boolean isRetailOrder(String orderType) {
        List<String> retailOrderTypeList = Arrays.asList(ALLOC.name(), REPLEN.name(), SPECIAL.name(), TRUNK.name());
        return retailOrderTypeList.stream().anyMatch(orderType::equalsIgnoreCase);
    }

    public static boolean isEcommOrStratOrder(String orderType) {
        List<String> retailOrderTypeList = Arrays.asList(ECOMM.name(), STRAT.name());
        return retailOrderTypeList.stream().anyMatch(orderType::equalsIgnoreCase);
    }

    public static boolean isEcommOrder(String orderType) {
        return ECOMM.name().equalsIgnoreCase(orderType);
    }

    public static boolean isStratOrder(String orderType) {
        return STRAT.name().equalsIgnoreCase(orderType);
    }
}
