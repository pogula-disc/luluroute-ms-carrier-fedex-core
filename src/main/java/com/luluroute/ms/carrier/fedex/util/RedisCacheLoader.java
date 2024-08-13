package com.luluroute.ms.carrier.fedex.util;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.redis.shipment.carriermain.CarrierMainPayload;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.carrier.config.RedisConfig;
import com.luluroute.ms.carrier.entity.TrackingSeedCacheEntity;
import com.luluroute.ms.carrier.entity.TrackingSeedEntity;
import com.luluroute.ms.carrier.repository.TrackingSeedRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;

import static com.luluroute.ms.carrier.fedex.util.Constants.MESSAGE_REDIS_KEY_LOADING;
import static com.luluroute.ms.carrier.fedex.util.Constants.REDIS_KEY_SHIPMENT_MESSAGE;

/**
 * The Class RedisCacheLoader.
 *
 * @author MANDALAKARTHIK1
 */

/**
 * The Constant log.
 */
@Slf4j
@Service
public class RedisCacheLoader {

    @Autowired
    private RedisConfig redisConfig;

    @Autowired
    private ReactiveRedisOperations<String, com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage> reactiveRedisOperations;

    @Autowired
    private TrackingSeedRepository trackingSeedRepository;

    public RedisCacheLoader() {
    }

    /**
     * Gets the entity by code.
     *
     * @param key the entity code
     * @return the entity by code
     */
    @Cacheable(cacheNames = "MSE01-PROFILE", key = "#key", unless = "#result == null")
    public EntityPayload getEntityByCode(String key) {
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "Entity Profile", key));
        return null;
    }

    /**
     * Gets the entity by code.
     *
     * @param key the entity code
     * @return the entity by code
     */
    @Cacheable(cacheNames = "MSCM01-PROFILE", key = "#key", unless = "#result == null")
    public CarrierMainPayload getCarrierByCode(String key) {
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "Carrier Profile", key));
        return null;
    }

    /**
     * @param correlationId
     * @return
     */
    public ShipmentMessage getShipmentMessageByCorrelationId(String correlationId) {
        String key = String.format(REDIS_KEY_SHIPMENT_MESSAGE, redisConfig.keyPrefix, correlationId);
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "SHIPMENTMESSAGE", key, ""));
        return reactiveRedisOperations.opsForValue().get(key).block();
    }

    @Cacheable(value = "FEDEX-FUSE:TRACKINGSEED:DATA", key = "#meter", unless = "#result == null")
    public TrackingSeedCacheEntity getTrackingSeedData(String meter) {
        return getTrackingSeedDataFromDB(meter);
    }

    @CachePut(value = "FEDEX-FUSE:TRACKINGSEED:DATA", key = "#meter", unless = "#result == null")
    public TrackingSeedCacheEntity updateTrackingSeedData(String meter, TrackingSeedCacheEntity trackingSeedEntity) {
        return trackingSeedEntity;
    }

    private TrackingSeedCacheEntity getTrackingSeedDataFromDB(String meter) {
        TrackingSeedEntity trackingSeedEntity = trackingSeedRepository.findByMeterAndActive(meter, 1);
        return TrackingSeedCacheEntity.builder()
                .meter(Integer.parseInt(trackingSeedEntity.getMeter()))
                .id(trackingSeedEntity.getSeedId())
                .beginTN(trackingSeedEntity.getSeedBegin())
                .currentTN(trackingSeedEntity.getSeedCurrent())
                .endTN(trackingSeedEntity.getSeedEnd())
                .build();
    }

}
