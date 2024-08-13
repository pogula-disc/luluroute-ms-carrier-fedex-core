package com.luluroute.ms.carrier.fedex.fuseapi.service;

import com.fedex.ursa.uvsdk.UVConstants;
import com.fedex.ursa.uvsdk.UVContext;
import com.fedex.ursa.uvsdk.UVFilePair;
import com.fedex.ursa.uvsdk.exceptions.UVRuntimeException;
import com.fedex.ursa.uvsdk.exceptions.UVStatusException;
import com.logistics.luluroute.carrier.fedex.TransitTimeDTO;
import com.logistics.luluroute.redis.shipment.entity.DCRoute;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.carrier.fedex.exception.MandatoryValuesMissingException;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.fedex.fuseapi.service.helper.DCRoutingUtil;
import com.luluroute.ms.carrier.fedex.util.Constants;
import com.luluroute.ms.carrier.fedex.util.DateUtil;
import com.luluroute.ms.carrier.fedex.util.OrderType;
import com.luluroute.ms.carrier.model.TransitTimeResponse;
import com.luluroute.ms.carrier.model.UrsaPayload;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import com.luluroute.ms.carrier.service.RedisRehydrateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.luluroute.ms.carrier.fedex.util.Constants.*;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.CODE_TRANSIT_TIME;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.SOURCE_TRANSIT_TIME;

/**
 * The Class URSATransitTimeService.
 *
 * @author MANDALAKARTHIK1
 */
@Service
@Slf4j
public class URSATransitTimeService extends BaseURSAService {

    /**
     * The default transit time calculator.
     */
    @Autowired
    private DefaultTransitTimeCalculator defaultTransitTimeCalculator;

    @Value("${config.carrier.code}")
    private String carrierCode;

    @Autowired
    private RedisRehydrateService redisRehydrateService;

    /**
     * Gets the transit time.
     *
     * @param inputData the input data
     * @return the transit time
     * @throws ShipmentMessageException the UV runtime exception
     */
    public List<TransitTimeResponse> getTransitTime(List<TransitTimeDTO> inputData) throws ShipmentMessageException {
        this.validateMandatory(inputData);
        return this.calcTransitTime(inputData);

    }

    /**
     * Validate mandatory.
     *
     * @param transitTimeDTOList the transit time DTO list
     */
    private void validateMandatory(List<TransitTimeDTO> transitTimeDTOList) {
        transitTimeDTOList.forEach(inputData -> {
            this.checkMandatory(inputData.getCarrierMode(), "CarrierMode");
            this.checkMandatory(inputData.getOriginCountry(), "Origin Country");
            this.checkMandatory(inputData.getOriginPostalCode(), "Origin PostalCode");
            this.checkMandatory(inputData.getTimeZone(), "Timezone");
            this.checkMandatory(inputData.getPlannedShipDate(), "Planned ship date");
            this.checkMandatory(inputData.getDestinationCountry(), "Destination Country");
            this.checkMandatory(inputData.getDestinationPostalCode(), "Destination PostalCode");
            this.checkMandatory(inputData.getDefaultTransitDays(), "Defailt Transit days");
            this.checkMandatory(inputData.getOriginEntityCode(), "Origin EntityCode");
        });

    }

    /**
     * Check mandatory.
     *
     * @param <M>        the generic type
     * @param input      the input
     * @param columnName the column name
     */
    private <M> void checkMandatory(M input, String columnName) {
        if (ObjectUtils.isEmpty(input))
            throw new MandatoryValuesMissingException("Mandatory value " + columnName + " missing");
    }

