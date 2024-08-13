package com.luluroute.ms.carrier.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.common.errors.SerializationException;

@Slf4j
public class CustomAvroDeserializer extends KafkaAvroDeserializer {

    @Override
    public Object deserialize(String s, byte[] bytes) {
        try {
            return deserialize(bytes);
        } catch (SerializationException e) {
            log.error("Error Occurred {}", ExceptionUtils.getStackTrace(e));
            return null;
        }
    }
    @Override
    public Object deserialize(String s, byte[] bytes, Schema readerSchema) {
        try {
            return deserialize(bytes, readerSchema);
        } catch (SerializationException e) {
            log.error("Error Occurred {}", ExceptionUtils.getStackTrace(e));
            return null;
        }
    }
}
