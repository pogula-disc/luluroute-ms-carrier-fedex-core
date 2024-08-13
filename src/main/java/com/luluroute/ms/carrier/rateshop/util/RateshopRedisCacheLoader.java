package com.luluroute.ms.carrier.rateshop.util;

import com.logistics.luluroute.carrier.fedex.rateshop.entity.FedexRateshopZoneCache;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierRatesCache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import static com.logistics.luluroute.util.ConstantUtil.RATE_BASE_KEY;
import static com.logistics.luluroute.util.ConstantUtil.ZONE_BASE_KEY;

@Component
public class RateshopRedisCacheLoader {

    @Cacheable(value = ZONE_BASE_KEY, key = "#fedexRateshopZoneCacheKey")
    public FedexRateshopZoneCache saveFedexRateshopZoneCacheById(
            String fedexRateshopZoneCacheKey, FedexRateshopZoneCache fedexRateshopZoneCache) {
        return fedexRateshopZoneCache;
    }

    @Cacheable(value = RATE_BASE_KEY, key = "#rateshopCarrierRatesKey")
    public RateShopCarrierRatesCache saveRateShopCarrierRatesCacheById(
            String rateshopCarrierRatesKey, RateShopCarrierRatesCache rateShopCarrierRatesCache) {
        return rateShopCarrierRatesCache;
    }

}
