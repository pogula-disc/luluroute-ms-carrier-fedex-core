package com.luluroute.ms.carrier.rateshop.service;

import com.logistics.luluroute.carrier.fedex.rateshop.entity.FedexRateshopZoneCache;
import com.logistics.luluroute.carrier.fedex.rateshop.service.FedexRateshopHelper;
import com.logistics.luluroute.carrier.fedex.rateshop.service.FedexRateshopRedisService;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierInput;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierRatesCache;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierRatesCacheMode;
import com.luluroute.ms.carrier.rateshop.entity.RateshopRateDb;
import com.luluroute.ms.carrier.rateshop.mapper.RateshopMapper;
import com.luluroute.ms.carrier.rateshop.model.RateShopCacheAndDb;
import com.luluroute.ms.carrier.rateshop.service.helper.RateshopHelper;
import com.luluroute.ms.carrier.rateshop.util.RateshopRedisCacheLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisRehydrateRateshopRatesService {

    private final RateshopHelper rateshopHelper;
    private final FedexRateshopRedisService fedexRateshopRedisService;
    private final RateshopRedisCacheLoader rateshopRedisCacheLoader;


    public RateShopCacheAndDb hydrateRateshopRatesCache(
            RateShopCarrierInput inputData, RateShopCacheAndDb rateShopCacheAndDb) {
        Set<RateshopRateDb> rateshopRateDbs = null;

        FedexRateshopZoneCache fedexRateshopZoneCache = rateShopCacheAndDb.getFedexRateshopZoneCache();
        RateShopCarrierRatesCache rateShopCarrierRatesCache = fedexRateshopRedisService.checkCacheForRateshopRates(
                inputData, fedexRateshopZoneCache);

        if(rateShopCarrierRatesCache == null) {
            rateshopRateDbs = rateshopHelper.populateRatesFromDb(inputData);
            rateShopCarrierRatesCache = cacheNewRateShopRates(
                    FedexRateshopHelper.getRateShopRatesKey(inputData, fedexRateshopZoneCache), rateshopRateDbs);
        }

        rateShopCacheAndDb.setRateshopRateDbs(rateshopRateDbs);
        rateShopCacheAndDb.setRateShopCarrierRatesCache(rateShopCarrierRatesCache);
        return rateShopCacheAndDb;
    }

    private RateShopCarrierRatesCache cacheNewRateShopRates(String rateshopRatesKey, Set<RateshopRateDb> rateshopRateDbs) {
        Set<RateShopCarrierRatesCacheMode> rateShopCarrierRatesCacheModes = rateshopRateDbs.stream()
                .map(RateshopMapper::getRateShopRatesCacheMode)
                .collect(Collectors.toSet());

        RateShopCarrierRatesCache rateShopCarrierRatesCache = RateShopCarrierRatesCache.builder()
                .modes(rateShopCarrierRatesCacheModes)
                .build();
        rateshopRedisCacheLoader.saveRateShopCarrierRatesCacheById(rateshopRatesKey, rateShopCarrierRatesCache);
        return rateShopCarrierRatesCache;
    }

}
