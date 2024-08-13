package com.luluroute.ms.carrier.config;

import com.luluroute.ms.carrier.filter.RequestLogFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
public class RequestLogFilterConfig {
    private static final int ORDER_REQUEST_LOG_FILTER = HIGHEST_PRECEDENCE;

    @Bean
    public FilterRegistrationBean<RequestLogFilter> filterRegistration() {
        final var registration = new FilterRegistrationBean<RequestLogFilter>();
        final var filter = new RequestLogFilter();
        registration.setFilter(filter);
        registration.setName("LOGGING_FILTER");
        registration.setOrder(ORDER_REQUEST_LOG_FILTER);
        return registration;
    }
}