    /**
     * Calculates and Returns Fedex's Shipment Planned Delivery Date
     *
     * @param inputData Transit Time Request Payload
     * @return List of transit time responses from URSA or Default Transit Days
     * @throws ShipmentMessageException when transit time calculation fails for non-URSA related error.
     */
    protected List<TransitTimeResponse> calcTransitTime(List<TransitTimeDTO> inputData) throws ShipmentMessageException {
        String msg = "URSATransitTimeService.composeURSARequest()";
        List<TransitTimeResponse> transitTimeList = new ArrayList<>();
        LinkedHashMap<Long, String> response = new LinkedHashMap<>();
        try {
            return buildTransitTimeList(inputData, transitTimeList, response);
        } catch (UVStatusException use) {
            log.debug("URSA did not provide transit days for this mode. So performing default transit day calculations");
            log.debug(URSA_TRANSIT_DAY_EXCEPTION_INFO, ExceptionUtils.getMessage(use),
                    ExceptionUtils.getStackTrace(use));
            for (TransitTimeDTO transitTime: inputData) {
                String defaultDeliveryDate = defaultTransitTimeCalculator.calculateDefaultTransitDays(transitTime);
                formatAndAddPDD(transitTime.getTimeZone(), transitTimeList, transitTime, defaultDeliveryDate);
            }
            return transitTimeList;
        } catch (Exception exp) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(exp));
            throw new ShipmentMessageException(CODE_TRANSIT_TIME, "Error occurred in TransitTime API",
                    SOURCE_TRANSIT_TIME);
        }
    }

    /**
     * Constructs Fedex URSA's UVSDK Transit Time Call Request payload through input object.
     * Executes UVSDK Transit Time Call, populating response object.
     * @param inputData Transit Time API request input
     * @param response response from UVSDK
     * @throws IOException
     * @throws UVRuntimeException
     */
    private void callURSATransitTime(List<TransitTimeDTO> inputData, LinkedHashMap<Long, String> response)
            throws IOException, UVRuntimeException {
        LinkedHashMap<Long, String> input = new LinkedHashMap<>();
        input.put((long) UVConstants.FDXPSP_I_IFACE_VERSION, "1");
        input.put((long) UVConstants.FDXPSP_I_IFACE_METHOD, UVConstants.PSPLUS_METHOD_BASIC_SERVICE_LIST);
        input.put((long) UVConstants.FDXPSP_I_ORIGIN_COUNTRY, inputData.get(0).getOriginCountry());
        input.put((long) UVConstants.FDXPSP_I_ORIGIN_POSTAL, getPostalCode(inputData.get(0).getOriginPostalCode()));
        input.put((long) UVConstants.FDXPSP_I_SHIPMENT_DATE, DateUtil.formatEpochToStringWithFormat(
                inputData.get(0).getPlannedShipDate(), inputData.get(0).getTimeZone(), LABEL_YYYYMMDD_FORMAT));
        input.put((long) UVConstants.FDXPSP_I_DEST_COUNTRY, inputData.get(0).getDestinationCountry());
        input.put((long) UVConstants.FDXPSP_I_DEST_POSTAL, getPostalCode(inputData.get(0).getDestinationPostalCode()));
        Instant startTime = Instant.now();
        log.debug("Before URSA Context: ");
        UVFilePair uvFilePair = new UVFilePair(ursaFile, editFile);
        UVContext uvContext = uvFilePair.createUVContext();
        Instant endTime = Instant.now();
        log.debug(" URSA Context created: >> ::Time taken to process in milli seconds {} ",
                Duration.between(startTime, endTime).toMillis());
        uvContext.uvTaggedIORun(input, response);
    }

    /**
     * Iterates through each transit time DTO and builds transit time response
     * @return Transit Time Response
     */
    private List<TransitTimeResponse> buildTransitTimeList(List<TransitTimeDTO> inputData,
            List<TransitTimeResponse> transitTimeList, LinkedHashMap<Long, String> response)
            throws IOException, UVRuntimeException {

        DCRoute routeInfo = null;
        String ursaProvidedDate = "";
        String orderType;
        for (TransitTimeDTO transitTime : inputData) {
            orderType = transitTime.getOrderType();
            if (OrderType.isRetailOrder(orderType)) {
                log.info("TransitTime calculation for order type: {}", orderType);
                // Load Profile from Redis
                EntityPayload entityProfile = redisRehydrateService.getEntityByCode(transitTime.getOriginEntityCode());

                String destEntity = transitTime.getDestinationEntityCode();
                if (StringUtils.isNotEmpty(destEntity)) {
                    log.info("In URSATransitTimeService - Destination Entity code: {}", destEntity);
                    String routeInfoKey = DCRoutingUtil.getCustomRouteIdentifier(destEntity, carrierCode,
                            transitTime.getCarrierMode());
                    routeInfo = entityProfile.getDcRouting().get(routeInfoKey);
                }

                if (null !=routeInfo  && null != routeInfo.getTransitDays() && routeInfo.getTransitDays() > 0) {
                    log.info("Transit days from DC Routing: {}", routeInfo.getTransitDays());
                    // Setting default transit days from DC Routing.
                    transitTime.setDefaultTransitDays(routeInfo.getTransitDays());
                } else {
                    log.info("Considering default Transit days: {}", transitTime.getDefaultTransitDays());
                }
                ursaProvidedDate = defaultTransitTimeCalculator.calculateDefaultTransitDays(transitTime);
                formatAndAddPDD(transitTime.getTimeZone(), transitTimeList, transitTime, ursaProvidedDate);
            } else {
                // Calling URSA only for Non-Retail orders.
                callURSATransitTime(inputData, response);
                log.debug("URSA Context invoked :");
                log.debug("Available modes from URSA: {}", response);
                ursaProvidedDate = getTransitDate(UrsaPayload.builder().out(response).build(),
                        transitTime.getCarrierMode(), transitTime.isSaturdayDelivery());
                if (StringUtils.isEmpty(ursaProvidedDate) || TRANSIT_DAYS_NOT_AVAILABLE.equalsIgnoreCase(ursaProvidedDate)) {
                    log.debug("URSA did not provide transit days for this mode. So performing default transit day calculations");
                    ursaProvidedDate = defaultTransitTimeCalculator.calculateDefaultTransitDays(transitTime);
                }
                log.debug(String.format(STANDARD_FIELD_INFO, "ursaProvidedDate", ursaProvidedDate));
                formatAndAddPDD(transitTime.getTimeZone(), transitTimeList, transitTime, ursaProvidedDate);
            }
        }
        return transitTimeList;
    }

    /**
     * Formats transit time response to YYYYMMDD date format and returns transit time response.
     */
    private void formatAndAddPDD(String timeZone, List<TransitTimeResponse> transitTimeList,
                                 TransitTimeDTO transitTime, String transitDate) {
        long calculatedPDD = DateUtil.formatStringWithFormatToEpoch(transitDate, timeZone, LABEL_YYYYMMDD_FORMAT);
        log.debug(String.format(STANDARD_FIELD_INFO, "calculatedPDD", calculatedPDD));
        transitTimeList.add(TransitTimeResponse.builder().responseDeliveryDate(calculatedPDD)
                .carrierMode(transitTime.getCarrierMode()).build());
    }

    /**
     * Gets the transit date.
     *
     * @param response           the response
     * @param carrierMode        the carrier mode
     * @param isSaturdayDelivery the is Saturday delivery
     * @return the transit date
     */
    private String getTransitDate(UrsaPayload response, String carrierMode, boolean isSaturdayDelivery) {
        String estimateDeliveryDate = "";
        carrierMode = carrierMode.equalsIgnoreCase(EXCEPTIONAL_MODE_95) ? EXCEPTIONAL_MODE_92 : carrierMode;
        if (isSaturdayDelivery) {
            estimateDeliveryDate = getBestDate(response, carrierMode);
        } else {
            estimateDeliveryDate = getEstimatedDeliveryDate(response, carrierMode, WEEKDAY_DELIVERY);
        }
        return estimateDeliveryDate;
    }

    /**
     * Saturday delivery date is enabled. This method helps check which is the best
     * day which can be weekday or Saturday
     *
     * @param response    the response
     * @param carrierMode the carrier mode
     * @return the best date
     */
    private String getBestDate(UrsaPayload response, String carrierMode) {
        String estimateDeliveryDate = "";
        String weekDayDate = getEstimatedDeliveryDate(response, carrierMode, WEEKDAY_DELIVERY);
        String saturadayDate = getEstimatedDeliveryDate(response, carrierMode, SATURDAY_DELIVERY);
        if (StringUtils.isEmpty(weekDayDate) && StringUtils.isEmpty(saturadayDate)) {
            estimateDeliveryDate = "";
        } else if (StringUtils.isEmpty(weekDayDate) && StringUtils.isNotEmpty(saturadayDate)) {
            estimateDeliveryDate = saturadayDate;
        } else if (StringUtils.isEmpty(saturadayDate) && StringUtils.isNotEmpty(weekDayDate)) {
            estimateDeliveryDate = weekDayDate;
        } else {
            // compare and check which date is best either can be weekday or saturday
            estimateDeliveryDate = DateUtil.compareTwoDates(weekDayDate, saturadayDate);
        }
        return estimateDeliveryDate;
    }

    /**
     * Gets the estimated delivery date.
     *
     * @param response    the response
     * @param carrierMode the carrier mode
     * @param deliveryDay the delivery day
     * @return the estimated delivery date
     */
    private String getEstimatedDeliveryDate(UrsaPayload response, String carrierMode, String deliveryDay) {
        String estimateDeliveryDate = "";
        String modeKey = new StringBuilder(carrierMode.replace(INTERNATIONAL, "")).append(COMMA).append(deliveryDay)
                .toString();
        Optional<String> availbaleCarrierModes = response.getOut().values().stream()
                .filter(predicate -> predicate.contains(modeKey)).findAny();
        if (availbaleCarrierModes.isPresent()) {
            String modeAndTransitDate = availbaleCarrierModes.get();
            estimateDeliveryDate = modeAndTransitDate.substring(modeAndTransitDate.lastIndexOf(COMMA) + 1);
        }
        return estimateDeliveryDate;
    }

    /**
     * Gets the postal code.
     * <p>
     * For Canadian Zip Codes - For Example - L3Z 0K5 - URSA expects to remove the
     * white space present in center.
     * <p>
     * For US Zip Codes - Always use only Zip5 - Some times Zip codes received in
     * Shipment Request may have 10 digits
     *
     * @param postalCode the postal code
     * @return the postal code
     */
    private String getPostalCode(String postalCode) {
        String validPostalCode = postalCode;
        if (NumberUtils.isCreatable(postalCode) && postalCode.length() > 5) {
            validPostalCode = postalCode.substring(0, 5);
        } else {
            validPostalCode = validPostalCode.replace(" ", "");
        }
        return validPostalCode;
    }
}
