package com.luluroute.ms.carrier.fedex.fuseapi.service.helper;

import static com.luluroute.ms.carrier.fedex.util.Constants.DELIMITER;

public class DCRoutingUtil {

    /**
     * Constructs Custom Key for DC Routing Lookup
     * @param destEntityCode Destination Entity Code
     * @param carrierCode Carrier Code, FEDX
     * @param carrierModeCode Carrier Mode Code
     * @return String of Custom Key
     */
    public static String getCustomRouteIdentifier(String destEntityCode, String carrierCode, String carrierModeCode) {
        return new StringBuilder(destEntityCode).append(DELIMITER)
                .append(carrierCode).append(DELIMITER).append(carrierModeCode)
                .toString();
    }
}
