package com.luluroute.ms.carrier.topic.consumer;

import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import com.lululemon.wms.integration.common.kafka.AbstractTopicConsumer;
import com.lululemon.wms.integration.common.kafka.EventHeaders;
import com.luluroute.ms.carrier.fedex.util.Constants;
import com.luluroute.ms.carrier.fedex.util.DateUtil;
import com.luluroute.ms.carrier.service.ShipmentCreateService;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.luluroute.ms.carrier.fedex.util.Constants.*;
import static com.luluroute.ms.carrier.service.helper.ShipmentCreateHelper.validCarrierCode;

@Slf4j
@Component
public class ShipmentCreateConsumer extends AbstractTopicConsumer<ShipmentArtifact> {

    @Value("${config.carrier.code}")
    private String enabledCarrierCode;
    @Autowired
    private ShipmentCreateService shipmentCreateService;

    @Autowired
    private Counter successCounter;

    public ShipmentCreateConsumer(@Value("${config.shipmentartifacts.topic}") String mainTopicName,
                                  @Value("${config.shipmentartifacts.topic}") String retryTopicName,
                                  @Value("${config.shipmentartifacts.topic}") String dlqTopicName) {

        super(mainTopicName, null, null);
    }

    /**
     * Thread 1 - Main Topic Consumer
     *
     * @param message - Message Payload
     * @param headers - Headers as a Map
     * @param ack     - Manually control offset commit
     */
    @Override
    @KafkaListener(id = "${config.shipmentartifacts.topic}", topics = "${config.shipmentartifacts.topic}", groupId = "${config.shipmentartifacts.consumerGroup}")
    public void consumeMessage(@Payload ShipmentArtifact message, @Headers MessageHeaders headers, Acknowledgment ack) {
        if (validCarrierCode(headers, enabledCarrierCode)) {
            super.consumeMessage(message, headers, ack);
        } else {
            ack.acknowledge();
        }
    }

    @Override
    protected void processMessageImpl(ShipmentArtifact shipmentArtifact, EventHeaders headers) {
        String msg = "ShipmentCreateConsumer.processMessageImpl()";
        if (shipmentArtifact != null && shipmentArtifact.getArtifactHeader() != null && shipmentArtifact.getArtifactBody() != null) {
            try {
                if (!ObjectUtils.isEmpty(shipmentArtifact.getArtifactBody()) &&
                        !ObjectUtils.isEmpty(shipmentArtifact.getArtifactBody().getRouteRules()) &&
                        !ObjectUtils.isEmpty(shipmentArtifact.getArtifactBody().getRouteRules().getRuleResult()) &&
                        !ObjectUtils.isEmpty(shipmentArtifact.getArtifactBody().getRouteRules().getRuleResult().getTargetCarrierCode()) &&
                        BUILD_ARTIFACT_STATUS_COMPLETE == shipmentArtifact.getArtifactHeader().getArtifactStatus() &&
                        BUILD_ARTIFACT_ROUTE_RULES.equalsIgnoreCase(String.valueOf(shipmentArtifact.getArtifactHeader().getArtifactType())) &&
                        String.valueOf(shipmentArtifact.getArtifactBody().getRouteRules()
                                .getRuleResult().getTargetCarrierCode()).equalsIgnoreCase(enabledCarrierCode)) {
                    // Process only Shipment Request-Create

                    log.info(String.format(MESSAGE, FEDEX_CARRIER_CONSUMER, shipmentArtifact));
                    addLoggingContext(shipmentArtifact);
                    shipmentCreateService.processRequest(shipmentArtifact);
                    successCounter.increment();
                    log.info("APP_MESSAGE=\"Received AVRO message and processed successfully\"");
                }
            } catch (Exception e) {
                log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            }
        } else {
            log.error(STANDARD_ERROR, msg, String.format("INVALID NULL Message {%s}", shipmentArtifact));
        }
        MDC.clear();
    }


    public void addLoggingContext(ShipmentArtifact message) {
        if (message != null && message.getArtifactHeader() != null) {
            MDC.put(Constants.X_MESSAGE_CORRELATION_ID, String.valueOf(message.getArtifactHeader().getMessageCorrelationId()));
        }
        MDC.put(X_JOB_START_TIME, String.valueOf(DateUtil.getCurrentTime()));
        MDC.put(X_CARRIER_STREAM_FUNCTION_ID, FEDEX_CARRIER_CONSUMER);
    }


    public void removeLoggingContext() {
        MDC.remove(Constants.X_SHIPMENT_CORRELATION_ID);
        MDC.remove(Constants.X_MESSAGE_CORRELATION_ID);
        MDC.remove(Constants.X_JOB_START_TIME);
        MDC.remove(Constants.X_CARRIER_STREAM_FUNCTION_ID);
    }

    @Override
    public void addLoggingContext(ShipmentArtifact message, MessageHeaders headers) {
        if (message != null && message.getArtifactHeader() != null && message.getArtifactBody() != null) {
            MDC.put(Constants.X_MESSAGE_CORRELATION_ID, String.valueOf(message.getArtifactHeader().getMessageCorrelationId()));
        }
        MDC.put(X_CARRIER_STREAM_FUNCTION_ID, FEDEX_CARRIER_CONSUMER);
        MDC.put(X_JOB_START_TIME, String.valueOf(DateUtil.getCurrentTime()));
    }
}
