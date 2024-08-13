package com.luluroute.ms.carrier.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Component
@ConfigurationProperties("fedex.ursa-config")
@Data
@Validated
public class UrsaConfig {
    Map<String, String> formIds;
}
