package com.luluroute.ms.carrier.rateshop.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierHelper;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierInput;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierResponse;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierResponseMode;
import com.luluroute.ms.carrier.config.FeatureConfig;
import com.luluroute.ms.carrier.rateshop.entity.RateshopRateDb;
import com.luluroute.ms.carrier.rateshop.mapper.RateshopMapper;
import com.luluroute.ms.carrier.rateshop.model.RateShopCacheAndDb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateshopService {
	
	@Autowired
	private FeatureConfig featureConfig;

    private final RedisRehydrateRateshopZoneService redisRehydrateRateshopZoneService;
    private final RedisRehydrateRateshopRatesService redisRehydrateRateshopRatesService;

    public RateShopCarrierResponse getRateShopResponse(RateShopCarrierInput inputData) {
        log.debug("Attempting FedEx rateshop Zone cache lookup");
        
        if(featureConfig.isSfsEnabled && StringUtils.isNumeric(inputData.getOriginEntityCode())){
        	log.debug("Identified Ship From Store Fedex Shipment");
        	return RateShopCarrierResponse.builder()
                    .modes(convertToSetofResponseMode(inputData.getModeCodes()))                    
                    .build();	
        }
        
        RateShopCacheAndDb rateShopCacheAndDb =
                redisRehydrateRateshopZoneService.hydrateRateshopZoneCache(inputData);

        if(rateShopCacheAndDb.getRateshopRateDbs() == null) {
            log.debug("FedEx rateshop Zone cache hit. Attempting rateshop Rates cache lookup");
            rateShopCacheAndDb = redisRehydrateRateshopRatesService.hydrateRateshopRatesCache(
                    inputData, rateShopCacheAndDb);
        }

        Set<RateShopCarrierResponseMode> rateShopCarrierResponseModes;
        if(rateShopCacheAndDb.getRateshopRateDbs() == null) {
            log.info("FedEx rateshop Rates cache hit. Populating response from Zone and Rates caches");
            rateShopCarrierResponseModes = RateShopCarrierHelper.getRateShopResponseModesFromCache(
                    inputData, rateShopCacheAndDb.getRateShopCarrierRatesCache().getModes());
        }
        else {
            log.info("FedEx rateshop Zone or Rates cache miss. Populating rates from database");
            rateShopCarrierResponseModes = getRateShopResponseModesFromDb(
                    inputData, rateShopCacheAndDb.getRateshopRateDbs());
        }

        return RateShopCarrierResponse.builder()
                .modes(rateShopCarrierResponseModes)
                .build();
    }

    private static Set<RateShopCarrierResponseMode> getRateShopResponseModesFromDb(
            RateShopCarrierInput inputData, Set<RateshopRateDb> rateshopRateDbs) {
        if(CollectionUtils.isEmpty(rateshopRateDbs)) {
            RateShopCarrierHelper.logMissingRateshopData(inputData);
        }

        return rateshopRateDbs.stream()
                .map(RateshopMapper::getRateShopResponseMode)
                .filter(rateShopCarrierResponseMode ->
                        RateShopCarrierHelper.isIncludeRateShopResponseMode(inputData, rateShopCarrierResponseMode))
                .collect(Collectors.toSet());
    }
    
    public Set<RateShopCarrierResponseMode> convertToSetofResponseMode(final Set<String> inputModes) {
    	
        return inputModes.stream().map(mode -> {
            final RateShopCarrierResponseMode responseMode = new RateShopCarrierResponseMode();
            responseMode.setModeCode(mode);
            responseMode.setBaseRate(0.01);
            responseMode.setTotalCost(0.01);
            return responseMode;
        }).collect(Collectors.toSet());
    }

}
