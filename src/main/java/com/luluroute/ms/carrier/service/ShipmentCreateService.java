package com.luluroute.ms.carrier.service;

import com.logistics.luluroute.avro.artifact.message.Extended;
import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import com.logistics.luluroute.avro.artifact.message.TransitTimeInfo;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;
import com.logistics.luluroute.redis.shipment.carriermain.CarrierMainPayload;
import com.logistics.luluroute.redis.shipment.entity.AssignedTransitModes;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.logistics.luluroute.redis.shipment.entity.HubInfo;
import com.logistics.luluroute.util.DaysOfWeekUtil;
import com.logistics.luluroute.validator.ValidationEngine;
import com.logistics.luluroute.validator.ValidationUtil;
import com.logistics.luluroute.validator.exception.ValidationException;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.fedex.fuseapi.service.CustomLabelService;
import com.luluroute.ms.carrier.fedex.fuseapi.service.FuseLabelService;
import com.luluroute.ms.carrier.fedex.util.Constants;
import com.luluroute.ms.carrier.fedex.util.DateUtil;
import com.luluroute.ms.carrier.fedex.util.OrderType;
import com.luluroute.ms.carrier.fedex.util.RedisCacheLoader;
import com.luluroute.ms.carrier.model.UrsaPayload;
import com.luluroute.ms.carrier.rule.ShipmentCreateBRule;
import com.luluroute.ms.carrier.service.helper.ShipmentCreateHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotEmpty;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static com.luluroute.ms.carrier.fedex.fuseapi.service.CustomLabelService.getHubInfoIfPMDelivery;
import static com.luluroute.ms.carrier.fedex.util.Constants.*;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.*;
import static com.luluroute.ms.carrier.service.helper.ShipmentCreateHelper.updateDestinationForRetailPM;

@Service
@Slf4j
public class ShipmentCreateService {
    @Autowired
    private RedisCacheLoader redisCacheLoader;
    @Autowired
    private RedisRehydrateService redisRehydrateService;
    @Autowired
    private ShipmentArtifactService shipmentArtifactService;
    @Autowired
    private TrackingSeedService trackingSeedService;
    @Autowired
    private FuseLabelService fuseLabelService;
    @Autowired
    private CustomLabelService customLabelService;
    @Autowired
    private ServiceEnquiryService serviceEnquiryService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ValidationEngine validationEngine;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${fedex.label.ewoCountries}")
    private List<String> ewoCountries;

    @Value("${config.serviceurl.trade-email}")
    private String tradeEmailAPIUrl;

    /**
     * Process the incoming shipment message for creating shipment label and publish the artifact
     *
     * @param message : Incoming Shipment
     */
    @Async("CreateShipmentTaskExecutor")
    public void processRequest(ShipmentArtifact message) {
        String msg = "ShipmentCreateService.processRequest()";
        String shipmentCorrelationId = message.getArtifactHeader().getShipmentCorrelationId().toString();
        String messageCorrelationId = null;
        MDC.put(Constants.X_SHIPMENT_CORRELATION_ID, shipmentCorrelationId);

        log.debug(String.format(PROCESSING_SHIPMENT_REQUEST, shipmentCorrelationId, message));
        try {
            //get shipment message from redis using shipment artifact (Kafka) shipmentCorrelationId
            ShipmentMessage shipmentMessage
                    = redisCacheLoader.getShipmentMessageByCorrelationId(shipmentCorrelationId);
            log.debug(String.format(STANDARD_FIELD_INFO, shipmentCorrelationId, shipmentMessage));

            if (ObjectUtils.isEmpty(shipmentMessage))
                throw new ShipmentMessageException(CODE_NO_SHIPMENT, String.format(STANDARD_DATA_ERROR,
                        msg, "No Shipment Request to process", shipmentCorrelationId), CODE_NO_DATA_SOURCE);

            messageCorrelationId = String.valueOf(shipmentMessage.getMessageHeader().getMessageCorrelationId());

            // Execute Shipment Validations
            validationEngine.runValidation(shipmentMessage, message);

            String finalMessageCorrelationId = messageCorrelationId;
            shipmentMessage.getMessageBody().getShipments().forEach(shipmentInfo ->
                    this.postProcessingShipmentMessage(shipmentInfo, message, finalMessageCorrelationId));

        } catch (Exception e) {
            handleExceptions(e, msg, shipmentCorrelationId, messageCorrelationId);
        }
    }

