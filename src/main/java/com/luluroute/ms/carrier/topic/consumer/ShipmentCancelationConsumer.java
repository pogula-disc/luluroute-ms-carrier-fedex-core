package com.luluroute.ms.carrier.topic.consumer;

import com.logistics.luluroute.avro.shipment.message.ShipmentMessage;
import com.lululemon.wms.integration.common.kafka.AbstractTopicConsumer;
import com.lululemon.wms.integration.common.kafka.EventHeaders;
import com.luluroute.ms.carrier.fedex.util.Constants;
import com.luluroute.ms.carrier.fedex.util.DateUtil;
import com.luluroute.ms.carrier.service.ShipmentCancelService;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
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

import static com.luluroute.ms.carrier.fedex.util.Constants.CARRIER_CODE_HEADER_KEY;
import static com.luluroute.ms.carrier.fedex.util.Constants.FEDEX_CARRIER_CANCEL_CONSUMER;
import static com.luluroute.ms.carrier.fedex.util.Constants.SHIPMENT_REQUEST_CANCEL;
import static com.luluroute.ms.carrier.fedex.util.Constants.STANDARD_ERROR;
import static com.luluroute.ms.carrier.fedex.util.Constants.X_CARRIER_STREAM_FUNCTION_ID;
import static com.luluroute.ms.carrier.fedex.util.Constants.X_JOB_START_TIME;
import static com.luluroute.ms.carrier.service.helper.ShipmentCreateHelper.validCarrierCode;

@Slf4j
@Component
public class ShipmentCancelationConsumer extends AbstractTopicConsumer<ShipmentMessage> {

    @Autowired
    private ShipmentCancelService shipmentCancelService;

    @Value("${config.carrier.code}")
    private String enabledCarrierCode;

    @Autowired
    private Counter successCounter;

    public ShipmentCancelationConsumer(@Value("${config.shipmentmessage.topic}") String mainTopicName,
                                       @Value("${config.shipmentmessage.topic}") String retryTopicName,
                                       @Value("${config.shipmentmessage.topic}") String dlqTopicName) {

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
    @KafkaListener(id = "${config.shipmentmessage.topic}", topics = "${config.shipmentmessage.topic}", groupId = "${config.shipmentmessage.consumerGroup}")
    public void consumeMessage(@Payload ShipmentMessage message, @Headers MessageHeaders headers, Acknowledgment ack) {
        if (validCarrierCode(headers, enabledCarrierCode)) {
            super.consumeMessage(message, headers, ack);
        } else {
            ack.acknowledge();
        }
    }

    @Override
    protected void processMessageImpl(ShipmentMessage message, EventHeaders headers) {
        String msg = "ShipmentMessageTopicConsumer.processMessageImpl()";
        if (message != null && message.getRequestHeader() != null && message.getMessageBody() != null) {
            try {
                if (SHIPMENT_REQUEST_CANCEL.equalsIgnoreCase(String.valueOf(message.getRequestHeader().getRequestType()))) {
                    // Process only Shipment Request-Cancel
                    log.info(String.format(Constants.MESSAGE, FEDEX_CARRIER_CANCEL_CONSUMER, message));
                    shipmentCancelService.processShipmentCancelRequest(message);
                    successCounter.increment();
                }
            } catch (Exception e) {
                log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            }
        } else {
            log.error(STANDARD_ERROR, msg, String.format("INVALID NULL Message {%s}", message));
        }
        MDC.clear();
    }

    @Override
    public void addLoggingContext(ShipmentMessage message, MessageHeaders headers) {
        if (message != null && message.getMessageHeader() != null && message.getRequestHeader() != null) {
            MDC.put(Constants.X_MESSAGE_CORRELATION_ID, String.valueOf(message.getMessageHeader().getMessageCorrelationId()));
        }
        MDC.put(X_CARRIER_STREAM_FUNCTION_ID, FEDEX_CARRIER_CANCEL_CONSUMER);
        MDC.put(X_JOB_START_TIME, String.valueOf(DateUtil.getCurrentTime()));
    }

    @Override
    public void removeLoggingContext() {
        MDC.remove(Constants.X_SHIPMENT_CORRELATION_ID);
        MDC.remove(Constants.X_MESSAGE_CORRELATION_ID);
        MDC.remove(Constants.X_JOB_START_TIME);
        MDC.remove(X_CARRIER_STREAM_FUNCTION_ID);
    }

}
