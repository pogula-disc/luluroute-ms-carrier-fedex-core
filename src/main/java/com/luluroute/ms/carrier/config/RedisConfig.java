package com.luluroute.ms.carrier.config;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import io.lettuce.core.ReadFrom;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableCaching
@Getter
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.ttl}")
    private int cacheClearTime;

    @Value("${config.shipmentmessage.redis.key}")
    public String keyPrefix;

    @Value("${package.sequence.key-prefix}")
    public String seqKeyPrefix;

    @Bean
    public RedisTemplate<String, Serializable> redisCacheTemplate(LettuceConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Serializable> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public RedisTemplate<String, Long> packageSeqRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setDefaultSerializer(new Jackson2JsonRedisSerializer<>(Long.class));
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializationContext.SerializationPair<Object> jsonSerializer = RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer());
        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(factory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(cacheClearTime))
                        .serializeValuesWith(jsonSerializer))
                .build();
    }

    @Bean
    public ReactiveRedisOperations<String, ShipmentMessage> RedisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisSerializer<ShipmentMessage> valueSerializer = new Jackson2JsonRedisSerializer<>(ShipmentMessage.class);
        RedisSerializationContext<String, ShipmentMessage> serializationContext = RedisSerializationContext.<String, ShipmentMessage>newSerializationContext(RedisSerializer.string())
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(lettuceConnectionFactory, serializationContext);
    }
    @Bean
    @ConditionalOnProperty(prefix = "spring.redis.cluster", name = "enabled", havingValue = "true")
    LettuceConnectionFactory redisConnectionFactory(RedisClusterConfiguration redisConfiguration) {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .readFrom(ReadFrom.REPLICA_PREFERRED).build();
        return new LettuceConnectionFactory(redisConfiguration, clientConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.redis.cluster", name = "enabled", havingValue = "true")
    RedisClusterConfiguration redisConfiguration() {
        List<String> list = new ArrayList<>();
        list.add(redisHost);
        RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(list);
        redisClusterConfiguration.setMaxRedirects(3);
        return redisClusterConfiguration;
    }
}