    /**
     * Start creating the shipment and label
     *
     * @param shipmentInfoAPI   : Incoming Shipment
     * @param routeRuleArtifact Artifacts Maps for 5100
     */
    private void postProcessingShipmentMessage(ShipmentInfo shipmentInfoAPI, ShipmentArtifact routeRuleArtifact,
                                               String messageCorrelationId) {
        String msg = "ShipmentCreateService.postProcessingShipmentMessage()";
        HubInfo retailHubInfo = null;
        try {
            // To Get the Entity Profile
            final String entityCode = String.valueOf(shipmentInfoAPI.getShipmentHeader().getOrigin().getEntityCode());
            // To Get the Carrier Profile
            final String destEntityCode = shipmentInfoAPI.getShipmentHeader().getDestination().getEntityCode();
            final String carrierCode = String.valueOf(routeRuleArtifact.getArtifactBody().getRouteRules().
                    getRuleResult().getTargetCarrierCode());
            final String carrierModeCode = String.valueOf(routeRuleArtifact.getArtifactBody().getRouteRules().
                    getRuleResult().getTargetCarrierModeCode());

            shipmentInfoAPI.getTransitDetails().setTransitMode(carrierModeCode);

            // Load Profile from Redis
            EntityPayload entityProfile = redisRehydrateService.getEntityByCode(entityCode);
            CarrierMainPayload carrierProfile = redisRehydrateService.getCarrierByCode(carrierCode);
            // Load Profile from Redis

            log.debug(String.format(PROFILE_MESSAGE, "Entity", entityCode, entityProfile,
                    shipmentInfoAPI.getShipmentHeader().getShipmentCorrelationId()));
            log.debug(String.format(PROFILE_MESSAGE, "Carrier", carrierCode, carrierProfile,
                    shipmentInfoAPI.getShipmentHeader().getShipmentCorrelationId()));

            // Throw exception in case no if no EntityProfile or CarrierProfile
            if (ObjectUtils.isEmpty(entityProfile) || ObjectUtils.isEmpty(carrierProfile))
                throw new ShipmentMessageException(CODE_PROFILE_NOT_AVAILABLE, "No Entity or Carrier Profile",
                        " entityCode # " + entityCode + " carrierCode # " + carrierCode);

            // Build the Domain ShipmentMessage
            com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage shipmentMessage = ShipmentCreateBRule
                    .buildShipmentMessage(shipmentInfoAPI, messageCorrelationId, entityProfile.getTimezone());
            log.debug(String.format(STANDARD_FIELD_INFO, "ShipmentMessage", shipmentMessage));

            // Get the account to bill for Carrier
            String orderType = shipmentInfoAPI.getOrderDetails().getOrderType();
            Map<String, String> carrierMetaDataMap =
                    accountService.getAccountsDetails(orderType, carrierProfile,
                            carrierCode, carrierModeCode, entityProfile, destEntityCode);

            // Add timezone as well
            carrierMetaDataMap.put(ENTITY_TIMEZONE, entityProfile.getTimezone());
            log.debug(String.format(STANDARD_FIELD_INFO, "ShipmentMessage - Account", carrierMetaDataMap));

            // Generate a new TrackingId
            final String trackingNumber = trackingSeedService.getNextTrackingNumber(carrierMetaDataMap.get(METER_NO));

            shipmentInfoAPI.getTransitDetails().setTrackingNo(trackingNumber);
            log.debug(String.format(STANDARD_FIELD_INFO, "trackingNumber", trackingNumber));

            // Check and update PlannedShipDate
            shipmentInfoAPI = ShipmentCreateBRule.checkAndUpdatePlannedShippedDate(routeRuleArtifact, shipmentInfoAPI);

            // Verify/Update if delivery date is Saturday
            shipmentInfoAPI.getTransitDetails().setSaturdayDelivery(
                    DateUtil.checkDateOfWeekForEPOCDate(shipmentInfoAPI.getTransitDetails().getDateDetails().getPlannedDeliveryDate(), entityProfile.getTimezone()));

            log.debug(String.format(STANDARD_FIELD_INFO, "SaturdayDelivery Flag", shipmentInfoAPI.getTransitDetails().isSaturdayDelivery));

            if(OrderType.isRetailOrder(shipmentInfoAPI.getOrderDetails().getOrderType())) {
                LocationItem destItem = shipmentInfoAPI.getShipmentHeader().getDestination().getAddressTo();
                // Store number to be appended for Retail
                destItem.setContact("%s # %s".formatted(destItem.getContact(), destEntityCode));
                retailHubInfo = getHubInfoIfPMDelivery(entityProfile, shipmentInfoAPI, carrierCode);
            }

            UrsaPayload ursaPayload = fuseLabelService.generateUrsaPayload(shipmentInfoAPI,
                    carrierMetaDataMap, isSaturdayDelivery(entityProfile,carrierModeCode), retailHubInfo);
            TransitTimeInfo transitTimeInfo = buildTransitTimeInfo(shipmentInfoAPI, entityProfile, carrierModeCode);

            //Generate Label
            String labelContent = customLabelService.generateCustomLabel(entityProfile, shipmentInfoAPI,
                    carrierMetaDataMap, ursaPayload, retailHubInfo);
            shipmentInfoAPI.getTransitDetails().getLabelDetails().setLabel(labelContent);

            //Assign Alternate Tracking No
            ShipmentCreateHelper.setAltTrackingNo(shipmentInfoAPI, ursaPayload);

            // Build Shipment Label Artifact
            ShipmentArtifact shipmentArtifact = shipmentArtifactService.buildLabelArtifact(shipmentInfoAPI, entityProfile, messageCorrelationId,
                    carrierCode, carrierModeCode, shipmentMessage.getRequestHeader().getRequestId(), transitTimeInfo,
                    routeRuleArtifact, carrierMetaDataMap.get(ACCOUNT_TO_BILL));

            // Match Label and HUF for Destination Address to Hub deatils
            updateDestinationForRetailPM(shipmentMessage, retailHubInfo);

            // Save Service Enquiry information for manifest and future enquiry
            serviceEnquiryService.saveServiceEnquiry(shipmentMessage, ursaPayload, shipmentArtifact, carrierMetaDataMap,
                    routeRuleArtifact.getArtifactHeader().getShipmentCorrelationId().toString(), entityProfile.getTimezone());

            // Send Shipment Label Artifact
            shipmentArtifactService.sendLabelArtifact(shipmentArtifact);

            // Send AWB/CI to DC ops.
            final String trackingNo = shipmentInfoAPI.getTransitDetails().getTrackingNo();
            final String destCountry = shipmentInfoAPI.getShipmentHeader().getDestination().getAddressTo().getCountry();
            if (ewoCountries.contains(destCountry)) {
                emailTradeDocuments(trackingNo);
            }

        } catch (Exception e) {
            handleExceptions(e, msg, shipmentInfoAPI.getShipmentHeader().getShipmentCorrelationId(), messageCorrelationId);
        }
    }

