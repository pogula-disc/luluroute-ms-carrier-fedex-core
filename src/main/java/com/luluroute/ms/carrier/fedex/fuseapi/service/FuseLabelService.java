package com.luluroute.ms.carrier.fedex.fuseapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedex.ursa.uvsdk.UVConstants;
import com.fedex.ursa.uvsdk.UVContext;
import com.fedex.ursa.uvsdk.UVFilePair;
import com.fedex.ursa.uvsdk.exceptions.UVRuntimeException;
import com.fedex.ursa.uvsdk.exceptions.UVStatusException;
import com.logistics.luluroute.domain.Shipment.Service.OrderInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Service.TransitInfo;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;
import com.logistics.luluroute.domain.Shipment.Shared.MeasureItem;
import com.logistics.luluroute.redis.shipment.entity.HubInfo;
import com.luluroute.ms.carrier.config.FedExModeConfig;
import com.luluroute.ms.carrier.config.UrsaConfig;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.fedex.fuseapi.service.helper.CustomLabelHelper;
import com.luluroute.ms.carrier.fedex.util.DateUtil;
import com.luluroute.ms.carrier.fedex.util.ExceptionConstants;
import com.luluroute.ms.carrier.fedex.util.PaymentType;
import com.luluroute.ms.carrier.model.FedExServiceInfo;
import com.luluroute.ms.carrier.model.UrsaPayload;
import com.luluroute.ms.carrier.repository.TrackingSeedRepository;
import com.luluroute.ms.carrier.service.helper.PackageSequenceHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.fedex.ursa.uvsdk.UVConstants.UV_GLOBAL_EMPTY_STR;
import static com.logistics.luluroute.util.ConstantUtil.COUNTRY_PR;
import static com.logistics.luluroute.util.ConstantUtil.COUNTRY_US;
import static com.luluroute.ms.carrier.fedex.fuseapi.service.helper.CustomLabelHelper.toUpper;
import static com.luluroute.ms.carrier.fedex.util.Constants.*;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.*;
import static com.luluroute.ms.carrier.service.ServiceEnquiryService.buildURSAValueToKey;

@Service
@Slf4j
public class FuseLabelService extends BaseURSAService implements FedExLabelService {

    @Autowired
    private FedExModeConfig fedExModeConfig;

    @Autowired
    private TrackingSeedRepository trackingSeedRepository;

    @Autowired
    private PackageSequenceHelper packageSequenceHelper;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${fedex.label.etdCountries}")
    private List<String> etdCountries;

    @Value("${fedex.label.us-territories}")
    private List<String> usTerritories;

    @Autowired
    private UrsaConfig ursaConfig;

