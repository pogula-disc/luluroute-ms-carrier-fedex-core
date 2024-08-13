package com.luluroute.ms.carrier.fedex.fuseapi.service.helper;

import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.carriermain.AccountDetails;
import com.logistics.luluroute.redis.shipment.carriermain.CarrierMainPayload;
import com.logistics.luluroute.redis.shipment.carriermain.TransitModes;
import com.luluroute.ms.carrier.config.FedExModeConfig;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.fedex.util.Constants;
import com.luluroute.ms.carrier.fedex.util.ExceptionConstants;
import com.luluroute.ms.carrier.fedex.util.OrderType;
import com.luluroute.ms.carrier.model.FedExServiceInfo;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import static com.luluroute.ms.carrier.fedex.util.Constants.LABEL_DD_MM_YYYY_FORMAT;
import static com.luluroute.ms.carrier.fedex.util.Constants.LABEL_REF_LPN_ID;
import static com.luluroute.ms.carrier.fedex.util.Constants.LABEL_TRACKER_ID;
import static com.luluroute.ms.carrier.fedex.util.Constants.STRING_EMPTY;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.CODE_MODE_NOT_AVAILABLE;


public class CustomLabelHelper {

    public static String formatCity(String city) {
        return Constants.usingSubstringMethod(city, 30);
    }

    /**
     * @param carrierProfile
     * @param carrierModeCode
     * @param accountToBill
     * @return
     */
    public static String getGenKeyAkaDispatchId(CarrierMainPayload carrierProfile, String carrierModeCode,
                                                String accountToBill) {
        for (TransitModes transitMode : carrierProfile.getTransitModes()) {
            if (transitMode.getModeCode().equalsIgnoreCase(carrierModeCode))
                for (AccountDetails accountDetails : transitMode.getAccounts()) {
                    if (accountDetails.getAccountNo().equalsIgnoreCase(accountToBill))
                        return accountDetails.getGenkey();
                }
        }
        return STRING_EMPTY;
    }


    public static String formatShipDate(long val) {
        return new SimpleDateFormat(LABEL_DD_MM_YYYY_FORMAT).format(new Date((val) * 1000));
    }

    /**
     * @param val
     * @return
     */
    public static String formatZipCode(String val) {
        if (!StringUtils.isEmpty(val))
            return val.substring(0, 4);

        return val;
    }

    /**
     * @param val
     * @return
     */
    public static String formatPhoneNbr(String val) {
        if (!StringUtils.isEmpty(val))
            return val.replaceFirst("(\\d{3})(\\d{3})(\\d+)", "$1-$2-$3");
        return STRING_EMPTY;
    }

    /**
     * Check NULL and convert to UPPER
     *
     * @param val : Input
     * @return : UPPER Case value
     */
    public static String toUpper(String val) {
        if (!StringUtils.isEmpty(val))
            return val.toUpperCase();

        return STRING_EMPTY;
    }

    public static String getDefaultWithValue(String val) {
        if (!StringUtils.isEmpty(val))
            return val;

        return STRING_EMPTY;
    }


    public static String createShipperRefLPNBarcode(String referenceLPN) {
        return String.format(LABEL_REF_LPN_ID, StringUtils.rightPad(referenceLPN, 10));
    }

    public static String getPO(ShipmentInfo shipmentInfo) {
        String orderType = shipmentInfo.getOrderDetails().getOrderType();
        String poValue = STRING_EMPTY;
        if (OrderType.isRetailOrder(orderType)) {
            poValue = shipmentInfo.getOrderDetails().getShipVia();
        } else if (OrderType.isEcommOrder(orderType)) {
            poValue = shipmentInfo.getOrderDetails().getTclpnid();
        } else if (OrderType.isStratOrder(orderType)) {
            poValue = shipmentInfo.getOrderDetails().getLaneName();
        }
        return toUpper(poValue);
    }

    public static String getINV(ShipmentInfo shipmentInfo) {
        String orderType = shipmentInfo.getOrderDetails().getOrderType();
        String invValue = STRING_EMPTY;
        if (OrderType.isRetailOrder(orderType) || OrderType.isStratOrder(orderType)) {
            invValue = shipmentInfo.getOrderDetails().getTclpnid();
        } else if (OrderType.isEcommOrder(orderType)) {
            invValue = orderType;
        }
        return toUpper(invValue);
    }

    public static String createShipperTrackerBarcode(String entityCode, String referenceLPN) {
        return String.format(LABEL_TRACKER_ID, StringUtils.leftPad(entityCode, 5, "0"),
                StringUtils.rightPad(referenceLPN, 10));
    }

    public static FedExServiceInfo getServiceDetailsByMode(FedExModeConfig fedExModeConfig, String modeCode) throws ShipmentMessageException {
        return fedExModeConfig.getModes().stream().filter(fedExServiceInfo -> fedExServiceInfo.getTransitModes().contains(modeCode))
                .findAny().orElseThrow(() -> new ShipmentMessageException(CODE_MODE_NOT_AVAILABLE, "Targeted mode is not configured in FEDEX for Label service",
                        ExceptionConstants.CODE_LABEL_SOURCE));
    }

    public static Boolean isInternational(ShipmentInfo shipmentInfo) {
        String originCountry = shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom().getCountry();
        String destCountry = shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getCountry();
        if (shipmentInfo.getOrderDetails().isINTL ||
                !originCountry.equals(destCountry)) {
            return true;
        }
        return false;
    }

}
