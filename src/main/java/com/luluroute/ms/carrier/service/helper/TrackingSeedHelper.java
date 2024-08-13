package com.luluroute.ms.carrier.service.helper;

import com.luluroute.ms.carrier.config.TrackingSeedRedisConfig;
import com.luluroute.ms.carrier.entity.TrackingSeedCacheEntity;
import com.luluroute.ms.carrier.fedex.exception.ShipmentMessageException;
import com.luluroute.ms.carrier.fedex.util.Constants;
import com.luluroute.ms.carrier.fedex.util.ExceptionConstants;
import com.luluroute.ms.carrier.repository.TrackingSeedRepository;
import com.luluroute.ms.carrier.service.RedisRehydrateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static java.lang.Boolean.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingSeedHelper {

    final RedisTemplate<String, Long> counterTemplate;

    final TrackingSeedRedisConfig redisConfig;
    @Autowired
    private TrackingSeedRepository seedRepository;

    @Autowired
    RedisRehydrateService redisService;

    @Async("DBTaskExecutor")
    public  void asyncUpdateTrackingSeed(String meterNo, Long nextValue){
        log.debug(String.format(Constants.STANDARD_FIELD_INFO, "ASYNC UPDATE ", nextValue));
        seedRepository.updateCurrentTrackingSeed(nextValue, meterNo);
    }

    public Long incrementCounter(String meter, long initialValue) {
        String counterKey = redisConfig.counterKeyPrefix + meter;
        boolean hasKey = TRUE.equals(counterTemplate.hasKey(counterKey));
        log.debug(String.format(Constants.STANDARD_FIELD_INFO, "counter-key", redisConfig.counterKeyPrefix + meter));
        RedisAtomicLong counter;
        if(hasKey){
            counter = new RedisAtomicLong(counterKey, counterTemplate);
        }
        else{
            counter = new RedisAtomicLong(counterKey, counterTemplate, initialValue);
        }
        return counter.incrementAndGet();
    }


    public Long getNextTrackingNumber(String hashKey) throws ShipmentMessageException {
        TrackingSeedCacheEntity trackingSeedEntity = redisService.getTrackingSeedData(hashKey);
        log.debug(String.format(Constants.STANDARD_FIELD_INFO, "tracking-seed-meter", redisConfig.trackingSeedKey + ":"+ hashKey));
        if (trackingSeedEntity == null)
            throw new ShipmentMessageException(ExceptionConstants.CODE_NO_TRACKING_SEED, "No Tracking seed data found",
                    ExceptionConstants.CODE_NO_DATA_REDIS_CACHE);
        Long counter = incrementCounter(hashKey, trackingSeedEntity.getCurrentTN());

        log.debug(String.format(Constants.STANDARD_FIELD_INFO, "next-counter", counter));
        if (counter <= trackingSeedEntity.getEndTN()) {
            trackingSeedEntity.setCurrentTN(counter);
            redisService.updateTrackingSeedCache(hashKey, trackingSeedEntity);
            return counter;
        }
        else
            throw new ShipmentMessageException(ExceptionConstants.CODE_TRACKING_SEED_EXCEEDED, "Tracking seed exceeded",
                    ExceptionConstants.CODE_NO_DATA_POSTGRES_SOURCE);

    }

}