    public LinkedHashMap<Long, String> testURSASDK(LinkedHashMap<Long, String> in) throws IOException, UVRuntimeException {
        UVFilePair uvFilePair = new UVFilePair(ursaFile, editFile);
        UVContext uvContext = uvFilePair.createUVContext();
        LinkedHashMap<Long, String> out = new LinkedHashMap<>();
        try {
            uvContext.uvTaggedIORun(in, out);
        }catch (Exception e) {
          log.error("Exception occurred in FuseLabelService", e);
        }
        return out;
    }
    @Override
    public UrsaPayload generateUrsaPayload(ShipmentInfo shipmentInfo, Map<String, String> carrierMetaDataMap
            , boolean isSaturdayDelivery, HubInfo retailHubInfo) throws ShipmentMessageException {

        UVContext uvContext = null;
        LocationItem addressTo = null;
        LinkedHashMap<Long, String> out = new LinkedHashMap<>();
        LinkedHashMap<Long, String> in = new LinkedHashMap<>();
        String modeCode = "";
        try {
            Instant startTime = Instant.now();
            UVFilePair uvFilePair = new UVFilePair(ursaFile, editFile);
            //effective date and expiry date
            uvContext = uvFilePair.createUVContext();
            Instant endTime = Instant.now();
            log.info(" URSA Context created: >> ::Time taken to process in milli seconds {} ",
                    Duration.between(startTime, endTime).toMillis());
            log.debug("display records: " + uvContext.getDisplayRecords());
            String entityAccountNo = carrierMetaDataMap.get(ACCOUNT_TO_BILL);
            String meterNo = carrierMetaDataMap.get(METER_NO);

            if (retailHubInfo != null) {
                    log.info("Shipment is for PM store. Updating destination address to hub details.");
                addressTo = new LocationItem();
                addressTo.setContact(toUpper( shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getContact()));
                addressTo.setContactPhone(toUpper( shipmentInfo.getShipmentHeader().getDestination().getAddressTo().getContactPhone()));
                addressTo.setDescription1(toUpper(retailHubInfo.getAddress1()));
                addressTo.setDescription2(toUpper(retailHubInfo.getAddress2()));
                addressTo.setCity(toUpper(retailHubInfo.getCity()));
                addressTo.setState(toUpper(retailHubInfo.getState()));
                addressTo.setZipCode(toUpper(retailHubInfo.getZipcode()));
                addressTo.setCountry(toUpper(retailHubInfo.getCountryCode()));
            } else {
                addressTo = shipmentInfo.getShipmentHeader().getDestination().getAddressTo();
            }
            LocationItem addressFrom = shipmentInfo.getShipmentHeader().getOrigin().getAddressFrom();
            TransitInfo transitDetails = shipmentInfo.getTransitDetails();
            MeasureItem weightDetails = shipmentInfo.getShipmentPieces().get(0).getWeightDetails();
            OrderInfo orderDetails = shipmentInfo.getOrderDetails();
            String transitMode = transitDetails.getTransitMode();
            mapCountryIfUSTerritoryState(addressTo);
            in.put((long) UVConstants.FDXPSP_I_IFACE_VERSION, "1");
            in.put((long) UVConstants.FDXPSP_I_IFACE_METHOD, UVConstants.PSPLUS_METHOD_SHIPMENT_CREATE_2D);
            in.put((long) UVConstants.FDXPSP_I_DEST_COUNTRY, addressTo.getCountry());
            in.put((long) UVConstants.FDXPSP_I_ORIGIN_COUNTRY, addressFrom.getCountry());
            in.put((long) UVConstants.FDXPSP_I_ORIGIN_POSTAL, getPostalCode(addressFrom.getCountry(), addressFrom.getZipCode()));
            in.put((long) UVConstants.FDXPSP_I_SHIPMENT_DATE, DateUtil.formatEpochToStringWithFormat(transitDetails.getDateDetails().getPlannedShipDate(), carrierMetaDataMap.get(ENTITY_TIMEZONE), LABEL_YYYYMMDD_FORMAT));
            in.put((long) UVConstants.FDXPSP_I_DEST_POSTAL, getPostalCode(addressTo.getCountry(), addressTo.getZipCode()));
            in.put((long) UVConstants.FDXPSP_I_RECIPIENT_POSTAL, getPostalCode(addressTo.getCountry(), addressTo.getZipCode()));
            modeCode = getTransitMode(transitMode, addressFrom.getCountry(), addressTo.getCountry());
            in.put((long) UVConstants.FDXPSP_I_SERVICE, modeCode);
            in.put((long) UVConstants.FDXPSP_I_PACKAGING, "01");
            in.put((long) UVConstants.FDXPSP_I_PACKAGE_PAYER_TYPE, "S");
            in.put((long) UVConstants.FDXPSP_I_TRACKING_NUMBER, transitDetails.getTrackingNo());
            in.put((long) UVConstants.FDXPSP_I_PACKAGE_SHIPPER_ACCT, entityAccountNo); //use account-level EAN for express modes

            FedExServiceInfo fedExServiceInfo = CustomLabelHelper.getServiceDetailsByMode(fedExModeConfig,
                    transitMode);
            Boolean isInternational = CustomLabelHelper.isInternational(shipmentInfo);
            if ("E".equalsIgnoreCase(fedExServiceInfo.getServiceLetter())) {
                if (addressFrom.getCountry().equalsIgnoreCase(COUNTRY_CA)) {
                    in.put((long) UVConstants.FDXPSP_I_FORM_ID, isInternational
                            ?  STRING_EMPTY : ursaConfig.getFormIds().get(CA_DOMESTIC));
                } else {
                    in.put((long) UVConstants.FDXPSP_I_FORM_ID, ursaConfig.getFormIds()
                            .get(isInternational ? US_INTL : US_DOMESTIC));
                }

                if (isSaturdayDelivery)
                    in.put((long) UVConstants.FDXPSP_I_DELIVERY_HANDLING, "03");
            }

            if (isInternational) {
                in.put((long) UVConstants.FDXPSP_I_CUSTOMS_CURRENCY, orderDetails.getDeclaredValueDetails().getCurrency());
                in.put((long) UVConstants.FDXPSP_I_CUSTOMS_VALUE_IN_CENTS, String.valueOf(new DecimalFormat(DECIMAL_FORMAT)
                        .format(orderDetails.getDeclaredValueDetails().getValue() * 100)));
                in.put((long) UVConstants.FDXPSP_I_PACKAGE_DECLARED_VALUE_IN_CENTS, String.valueOf(new DecimalFormat(DECIMAL_FORMAT)
                        .format(orderDetails.getDeclaredValueDetails().getValue() * 100)));
                in.put((long) UVConstants.FDXPSP_I_COMMODITY_DESCRIPTION, orderDetails.getShipmentContents());
                in.put((long) UVConstants.FDXPSP_I_CUSTOMS_SHIPMENT_TYPE, "010");
                in.put((long) UVConstants.FDXPSP_I_DUTYTAXES_ACCT_NUMBER, entityAccountNo);
                in.put((long) UVConstants.FDXPSP_I_DUTYTAXES_PAYMENT_TYPE,
                        getDutiesPaymentType(addressFrom.getCountry(), addressTo.getCountry()));
                in.put((long) UVConstants.FDXPSP_I_SED_STATEMENT, SED_STATEMENT);
            }
            if (GROUND_ECONOMY_MODE.equals(transitMode)) {
                in.put((long) UVConstants.FDXPSP_I_USPS_MAILER_ID, carrierMetaDataMap.get(SMART_POST_METER_ID)); //SPMID
                in.put((long) UVConstants.FDXPSP_I_USPS_PACKAGE_SEQ_NBR, String.format("%07d", packageSequenceHelper.getPackageSeqByAccountNo(carrierMetaDataMap.get(ACCOUNT_TO_BILL))));
                in.put((long) UVConstants.FDXPSP_I_USPS_INDICIA_TYPE, "1");
                in.put((long) UVConstants.FDXPSP_I_IFACE_METHOD, UVConstants.PSPLUS_METHOD_SHIPMENT_CREATE); //No need for 2D Barcode in Ground Economy
                addSmartPostReleaseAndEnd(in, orderDetails.isPOBox(), orderDetails.isMilitary());
            }

            if (GROUND_ECONOMY_MODE.equals(transitMode) || isGroundOrHome(transitMode)) {
                in.put((long) UVConstants.FDXPSP_I_PACKAGE_SHIPPER_ACCT, carrierMetaDataMap.get(ACCOUNT_GSN)); //use account-level GSN for Ground, Home, SP modes
            }

            in.put((long) UVConstants.FDXPSP_I_DEST_ADDRESS_LINE1, addressTo.getDescription1());
            in.put((long) UVConstants.FDXPSP_I_DEST_CITY_NAME, addressTo.getCity());
            in.put((long) UVConstants.FDXPSP_I_RECIPIENT_CONTACT_NAME, addressTo.getContact());
            in.put((long) UVConstants.FDXPSP_I_RECIPIENT_PHONE_NUMBER, addressTo.getContactPhone());

            in.put((long) UVConstants.FDXPSP_I_PACKAGE_WEIGHT_UNITS, weightDetails.getUom().toUpperCase().contains("LB") ? "CP" : "DG");
            in.put((long) UVConstants.FDXPSP_I_PACKAGE_WEIGHT, formatPackageWeight(weightDetails.getValue()));

            in.put((long) UVConstants.FDXPSP_I_METER_NUMBER, meterNo);
            //pass ansi representation tbd
            if (!GROUND_ECONOMY_MODE.equals(transitMode)) {
                Optional.ofNullable(getOptionalServiceCode(transitDetails.isSignatureRequired, transitDetails.isResidential,
                                addressTo.getCountry(), addressFrom.getCountry(), transitMode, isInternational))
                        .ifPresent(optionalServiceCode -> in.put((long) UVConstants.FDXPSP_I_OPTIONAL_SERVICES, optionalServiceCode));
            }

            log.info("URSA INPUT : {}", in);
            uvContext.uvTaggedIORun(in, out);
            log.info("URSA OUTPUT : {}", out);
            return UrsaPayload.builder().input(in).out(out).build();

        } catch (UVStatusException.UVExceptionMissingService uve) {
            log.error(STANDARD_ERROR, String.format("FedEx service: %s is not offered for the given destination address", modeCode),
                    ExceptionUtils.getStackTrace(uve));
            throw new ShipmentMessageException(CODE_UVSDK_SERVICE_ERROR,
                    String.format(URSA_SERVICE_ERROR, modeCode), ExceptionConstants.CODE_UVSDK_SOURCE);
        } catch (UVStatusException.UVExceptionPostalNotServed uve) {
            String zipCode = (addressTo == null) ? "" : addressTo.getZipCode();
            log.error(STANDARD_ERROR, String.format("FedEx service is not available for destination zip: %s", zipCode),
                    ExceptionUtils.getStackTrace(uve));
            throw new ShipmentMessageException(CODE_UVSDK_ZIPCODE_ERROR,
                    String.format(URSA_ZIPCODE_ERROR, modeCode, zipCode), ExceptionConstants.CODE_UVSDK_SOURCE);
        } catch (UVRuntimeException e) {
            log.error("Error Output from URSA: {}",objectMapper.valueToTree(buildURSAValueToKey(out)));
            if(isSpecialHandlingError(out)){
                log.error("Special Handling not applicable for the Shipment request." +
                        "Will be re-processed without Special handling for Saturday Delivery");
                return generateUrsaPayload(shipmentInfo,carrierMetaDataMap,false, retailHubInfo);

            }
            log.error(STANDARD_ERROR, e.getMessage(), ExceptionUtils.getStackTrace(e));
            throw new ShipmentMessageException(CODE_UVSDK, String.format(URSA_RUNTIME_EXCEPTION_INFO, e, e.getMessage()),
                    ExceptionConstants.CODE_UVSDK_SOURCE);
        } catch (IOException ioe) {
            log.error(STANDARD_ERROR, ioe.getMessage(), ExceptionUtils.getStackTrace(ioe));
            throw new ShipmentMessageException(CODE_UVSDK, ioe.getMessage(),
                    ExceptionConstants.CODE_UVSDK_SOURCE);
        }

    }

