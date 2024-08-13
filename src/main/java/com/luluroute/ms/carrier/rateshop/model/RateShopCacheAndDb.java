package com.luluroute.ms.carrier.rateshop.model;

import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierRatesCache;
import com.logistics.luluroute.carrier.fedex.rateshop.entity.FedexRateshopZoneCache;
import com.luluroute.ms.carrier.rateshop.entity.RateshopRateDb;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Builder
@Data
public class RateShopCacheAndDb {

    private FedexRateshopZoneCache fedexRateshopZoneCache;
    private RateShopCarrierRatesCache rateShopCarrierRatesCache;
    private Set<RateshopRateDb> rateshopRateDbs;
}
