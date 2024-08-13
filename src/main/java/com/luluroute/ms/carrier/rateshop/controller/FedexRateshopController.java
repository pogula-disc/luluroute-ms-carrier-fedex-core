package com.luluroute.ms.carrier.rateshop.controller;

import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierInput;
import com.logistics.luluroute.domain.rateshop.carrier.RateShopCarrierResponse;
import com.luluroute.ms.carrier.config.SwaggerConfig;
import com.luluroute.ms.carrier.rateshop.service.RateshopService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/api/carrier/fedex")
@Api(value = "/v1/api/carrier/fedex", tags = {SwaggerConfig.ENTITY_SVC_TAG})
@RequiredArgsConstructor
@Slf4j
public class FedexRateshopController {

    private final RateshopService rateshopService;

    @PostMapping(value = "/rateshop")
    public RateShopCarrierResponse calculateRates(@Valid @RequestBody RateShopCarrierInput inputData) {
        return rateshopService.getRateShopResponse(inputData);
    }

}
