package com.luluroute.ms.carrier.service;

import com.logistics.luluroute.avro.artifact.message.*;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.carrier.fedex.util.DateUtil;
import com.luluroute.ms.carrier.topic.producer.ShipmentArtifactsProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.luluroute.ms.carrier.fedex.util.Constants.*;
import static com.luluroute.ms.carrier.service.helper.ShipmentArtifactHelper.*;

@Slf4j
@Service
public class ShipmentArtifactService {

    @Value("${config.carrier.code}")
    private String carrierName;

    @Autowired
    private ShipmentArtifactsProducer artifactsProducer;

    /**
     * @param shipmentInfo : Incoming Shipment
     * @return ShipmentArtifact : Build Shipment Artifacts
     */
    public ShipmentArtifact buildLabelArtifact(
            ShipmentInfo shipmentInfo, EntityPayload entityPayload, String messageCorrelationId, String targetCarrierCode,
            String targetModeCode, String requestId, TransitTimeInfo transitTimeInfo, ShipmentArtifact shipmentArtifact,
            String accountNumber) {
        String msg = "ShipmentArtifactService.buildLabelArtifact()";
        try {
            // Check if label require render on different format or not
            String buildArtifactType = !ObjectUtils.isEmpty(entityPayload.getCustomAttributes()) &&
                    !ObjectUtils.isEmpty(entityPayload.getCustomAttributes().getLabelFormat()) &&
                    !ObjectUtils.isEmpty(shipmentInfo.getTransitDetails().getLabelDetails()) &&
                    !entityPayload.getCustomAttributes().getLabelFormat().equalsIgnoreCase(shipmentInfo.getTransitDetails().getLabelDetails().getFormat()) ?
                    BUILD_ARTIFACT_LABEL_REQUIRE_RENDER : BUILD_ARTIFACT_LABEL_DND_REQUIRE_RENDER;

            ArtifactHeader artifactHeader = ArtifactHeader.newBuilder().setArtifactCreationDay(DateUtil.currentDateTimeInLong())
                    .setArtifactType(buildArtifactType)
                    .setArtifactStatus(BUILD_ARTIFACT_STATUS_COMPLETE)
                    .setShipmentArtifactId(getCorrelationId())
                    .setMessageCorrelationId(messageCorrelationId)
                    .setShipmentCorrelationId(shipmentInfo.getShipmentHeader().getShipmentCorrelationId())
                    .setProcesses(buildCarrierProcessDetails(false, null, null, null))
                    .build();

            LabelInfo labelInfo = LabelInfo.newBuilder().setLabelId(getCorrelationId())
                    .setContentFromCarrier(shipmentInfo.getTransitDetails().getLabelDetails().getLabel())
                    .setFormatFromCarrier(entityPayload.getCustomAttributes().getLabelFormat())
                    .setFormatRendered(shipmentInfo.getTransitDetails().getLabelDetails().getFormat())
                    .setRotationDegrees(entityPayload.getCustomAttributes().getLabelRotation())
                    .setGenerationDate(DateUtil.getCurrentTime()).build();

            shipmentArtifact.setArtifactHeader(artifactHeader);
            shipmentArtifact.getArtifactBody().setLabelInfo(labelInfo);
            shipmentArtifact.getArtifactBody().setCarrierExternalData(buildCarrierExternalDataInfo(shipmentInfo, targetModeCode,
                    targetCarrierCode, carrierName, requestId, transitTimeInfo, accountNumber));

            log.info(String.format(STANDARD_FIELD_INFO, "ShipmentArtifactLabel", shipmentArtifact));

            return shipmentArtifact;

        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * @param labelArtifact : Incoming ShipmentArtifact
     * @return ShipmentArtifact : Send Shipment Artifacts
     */
    public void sendLabelArtifact(ShipmentArtifact labelArtifact) {
        String msg = "ShipmentArtifactService.sendLabelArtifact()";
        try {
            // Send Shipment Label Artifact
            artifactsProducer.sendPayload(labelArtifact);

        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * @param shipmentCorrelationId : Incoming shipmentCorrelationId
     */
    public void buildAndSendErrorLabelArtifact(String shipmentCorrelationId,
                                               String messageCorrelationId,
                                               String artifactType,
                                               String errorCode,
                                               String errorMessage,
                                               String errorSource) {
        String msg = "ShipmentArtifactService.buildAndSendErrorLabelArtifact()";
        try {

            ArtifactHeader artifactHeader = ArtifactHeader.newBuilder()
                    .setArtifactCreationDay(DateUtil.currentDateTimeInLong())
                    .setArtifactType(artifactType)
                    .setArtifactStatus(BUILD_ARTIFACT_STATUS_ERROR)
                    .setShipmentArtifactId(getCorrelationId())
                    .setMessageCorrelationId(!StringUtils.isEmpty(messageCorrelationId)
                            ? messageCorrelationId : STRING_EMPTY)
                    .setShipmentCorrelationId(shipmentCorrelationId)
                    .setProcesses(buildCarrierProcessDetails(true,
                            errorCode, errorMessage, errorSource)).build();

            LabelInfo labelInfo = LabelInfo.newBuilder().setLabelId(getCorrelationId())
                    .setContentFromCarrier(STRING_EMPTY)
                    .setFormatFromCarrier(STRING_EMPTY)
                    .setFormatRendered(STRING_EMPTY)
                    .setGenerationDate(DateUtil.getCurrentTime()).build();

            ShipmentArtifact labelArtifact = ShipmentArtifact.newBuilder().setArtifactHeader(artifactHeader)
                    .setArtifactBody(ArtifactBody.newBuilder().setLabelInfo(labelInfo).build()).build();

            log.info(String.format(STANDARD_FIELD_INFO, "ShipmentArtifactError", labelArtifact));

            // Send Shipment Label Artifact
            artifactsProducer.sendPayload(labelArtifact);

        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    public void buildAndSendValidationErrorLabelArtifact(String shipmentCorrelationId,
                                               String messageCorrelationId,
                                               String artifactType,
                                               String errorCode,
                                               String errorMessage,
                                               String errorSource,
                                               List<Extended> extended) {
        String msg = "ShipmentArtifactService.buildAndSendErrorLabelArtifact()";
        try {

            ArtifactHeader artifactHeader = ArtifactHeader.newBuilder()
                    .setArtifactCreationDay(DateUtil.currentDateTimeInLong())
                    .setArtifactType(artifactType)
                    .setArtifactStatus(BUILD_ARTIFACT_STATUS_ERROR)
                    .setShipmentArtifactId(getCorrelationId())
                    .setMessageCorrelationId(!StringUtils.isEmpty(messageCorrelationId)
                            ? messageCorrelationId : STRING_EMPTY)
                    .setShipmentCorrelationId(shipmentCorrelationId)
                    .setProcesses(buildCarrierValidationProcessDetails(true,
                            errorCode, errorMessage, errorSource, extended)).build();

            ShipmentArtifact labelArtifact = ShipmentArtifact.newBuilder().setArtifactHeader(artifactHeader)
                    .setArtifactBody(ArtifactBody.newBuilder().build()).build();

            log.info(String.format(STANDARD_FIELD_INFO, "ShipmentArtifactError", labelArtifact));

            // Send Shipment Label Artifact
            artifactsProducer.sendPayload(labelArtifact);

        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    /**
     * @param shipmentInfo : Incoming Shipment
     * @return ShipmentArtifact : Build Shipment Artifacts
     */
    public void buildAndSendCancelArtifact(com.logistics.luluroute.avro.shipment.service.ShipmentInfo shipmentInfo,
                                           String messageCorrelationId) {
        String msg = "ShipmentArtifactService.buildAndSendCancelArtifact()";
        try {

            ArtifactHeader artifactHeader = ArtifactHeader.newBuilder()
                    .setArtifactCreationDay(DateUtil.currentDateTimeInLong()).setArtifactType(BUILD_ARTIFACT_CANCEL)
                    .setArtifactStatus(BUILD_ARTIFACT_STATUS_COMPLETE).setShipmentArtifactId(getCorrelationId())
                    .setMessageCorrelationId(messageCorrelationId)
                    .setShipmentCorrelationId(shipmentInfo.getShipmentHeader().getShipmentCorrelationId())
                    .setProcesses(buildCarrierProcessDetails(false, null, null, null)).build();

            ShipmentArtifact cancelArtifact = ShipmentArtifact.newBuilder().setArtifactHeader(artifactHeader)
                    .setArtifactBody(ArtifactBody.newBuilder().build()).build();

            log.info(String.format(STANDARD_FIELD_INFO, "ShipmentArtifactCancel", cancelArtifact));

            // Send Shipment Cancel Artifact
            artifactsProducer.sendPayload(cancelArtifact);

        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }
}
