package com.luluroute.ms.carrier.config;


import com.luluroute.ms.carrier.model.FedExServiceInfo;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("fedex.mode-config")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FedExModeConfig {
    List<FedExServiceInfo> modes;
}