    private TransitTimeInfo buildTransitTimeInfo(com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo shipmentInfoAPI,
                                                 EntityPayload entityProfile,
                                                 String carrierModeCode) throws ShipmentMessageException {
        String msg = "buildTransitTimeInfo()";
        try {

            AssignedTransitModes assignedTransitMode = getAssignedTransitModes(entityProfile, carrierModeCode);

            long plannedShipDate = shipmentInfoAPI.getTransitDetails().getDateDetails().getPlannedShipDate();
            long plannedDeliveryDate = shipmentInfoAPI.getTransitDetails().getDateDetails().getPlannedDeliveryDate();

            log.debug(String.format(STANDARD_FIELD_INFO, "TransitTime-plannedShipDate", plannedShipDate));
            log.debug(String.format(STANDARD_FIELD_INFO, "TransitTime-plannedDeliveryDate", plannedDeliveryDate));

            return TransitTimeInfo.newBuilder()
                    .setShippedDate(plannedShipDate)
                    .setDeliveredDate(0)
                    .setPlannedDeliveryDate(plannedDeliveryDate)
                    .setCutOffTimeHH(assignedTransitMode.getCutOffHH())
                    .setCutOffTimeMM(assignedTransitMode.getCutOffMM())
                    .setTransitDays(0)
                    .setCutOffTimeApplied(Boolean.FALSE)
                    .setInStoreDate(0)
                    .build();
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw new ShipmentMessageException(CODE_ARTIFACT_CONSUME, "Error occurred in TransitTime population in artifact build",
                    CODE_ARTIFACT_CONSUME_SOURCE);
        }
    }

