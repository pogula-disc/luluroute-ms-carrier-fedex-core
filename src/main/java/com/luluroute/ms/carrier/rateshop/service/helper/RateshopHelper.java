package com.luluroute.ms.carrier.rateshop.service.helper;

import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierInput;
import com.luluroute.ms.carrier.rateshop.entity.RateshopRateDb;
import com.luluroute.ms.carrier.rateshop.repository.RateshopRateDbRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.logistics.luluroute.carrier.fedex.rateshop.service.FedexRateshopHelper.isDestinationRequiresZip;
import static com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierHelper.getRateShopCacheWeight;

@Component
@RequiredArgsConstructor
public class RateshopHelper {

    private final RateshopRateDbRepository rateshopRateDbRepository;


    public Set<RateshopRateDb> populateRatesFromDb(RateShopCarrierInput inputData) {
        return rateshopRateDbRepository.findByWeightOriginDestination(
                getRateShopCacheWeight(inputData.getWeight()),
                inputData.isMilitary(),
                isDestinationRequiresZip(inputData),
                inputData.getOriginZipCode(),
                inputData.getDestinationZipCode(),
                inputData.getOriginCountry(),
                inputData.getDestinationCountry());
    }

}
