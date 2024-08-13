package com.luluroute.ms.carrier.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

	@Value("${config.metrics.prometheus.consumer.records.success}")
	private String noOfSuccessRecords;

	@Value("${config.metrics.prometheus.last.gasp}")
	private String lastGaspMetricsName;

	@Value("${config.metrics.prometheus.dlq}")
	private String dlqMetricsName;

	@Value("${config.metrics.prometheus.elapsedtime}")
	private String elapsedTimeMetricsName;

	@Bean
	public Counter successCounter(MeterRegistry metric) {
		return metric.counter(noOfSuccessRecords);
	}

	@Bean(name = "retryMetrics")
	public Counter retryCounter(MeterRegistry metric) {
		return metric.counter(lastGaspMetricsName);
	}

	@Bean(name = "errorMetrics")
	public Counter dlqCounter(MeterRegistry metric) {
		return metric.counter(dlqMetricsName);
	}


	@Bean(name = "elapsedTimeMetrics")
	public Timer elapsedTimeMetrics(MeterRegistry metric) {
		return metric.timer(elapsedTimeMetricsName);
	}
}
