package com.luluroute.ms.carrier.service;

import com.logistics.luluroute.redis.shipment.carriermain.CarrierMainPayload;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.carrier.entity.TrackingSeedCacheEntity;
import com.luluroute.ms.carrier.fedex.util.RedisCacheLoader;
import com.luluroute.ms.carrier.fedex.util.RestPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.luluroute.ms.carrier.fedex.util.Constants.STANDARD_FIELD_INFO;

@Service
@Slf4j
public class RedisRehydrateService {

    @Value("${config.serviceurl.entity}")
    private String entityUrl;
    @Value("${config.serviceurl.carrier}")
    private String carrierUrl;
    @Autowired
    private RestPublisher<EntityPayload> entityPayloadRestPublisher;
    @Autowired
    private RestPublisher<CarrierMainPayload> carrierMainPayloadRestPublisher;
    @Autowired
    private RedisCacheLoader redisCacheLoader;

    /**
     * Gets the entity by code.
     *
     * @param key the entity code
     * @return the entity by code
     */
    public EntityPayload getEntityByCode(String key) {
        EntityPayload entityPayload = redisCacheLoader.getEntityByCode(key);
        log.debug(String.format(STANDARD_FIELD_INFO, "Loading Store Profile from Redis", key));
        if (ObjectUtils.isEmpty(entityPayload)) {
            log.debug(String.format(STANDARD_FIELD_INFO, "Loading Store Profile from DB", key));
            entityPayload = entityPayloadRestPublisher.performRestCall(entityUrl, key, EntityPayload.class);
        }
        return entityPayload;
    }


    /**
     * Gets the carrier by code.
     *
     * @param key the entity code
     * @return the entity by code
     */
    public CarrierMainPayload getCarrierByCode(String key) {
        CarrierMainPayload carrierMainPayload = redisCacheLoader.getCarrierByCode(key);
        log.info(String.format(STANDARD_FIELD_INFO, "Loading Carrier Profile from Redis", key));
        if (ObjectUtils.isEmpty(carrierMainPayload)) {
            log.info(String.format(STANDARD_FIELD_INFO, "Loading Carrier Profile from DB", key));
            carrierMainPayload = carrierMainPayloadRestPublisher.performRestCall(carrierUrl, key, CarrierMainPayload.class);
        }
        return carrierMainPayload;
    }

    public TrackingSeedCacheEntity getTrackingSeedData(String meter){
        return redisCacheLoader.getTrackingSeedData(meter);
    }

    public void updateTrackingSeedCache(String meter, TrackingSeedCacheEntity trackingSeedEntity){
        redisCacheLoader.updateTrackingSeedData(meter, trackingSeedEntity);
    }

}
