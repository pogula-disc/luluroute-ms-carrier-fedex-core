package com.luluroute.ms.carrier.rateshop.service;

import com.logistics.luluroute.carrier.fedex.rateshop.entity.FedexRateshopZoneCache;
import com.logistics.luluroute.carrier.fedex.rateshop.service.FedexRateshopHelper;
import com.logistics.luluroute.carrier.fedex.rateshop.service.FedexRateshopRedisService;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierInput;
import com.luluroute.ms.carrier.rateshop.entity.RateshopRateDb;
import com.luluroute.ms.carrier.rateshop.model.RateShopCacheAndDb;
import com.luluroute.ms.carrier.rateshop.service.helper.RateshopHelper;
import com.luluroute.ms.carrier.rateshop.util.RateshopRedisCacheLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.logistics.luluroute.carrier.fedex.rateshop.service.FedexRateshopHelper.isDestinationRequiresZip;
import static com.logistics.luluroute.carrier.fedex.rateshop.util.FedexRateshopConstants.LOCATION_MILITARY;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisRehydrateRateshopZoneService {

    private final RateshopRedisCacheLoader rateshopRedisCacheLoader;
    private final RateshopHelper rateshopHelper;
    private final FedexRateshopRedisService fedexRateshopRedisService;


    public RateShopCacheAndDb hydrateRateshopZoneCache(RateShopCarrierInput inputData) {
        Set<RateshopRateDb> rateshopRateDbs = null;
        FedexRateshopZoneCache fedexRateshopZoneCache =
                fedexRateshopRedisService.checkCacheForRateshopZone(inputData);
        if(fedexRateshopZoneCache == null) {
            rateshopRateDbs = rateshopHelper.populateRatesFromDb(inputData);
            fedexRateshopZoneCache = rateshopRateDbs.stream()
                    .findFirst()
                    .map(rateshopRateDb -> cacheNewRateShopZone(inputData, rateshopRateDb))
                    .orElse(null);
        }

        return RateShopCacheAndDb.builder()
                .rateshopRateDbs(rateshopRateDbs)
                .fedexRateshopZoneCache(fedexRateshopZoneCache)
                .build();
    }

    private FedexRateshopZoneCache cacheNewRateShopZone(RateShopCarrierInput inputData, RateshopRateDb rateshopRateDb) {
        String origin;
        String destination;
        if (inputData.isMilitary()) {
            origin = LOCATION_MILITARY;
            destination = LOCATION_MILITARY;
        } else if(isDestinationRequiresZip(inputData)) {
            origin = inputData.getOriginZipCode();
            destination = rateshopRateDb.getDestinationZipCodePrefix();
        } else {
            origin = inputData.getOriginCountry();
            destination = inputData.getDestinationCountry();
        }

        FedexRateshopZoneCache fedexRateshopZoneCache = FedexRateshopZoneCache.builder()
                .rateshopZone(rateshopRateDb.getRateshopZone())
                .build();
        String rateShopZoneKey = FedexRateshopHelper.getRateShopZoneKey(origin, destination);
        rateshopRedisCacheLoader.saveFedexRateshopZoneCacheById(rateShopZoneKey, fedexRateshopZoneCache);
        return fedexRateshopZoneCache;
    }

}