    /**
     * If Smart Post Address is military or PO Box, FDXPSP_I_USPS_RELEASE is NO & FDXPSP_I_USPS_ENDORSEMENTS is Return Service Requested
     * Otherwise for all other Smart Post deliveries such as gift cardsm, FDXPSP_I_USPS_RELEASE is YES and FDXPSP_I_USPS_ENDORSEMENTS is not needed for input
     * @param in map of URSA input key/values
     */
    private void addSmartPostReleaseAndEnd(LinkedHashMap<Long, String> in, boolean isPoBox, boolean isMilitary) {
        if (isPoBox || isMilitary) {
            log.debug(String.format(STANDARD_FIELD_INFO, "FDXPSP_I_USPS_RELEASE = N, FDXPSP_I_USPS_ENDORSEMENTS = RS", "Return Service Requested. Ground Economy shipment IS delivering to PO Box or Military address."));
            in.put((long) UVConstants.FDXPSP_I_USPS_RELEASE, "N");
            in.put((long) UVConstants.FDXPSP_I_USPS_ENDORSEMENTS, "RS");
        } else {
            log.debug(String.format(STANDARD_FIELD_INFO, "FDXPSP_I_USPS_RELEASE = Y", "Carrier Release If No Response is endorsed. Ground Economy shipment IS NOT delivering to PO Box or Military address."));
            in.put((long) UVConstants.FDXPSP_I_USPS_RELEASE, "Y");
        }
    }

