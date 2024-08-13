package com.luluroute.ms.carrier.topic.producer;

import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.luluroute.ms.carrier.fedex.util.Constants.MESSAGE_PUBLISHED;
import static com.luluroute.ms.carrier.fedex.util.Constants.STANDARD_ERROR;

@Slf4j
@Service
@NoArgsConstructor
public class ShipmentArtifactsProducer {

    @Value("${config.shipmentartifacts.topic}")
    private String topicName;

    @Autowired
    private KafkaTemplate<String, ShipmentArtifact> kafkaTemplate;

    /**
     * Send Avro payload message to FactoryASN topic
     *
     * @param shipmentArtifact
     */
    public void sendPayload(ShipmentArtifact shipmentArtifact) {
        String msg = "ShipmentArtifactsProducer.sendPayload()";
        try {
            log.info("{} - Preparing ShipmentArtifactsProducer message for key # {}", msg,
                    shipmentArtifact.getArtifactHeader().getShipmentCorrelationId());

            Headers headers = new RecordHeaders();
            kafkaTemplate.send(shipmentArtifactRecord(shipmentArtifact,
                    String.valueOf(shipmentArtifact.getArtifactHeader().getShipmentCorrelationId()),
                    headers));

            log.info(MESSAGE_PUBLISHED,
                    shipmentArtifact.getArtifactHeader().getShipmentCorrelationId(),
                    shipmentArtifact, topicName);

        } catch (KafkaException kfe) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(kfe));
            throw kfe;
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    private <V> ProducerRecord<String, V> shipmentArtifactRecord(V value, String key, Headers headers) {
        return new ProducerRecord<>(topicName, null, key, value, headers);
    }
}
