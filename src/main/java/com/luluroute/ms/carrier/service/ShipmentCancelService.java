package com.luluroute.ms.carrier.service;

import com.logistics.luluroute.avro.shipment.message.ShipmentMessage;
import com.luluroute.ms.carrier.entity.ServiceEnquiry;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.fedex.util.Constants;
import com.luluroute.ms.carrier.fedex.util.ExceptionConstants;
import com.luluroute.ms.carrier.repository.ServiceEnquiryRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import static com.luluroute.ms.carrier.fedex.util.Constants.BUILD_ARTIFACT_CANCEL;
import static com.luluroute.ms.carrier.fedex.util.Constants.BUILD_ARTIFACT_TYPE_7900;
import static com.luluroute.ms.carrier.fedex.util.Constants.STANDARD_ERROR;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.CODE_NO_DATA;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.CODE_NO_DATA_SOURCE;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.CODE_UNKNOWN;
import static com.luluroute.ms.carrier.fedex.util.ExceptionConstants.CODE_UNKNOWN_SOURCE;

@Service
@Slf4j
public class ShipmentCancelService {

    @Autowired
    private ShipmentArtifactService shipmentArtifactService;
    @Autowired
    private ServiceEnquiryRepository serviceEnquiryRepository;

    private String messageCorrelationId = null;


    /**
     * Process the incoming shipment message for creating shipment label and publish the artifact
     *
     * @param message : Incoming Shipment
     */
    @Async("CancelShipmentTaskExecutor")
    public void processShipmentCancelRequest(ShipmentMessage message) {
        String msg = "ShipmentRequestServiceImpl.processShipmentCancelRequest()";

        try {
            if (validateShipmentMessage(message)) {
                messageCorrelationId = message.getMessageHeader().getMessageCorrelationId().toString();

                // Iterate over each Shipment
                message.getMessageBody().getShipments().forEach(shipmentInfo -> {
                    try {
                        if (ObjectUtils.isEmpty(shipmentInfo.getShipmentHeader())
                                || ObjectUtils.isEmpty(shipmentInfo.getShipmentHeader().getShipmentCorrelationId()))
                            throw new ShipmentMessageException(ExceptionConstants.CODE_NO_DATA, "No ShipmentCorrelationId",
                                    ExceptionConstants.CODE_NO_DATA_SOURCE);

                        String shipmentCorrelationId = shipmentInfo.getShipmentHeader().getShipmentCorrelationId().toString();
                        MDC.put(Constants.X_SHIPMENT_CORRELATION_ID, shipmentCorrelationId);

                        List<ServiceEnquiry> serviceEnquiries = serviceEnquiryRepository
                                .findByShipmentCorrelationId(shipmentCorrelationId);

                        if (CollectionUtils.isEmpty(serviceEnquiries)) {
                            log.info(String.format(Constants.STANDARD_FIELD_INFO, "No shipment exists for cancellation:", shipmentCorrelationId));
                            return;
                        }


                        for (ServiceEnquiry serviceEnquiry : serviceEnquiries) {
                            serviceEnquiry.setShipmentStatus(ServiceEnquiry.ShipmentStatus.CANCELLED);
                            serviceEnquiry.setUpdatedBy(Constants.UPDATED_BY);
                            serviceEnquiry.setUpdatedDate(LocalDateTime.now(ZoneOffset.UTC));
                        }

                        shipmentArtifactService.buildAndSendCancelArtifact(shipmentInfo, messageCorrelationId);
                        serviceEnquiryRepository.saveAll(serviceEnquiries);

                    } catch (ShipmentMessageException e) {
                        log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
                        shipmentArtifactService.buildAndSendErrorLabelArtifact(
                                String.valueOf(shipmentInfo.getShipmentHeader().getShipmentCorrelationId()),
                                messageCorrelationId,
                                BUILD_ARTIFACT_TYPE_7900,
                                e.getCode(),
                                e.getDescription(),
                                e.getSource());
                    } catch (Exception e) {
                        log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
                        shipmentArtifactService.buildAndSendErrorLabelArtifact(
                                String.valueOf(shipmentInfo.getShipmentHeader().getShipmentCorrelationId()),
                                messageCorrelationId,
                                BUILD_ARTIFACT_CANCEL,
                                CODE_UNKNOWN,
                                e.getMessage(),
                                CODE_UNKNOWN_SOURCE);
                    }
                });
            }
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            shipmentArtifactService.buildAndSendErrorLabelArtifact(
                    null,
                    messageCorrelationId,
                    BUILD_ARTIFACT_CANCEL,
                    CODE_UNKNOWN,
                    e.getMessage(),
                    CODE_UNKNOWN_SOURCE);
        }
    }


    /**
     * Validate Shipment Message Mandatory Attribute
     *
     * @param message
     * @return
     */
    private boolean validateShipmentMessage(ShipmentMessage message) throws ShipmentMessageException {
        String msg = "ShipmentMessageService.validateShipmentMessage()";
        // check shipmentCorrelationId
        if (message.getMessageHeader().getMessageCorrelationId() == null)
            throw new ShipmentMessageException(CODE_NO_DATA, "No MessageCorrelationId", CODE_NO_DATA_SOURCE);

        // check Shipments
        if (CollectionUtils.isEmpty(message.getMessageBody().getShipments()))
            throw new ShipmentMessageException(CODE_NO_DATA, "No Shipments", CODE_NO_DATA_SOURCE);

        return Boolean.TRUE;
    }

}
