package com.luluroute.ms.carrier.service.helper;

import com.logistics.luluroute.avro.artifact.message.CarrierExternalDataInfo;
import com.logistics.luluroute.avro.artifact.message.CostSummaryInfo;
import com.logistics.luluroute.avro.artifact.message.Extended;
import com.logistics.luluroute.avro.artifact.message.ProcessException;
import com.logistics.luluroute.avro.artifact.message.Processes;
import com.logistics.luluroute.avro.artifact.message.TrackingInfo;
import com.logistics.luluroute.avro.artifact.message.TransitTimeInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.luluroute.ms.carrier.fedex.util.DateUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import static com.luluroute.ms.carrier.fedex.util.Constants.LABEL_PROCESS_CODE;
import static com.luluroute.ms.carrier.fedex.util.Constants.LABEL_PROCESS_NAME;
import static com.luluroute.ms.carrier.fedex.util.Constants.PROCESS_STATUS_COMPLETED;
import static com.luluroute.ms.carrier.fedex.util.Constants.PROCESS_STATUS_FAILED;
import static com.luluroute.ms.carrier.fedex.util.Constants.STANDARD_ERROR;
import static com.luluroute.ms.carrier.fedex.util.Constants.X_JOB_START_TIME;
import static com.luluroute.ms.carrier.fedex.util.Constants.getCorrelationId;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.CARRIER_NAME;

@Slf4j
@Component
public class ShipmentArtifactHelper {

    /**
     * @return
     */
    public static List<Processes> buildCarrierProcessDetails(boolean isErrorOccurred,
                                                             String errorCode,
                                                             String errorMessage,
                                                             String errorSource) {
        String msg = "ShipmentArtifactService.buildCarrierProcessDetails()";
        List<Processes> processes = new ArrayList<>();
        try {
            Processes carrierProcess = Processes.newBuilder()
                    .setProcessCode(LABEL_PROCESS_CODE)
                    .setProcessName(LABEL_PROCESS_NAME)
                    .setProcessStatus(isErrorOccurred == false ? PROCESS_STATUS_FAILED : PROCESS_STATUS_COMPLETED)
                    .setStartTime(MDC.get(X_JOB_START_TIME) != null ? Long.valueOf(MDC.get(X_JOB_START_TIME)) : DateUtil.currentDateTimeInLong())
                    .setEndTime(DateUtil.currentDateTimeInLong())
                    .setOperationId(getCorrelationId()).build();

            // Add any exception
            if (!StringUtils.isEmpty(errorCode)) {
                carrierProcess.setProcessException(ProcessException.newBuilder()
                        .setCode(errorCode)
                        .setDescription(CARRIER_NAME + errorMessage)
                        .setSource(errorSource).build());
            }
            processes.add(carrierProcess);
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
        return processes;
    }

    public static List<Processes> buildCarrierValidationProcessDetails(boolean isErrorOccurred,
                                                             String errorCode,
                                                             String errorMessage,
                                                             String errorSource,
                                                             List<Extended> extended) {
        String msg = "ShipmentArtifactService.buildCarrierValidationProcessDetails()";
        List<Processes> processes = new ArrayList<>();
        try {
            processes = buildCarrierProcessDetails(isErrorOccurred, errorCode, errorMessage, errorSource);
            if (!CollectionUtils.isEmpty(processes)) {
                processes.get(0).setExtended(extended);
            }
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
        return processes;

    }

    public static CarrierExternalDataInfo buildCarrierExternalDataInfo(ShipmentInfo shipmentInfo, String targetModeCode,
                                                                       String targetCarrierCode, String carrierName, String requestId, TransitTimeInfo transitTimeInfo,
                                                                       String accountNumber) {

        return CarrierExternalDataInfo.newBuilder()
                .setShipmentId(shipmentInfo.getShipmentHeader().getShipmentCorrelationId())
                .setCreationDate(String.valueOf(DateUtil.utcCurrentDatetime()))
                .setEstimatedDeliveryDate(
                        String.valueOf(shipmentInfo.getTransitDetails().getDateDetails().getEstimatedDeliveryDate()))
                .setTransitTime(Double.valueOf(shipmentInfo.getTransitDetails().getDateDetails().getTransitDays()))
                .setModeCode(targetModeCode).setCarrierCode(targetCarrierCode).setCarrierName(carrierName)
                .setRequestId(requestId)
                .setWeight(shipmentInfo.getShipmentPieces().get(0).getCartonsDetails().get(0).getWeightDetails()
                        .getValue())
                .setCostSummary(CostSummaryInfo.newBuilder().setChargeToAccount(null).setTotalCost(null)
                        .setTotalCostExTaxes(null).setShippingCost(null).setFuelSurcharge(null)
                        .setAdditionalCharges(0.00).setTotalTaxes(null).build())
                .setTracking(buildTrackingInfo(shipmentInfo, accountNumber)).setAuthorityToLeave(true).setContainsDangerousGoods(false)
                .setSafeDropEnabled(false).setAllowPartialDelivery(true).setTransitTimeInfo(transitTimeInfo).build();

    }

    public static TrackingInfo buildTrackingInfo(ShipmentInfo shipmentInfo, String accountNumber) {
        return TrackingInfo.newBuilder().setTrackingId(shipmentInfo.getTransitDetails().getTrackingNo())
                .setAltTrackingId(shipmentInfo.getTransitDetails().getAltTrackingNo()).setArticleId(null)
                .setConsignmentId(shipmentInfo.getTransitDetails().getAltTrackingNo()).setReferences(Arrays.asList(accountNumber)).build();
    }
}
