package com.luluroute.ms.carrier.config;


import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

import static io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class KafkaConfig {

    public static final String SECURITY_PROTOCOL_KEY = "security.protocol";
    public static final String SASL_MECHANISM_KEY = "sasl.mechanism";
    public static final String SASL_JAAS_CONFIG_KEY = "sasl.jaas.config";

    @Value("${spring.kafka.bootstrap-servers}")
    private String primaryBootstrapServers;
    @Value("${spring.kafka.properties.schema.registry.url}")
    private String primarySchemaRegistry;
    @Value("${spring.kafka.properties.sasl.jaas.config}")
    private String primarySaslJaasConfig;
    @Value("${spring.kafka.properties.security.protocol}")
    private String securityProtocol;
    @Value("${spring.kafka.properties.auto.register.schemas}")
    private String autoRegisterSchemas;
    @Value("${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism;
    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;
    @Value("${spring.kafka.properties.basic.auth.user.info}")
    private String avroSchemaUserInfo;

    @Bean
    public ProducerFactory<String, ShipmentArtifact> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, primaryBootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, ShipmentArtifact> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    @Bean
    @Primary
    public ConsumerFactory<Object, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, primaryBootstrapServers);
        config.put(SCHEMA_REGISTRY_URL_CONFIG, primarySchemaRegistry);
        config.put(SASL_JAAS_CONFIG_KEY, primarySaslJaasConfig);
        addCommonConfig(config);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Integer, String> containerFactory() {
        ConcurrentKafkaListenerContainerFactory<Integer, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    private void addCommonConfig(Map<String, Object> config) {
        config.put(SECURITY_PROTOCOL_KEY, securityProtocol);
        config.put(SASL_MECHANISM_KEY, saslMechanism);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);

    }

}
