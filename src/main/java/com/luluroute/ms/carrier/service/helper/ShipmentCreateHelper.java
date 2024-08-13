package com.luluroute.ms.carrier.service.helper;

import com.fedex.ursa.uvsdk.UVConstants;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;
import com.logistics.luluroute.redis.shipment.entity.DCRoute;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.logistics.luluroute.redis.shipment.entity.HubInfo;
import com.luluroute.ms.carrier.fedex.util.AccountType;
import com.luluroute.ms.carrier.fedex.util.OrderType;
import com.luluroute.ms.carrier.model.UrsaPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.luluroute.ms.carrier.fedex.fuseapi.service.helper.CustomLabelHelper.toUpper;
import static com.luluroute.ms.carrier.fedex.util.Constants.*;

@Slf4j
public class ShipmentCreateHelper {

    /**
     * Queries dcRouting map from DC Entity's Redis Cache
     * Checks if order type is retail (ALLOC, REPLEN, SPECIAL, TRUNK)
     * Checks if destination/store has territory designated as PM and assigns target account type as RETAIL PM if so
     * Otherwise, target account type for Retail order is RETAIL
     *
     * All other order types map to target account type as is (ECOMM, STRAT, etc)
     * @param carrierCode carrier code, FEDX
     * @param carrierModeCode carrier's mode code
     * @param entityProfile DC entity profile
     * @param orderType shipment's order type
     * @param destEntityCode shipment destination entity code
     * @return
     */
    public static String getTargetAccountType(String carrierCode, String carrierModeCode, EntityPayload entityProfile,
                                              String orderType, String destEntityCode) {
        if (OrderType.isRetailOrder(orderType)) {
            // Looks up account type for Retail Orders and sets PM if selected
            HashMap<String, DCRoute> dcRouting = entityProfile.getDcRouting();
            String targetAccountType = AccountType.RETAIL.toString();
            String customRouteId = getCustomRouteIdentifier(destEntityCode, carrierCode, carrierModeCode);
            if (dcRouting.containsKey(customRouteId) && dcRouting.get(customRouteId).getTerritory().equalsIgnoreCase(TERRITORY_PM)) {
                targetAccountType = AccountType.RETAIL_PM.toString();
            }
            return targetAccountType;
        } else {
            return orderType;
        }
    }

    /**
     * Return the DCRoutingInfo for a given
     * DestinationEntityCode
     * carrierCode
     * carrierModeCode
     */
    public static DCRoute getDCRoutingInfoForStoreEntityCode(String carrierCode, EntityPayload entityProfile,
                                                             String destEntityCode){
        HashMap<String, DCRoute> dcRoutingMap = entityProfile.getDcRouting();
        String customRouteId = getCustomRouteIdentifier(destEntityCode, carrierCode);
        Optional<DCRoute> dcRouteOptional = dcRoutingMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(customRouteId))
                .findFirst()
                .map(Map.Entry::getValue);
        if (dcRouteOptional.isPresent()) {
            log.debug("DCRouting Info is found: {}", dcRouteOptional.get());
            return dcRouteOptional.get();
        }
        log.debug("No DCRouting Info found for destEntityCode: {}, carrierCode: {}", destEntityCode, carrierCode);
        return null;
    }

    /**
     * Constructs Custom Key for DC Routing Lookup
     * @param destEntityCode Destination Entity Code
     * @param carrierCode Carrier Code, FEDX
     * @param carrierModeCode Carrier Mode Code
     * @return String of Custom Key
     */
    private static String getCustomRouteIdentifier(String destEntityCode, String carrierCode, String carrierModeCode) {
        return new StringBuilder(destEntityCode).append(DELIMITER).append(carrierCode)
                .append(DELIMITER).append(carrierModeCode).toString();
    }

    /**
     * Constructs Custom Key for DC Routing Lookup
     * @param destEntityCode Destination Entity Code
     * @param carrierCode Carrier Code, FEDX
     * @return String of Custom Key
     */
    private static String getCustomRouteIdentifier(String destEntityCode, String carrierCode) {
        return new StringBuilder(destEntityCode).append(DELIMITER).append(carrierCode).toString();
    }

    /**
     * Uses URSA Payload's 1D Tracking Barcode value as the Alternate Tracking Number
     * To be sent back to WMS
     * <p>If transit mode is Ground Economy, set alternate tracking number as USPS Tracking
     * All else, set Alternate Tracking Number as FIELD_7 a.k.a. Fedex Barcode</p>
     * @param shipmentInfo Shipment Message
     * @param ursaPayload FEDEX URSA Payload
     */
    public static void setAltTrackingNo(ShipmentInfo shipmentInfo, UrsaPayload ursaPayload) {
        String altTrackingNo;
        if (GROUND_ECONOMY_MODE.equals(shipmentInfo.getTransitDetails().getTransitMode())) {
            altTrackingNo = ursaPayload.getOut().get((long) UVConstants.FDXPSP_O_FGELBL_FIELD_D3);
        } else {
            altTrackingNo = ursaPayload.getOut().get((long) UVConstants.FDXPSP_O_LBL_FIELD_7);
        }
        log.debug(String.format(STANDARD_FIELD_INFO, "AlternateTrackingNo", altTrackingNo));
        shipmentInfo.getTransitDetails().setAltTrackingNo(altTrackingNo);
    }

    /**
     * Validate Carrier Code for cancellation/create
     *
     * @param headers            event headers
     * @param desiredCarrierCode FEDX
     * @return boolean
     */
    public static boolean validCarrierCode(MessageHeaders headers, String desiredCarrierCode) {
        if (null != headers.get(CARRIER_CODE_HEADER_KEY)
                && headers.containsKey(CARRIER_CODE_HEADER_KEY)) {
            String headerCarrierCd = new String((byte[]) headers.get(CARRIER_CODE_HEADER_KEY));
            return desiredCarrierCode.equalsIgnoreCase(headerCarrierCd);
        }
        return false;
    }

    public static void updateDestinationForRetailPM(ShipmentMessage shipmentMessage, HubInfo hubInfo) {
            if (hubInfo != null) {
                log.info("Shipment is for PM store. Updating destination address to hub details.");
                LocationItem addressTo = shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader().getDestination().getAddressTo();
                addressTo.setDescription1(toUpper(hubInfo.getAddress1()));
                addressTo.setDescription2(toUpper(hubInfo.getAddress2()));
                addressTo.setCity(toUpper(hubInfo.getCity()));
                addressTo.setState(toUpper(hubInfo.getState()));
                addressTo.setZipCode(toUpper(hubInfo.getZipcode()));
                addressTo.setCountry(toUpper(hubInfo.getCountryCode()));
            }
    }
}