    private boolean isSpecialHandlingError(LinkedHashMap<Long, String> out) {
        return out!=null && out.containsKey((long)UVConstants.FDXPSP_E_BAD_HANDLING);
    }

    public String getPostalCode(String country, String postalCode) {
        return country.equalsIgnoreCase(COUNTRY_CA) ? postalCode.replace(" ", "") :
                country.equalsIgnoreCase(COUNTRY_US) ? postalCode.trim().substring(0, 5) : postalCode;
    }

    public String getTransitMode(String transitMode, String shipFromLocationCountry, String shipToLocationCountry) {
        String finalTransaitMode = transitMode;
        if (!shipFromLocationCountry.equalsIgnoreCase(shipToLocationCountry))
            finalTransaitMode = transitMode.substring(0, 2);
        if (transitMode.equals("04") && shipFromLocationCountry.equalsIgnoreCase(COUNTRY_CA)
                && shipToLocationCountry.equalsIgnoreCase(COUNTRY_US))
            finalTransaitMode = "03";

        return finalTransaitMode;
    }

    private String formatPackageWeight(double shipmentWeight) {
        String weightStr = String.valueOf(new DecimalFormat(DECIMAL_FORMAT).format(shipmentWeight));
        return weightStr.replace(".", UV_GLOBAL_EMPTY_STR);
    }

