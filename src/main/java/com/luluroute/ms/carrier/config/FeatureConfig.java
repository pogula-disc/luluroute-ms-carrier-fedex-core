package com.luluroute.ms.carrier.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeatureConfig {
    @Value("${config.feature.sfs-enabled}")
    public boolean isSfsEnabled;
}
