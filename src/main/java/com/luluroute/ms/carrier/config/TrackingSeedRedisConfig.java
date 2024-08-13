package com.luluroute.ms.carrier.config;

import com.luluroute.ms.carrier.entity.TrackingSeedCacheEntity;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@NoArgsConstructor
//@Getter
public class TrackingSeedRedisConfig {

    @Value("${trackingseed.seeddata.key}")
    public String trackingSeedKey;

    @Value("${trackingseed.counter.key-prefix}")
    public String counterKeyPrefix;

    @Bean
    public RedisTemplate<String, TrackingSeedCacheEntity> trackingSeedRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, TrackingSeedCacheEntity> trackingSeedEntityRedisTemplate = new RedisTemplate<>();
        trackingSeedEntityRedisTemplate.setKeySerializer(new StringRedisSerializer());
        trackingSeedEntityRedisTemplate.setHashKeySerializer(new StringRedisSerializer());
        trackingSeedEntityRedisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(TrackingSeedCacheEntity.class));
        trackingSeedEntityRedisTemplate.setDefaultSerializer(new Jackson2JsonRedisSerializer<>(TrackingSeedCacheEntity.class));
        trackingSeedEntityRedisTemplate.setConnectionFactory(redisConnectionFactory);
        trackingSeedEntityRedisTemplate.afterPropertiesSet();
        return trackingSeedEntityRedisTemplate;

    }

    @Bean
    public RedisTemplate<String, Long> counterTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Long> trackingSeedEntityRedisTemplate = new RedisTemplate<>();
        trackingSeedEntityRedisTemplate.setKeySerializer(new StringRedisSerializer());
        trackingSeedEntityRedisTemplate.setHashKeySerializer(new StringRedisSerializer());
        trackingSeedEntityRedisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(TrackingSeedCacheEntity.class));
        trackingSeedEntityRedisTemplate.setDefaultSerializer(new Jackson2JsonRedisSerializer<>(TrackingSeedCacheEntity.class));
        trackingSeedEntityRedisTemplate.setConnectionFactory(redisConnectionFactory);
        trackingSeedEntityRedisTemplate.afterPropertiesSet();
        return trackingSeedEntityRedisTemplate;

    }

    @Bean(name = "IdempotentRedisTemplate")
    public RedisTemplate<String, String> sftpIdempotentConsumerTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
