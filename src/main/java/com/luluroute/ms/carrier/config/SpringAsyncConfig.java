package com.luluroute.ms.carrier.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

@Configuration
@EnableAsync
public class SpringAsyncConfig {

    @Value("${config.async.cancelshipment.corepoolsize}")
    private int cancelCorePoolSize;

    @Value("${config.async.cancelshipment.maxpoolsize}")
    private int cancelMaxPoolSize;

    @Value("${config.async.createshipment.corepoolsize}")
    private int createCorePoolSize;

    @Value("${config.async.createshipment.maxpoolsize}")
    private int createMaxPoolSize;

    @Bean(name = "CreateShipmentTaskExecutor")
    public Executor createShipmentTaskExecutor() {
        return new ThreadPoolExecutor(createCorePoolSize, createMaxPoolSize, 30, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());

    }

    @Bean(name = "CancelShipmentTaskExecutor")
    public Executor cancelShipmentTaskExecutor() {
        return new ThreadPoolExecutor(cancelCorePoolSize, cancelMaxPoolSize, 30, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());

    }

    @Bean(name = "DBTaskExecutor")
    public Executor dbTaskExecutor() {
        return new ThreadPoolExecutor(cancelCorePoolSize, cancelMaxPoolSize, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.DiscardOldestPolicy());

    }
}
