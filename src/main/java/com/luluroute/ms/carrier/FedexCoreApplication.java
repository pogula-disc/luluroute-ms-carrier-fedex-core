package com.luluroute.ms.carrier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.logistics.luluroute.validator, com.luluroute.ms.carrier")
public class FedexCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(FedexCoreApplication.class, args);
	}

}