    /**
     * <h3>Builds optional service code input for URSA</h3>
     * <p>Ground and Home Labels should not accept Direct Signature Options</p>
     * <p>Optional Service Code Mapping</p>
     * <ul>
     *     <li>Direct Signature Required - 10</li>
     *     <li>Residential Delivery - 28</li>
     *     <li>No Signature Required (International and intra-country (non-US Domestic) Express and Ground) - 36</li>
     * </ul>
     */
    public String getOptionalServiceCode(boolean isSignatureRequired, boolean isResidential, String shipToCountry,
                                         String shipFromCountry, String transitMode, Boolean isIntl) {
        StringBuilder optionServiceCode = new StringBuilder();
        if (isResidential && !GROUND_MODE.equals(transitMode)) {
            optionServiceCode.append("28");
        }

        if (isSignatureRequired && !isGroundOrHome(transitMode)) {
            optionServiceCode.append("10");
        } else if (isResidential && (shipToCountry.equalsIgnoreCase(COUNTRY_CA)
                || (shipFromCountry.equalsIgnoreCase(COUNTRY_CA) && shipToCountry.equalsIgnoreCase(COUNTRY_US)))) {
            optionServiceCode.append("36");
        }

        if (isIntl) {
            optionServiceCode.append(etdCountries.contains(shipToCountry) ? URSA_CD_ETD : URSA_CD_EWO);
        }
        return optionServiceCode.toString();
    }

    private boolean isGroundOrHome(String transitMode) {
        return HOME_MODE.equals(transitMode) || GROUND_MODE.equals(transitMode);
    }

    /**
     * Identifies International Shipment's Duties Payment Type as
     * <ul>
     * <li>Sender if CA to US or US to CA/PR</li>
     * <li>Recipient if US to ROW or CA to ROW (Rest of World)</li>
     * </ul>
     * @param sourceCountry shipment request shipper's country
     * @param destCountry shipment request recipient's country
     * @return Payment Type String
     */
    private String getDutiesPaymentType(String sourceCountry, String destCountry) {
        if (isCAtoUS(sourceCountry, destCountry) || isUStoCAorPR(sourceCountry, destCountry)) {
            return PaymentType.SENDER.getValue();
        } else {
            return PaymentType.RECIPIENT.getValue();
        }
    }

    private boolean isCAtoUS(String sourceCountry, String destCountry) {
        return sourceCountry.equalsIgnoreCase(COUNTRY_CA)
                && destCountry.equalsIgnoreCase(COUNTRY_US);
    }

    private boolean isUStoCAorPR(String sourceCountry, String destCountry) {
        return sourceCountry.equalsIgnoreCase(COUNTRY_US)
                && (destCountry.equalsIgnoreCase(COUNTRY_CA)
                || destCountry.equalsIgnoreCase(COUNTRY_PR));
    }

    /**
     * For scenarios where input country is US and state is US Territory, maps FDXPSP_I_DEST_COUNTRY to US Territory.
     * Otherwise, Using US Territory as the state will cause the URSA SDK call to fail.
     * @param addressTo destination address
     */
    private void mapCountryIfUSTerritoryState(LocationItem addressTo) {
        String state = addressTo.getState();
        if (isUSTerritoryState(addressTo.getCountry(), state)) {
            addressTo.setCountry(state);
        }
    }

    private boolean isUSTerritoryState(String destCountry, String destState) {
        return destCountry.equalsIgnoreCase(COUNTRY_US) && usTerritories.contains(destState);
    }

}
