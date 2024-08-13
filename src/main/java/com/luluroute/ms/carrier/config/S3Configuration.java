package com.luluroute.ms.carrier.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Getter
public class S3Configuration {

    @Value("${s3.accessKey}")
    private String awsAccessKey;

    @Value("${s3.secretKey}")
    private String awsSecretKey;

    @Value("${s3.region}")
    private String region;

    @Value("${s3.bucketName}")
    private String bucketName;


    @Bean
    public AmazonS3 amazonS3() {
        final BasicAWSCredentials basicAWSCredentials =
                new BasicAWSCredentials(awsAccessKey, awsSecretKey);

        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .withRegion(region)
                .build();
    }

}