    public boolean isSaturdayDelivery(EntityPayload entityProfile,
                                      String carrierModeCode) {
        AssignedTransitModes assignedTransitModes = getAssignedTransitModes(entityProfile, carrierModeCode);
        EnumSet<DaysOfWeekUtil.DaysOfWeek> deliveryDay = DaysOfWeekUtil.fromBitValues(assignedTransitModes.getDeliveryDaysMask());
        return deliveryDay.contains(DaysOfWeekUtil.DaysOfWeek.Saturday) || deliveryDay.contains(DaysOfWeekUtil.DaysOfWeek.Sunday);
    }


    public void handleExceptions(Exception exception, String message, String shipmentCorrelationId,
                                 String messageCorrelationId) {
        log.error(STANDARD_ERROR, message, ExceptionUtils.getStackTrace(exception));
        if (exception instanceof ShipmentMessageException) {
            handleShipmentMsgException(exception, message, shipmentCorrelationId, messageCorrelationId);
        } else if (exception instanceof ValidationException) {
            handleValidationException(exception, message, shipmentCorrelationId, messageCorrelationId);
        } else {
            handleGenericException(exception, message, shipmentCorrelationId, messageCorrelationId);
        }
    }

    private void handleGenericException(Exception exception, String message, String shipmentCorrelationId, String messageCorrelationId) {
        shipmentArtifactService.buildAndSendErrorLabelArtifact(
                shipmentCorrelationId,
                messageCorrelationId,
                BUILD_ARTIFACT_TYPE_7900,
                CODE_NO_ARTIFACTS,
                TECHNICAL_SHIPMENT_ERROR + exception.getMessage(),
                CODE_NO_ARTIFACTS_SOURCE);
    }

    private void handleShipmentMsgException(Exception exception, String message, String shipmentCorrelationId, String messageCorrelationId) {
        ShipmentMessageException shipmentMessageException = (ShipmentMessageException) exception;
        shipmentArtifactService.buildAndSendErrorLabelArtifact(
                shipmentCorrelationId,
                messageCorrelationId,
                BUILD_ARTIFACT_TYPE_7900,
                shipmentMessageException.getCode(),
                shipmentMessageException.getDescription(),
                shipmentMessageException.getSource());
    }

    private void handleValidationException(Exception exception, String message, String shipmentCorrelationId, String messageCorrelationId) {
        ValidationException validationException = (ValidationException) exception;
        List<Extended> extended = ValidationUtil.mapList(validationException.getErrorResponse().getExtended(), Extended.class);
        shipmentArtifactService.buildAndSendValidationErrorLabelArtifact(
                shipmentCorrelationId,
                messageCorrelationId,
                BUILD_ARTIFACT_TYPE_7900,
                validationException.getCode(),
                validationException.getMessage(),
                validationException.getSource(),
                extended);
    }

    private static AssignedTransitModes getAssignedTransitModes(EntityPayload entityProfile, String carrierModeCode) {

        for (AssignedTransitModes transitModes :
                entityProfile.getAssignedTransitModes()) {
            if (carrierModeCode.equalsIgnoreCase(transitModes.getModeCode()))
                return transitModes;
        }
        return null;
    }

    public void emailTradeDocuments(@NotEmpty String trackingNo) {
        log.info("Calling emailTradeDocuments API to send AWB & CI to DC Ops team.");
        restTemplate.getForEntity(String.format(tradeEmailAPIUrl, trackingNo), String.class);
    }
}