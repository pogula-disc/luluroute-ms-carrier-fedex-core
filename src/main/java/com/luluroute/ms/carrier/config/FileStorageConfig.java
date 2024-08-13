package com.luluroute.ms.carrier.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("fuse.fuse-file")
@Data
public class FileStorageConfig {
    String directory;
}
