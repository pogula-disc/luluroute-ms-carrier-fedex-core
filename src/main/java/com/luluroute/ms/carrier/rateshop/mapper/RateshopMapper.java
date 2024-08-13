package com.luluroute.ms.carrier.rateshop.mapper;

import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierRatesCacheMode;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierResponseMode;
import com.luluroute.ms.carrier.rateshop.entity.RateshopRateDb;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RateshopMapper {
    public static RateShopCarrierResponseMode getRateShopResponseMode(RateshopRateDb rateshopRateDb) {
        return RateShopCarrierResponseMode.builder()
                .modeCode(rateshopRateDb.getModeCode())
                .baseRate(rateshopRateDb.getBaseRate())
                // Future may include adding additional costs
                .totalCost(rateshopRateDb.getBaseRate())
                .build();
    }

    public static RateShopCarrierRatesCacheMode getRateShopRatesCacheMode(RateshopRateDb rateshopRateDb) {
        return RateShopCarrierRatesCacheMode.builder()
                .modeCode(rateshopRateDb.getModeCode())
                .baseRate(rateshopRateDb.getBaseRate())
                .build();
    }
}
