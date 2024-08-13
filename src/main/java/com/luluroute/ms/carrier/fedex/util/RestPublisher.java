package com.luluroute.ms.carrier.fedex.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.NoHttpResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

@Service
@Slf4j
public class RestPublisher<V> {

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    RestTemplate restTemplate;

    @Retryable(include = {HttpServerErrorException.class,
            UnknownHttpStatusCodeException.class,
            NoHttpResponseException.class},
            maxAttemptsExpression = "${restTemplate.maxAttempts:3}",
            recover = "handlePerformRestCallException")
    public V performRestCall(String url, String code, Class<V> responseType) {
        String msg = "RestPublisher.performRestCall()";

        log.info(String.format(Constants.STANDARD_INFO, msg, url, code));
        HttpHeaders headers = prepareHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<V> response =
                restTemplate.exchange(url + code, HttpMethod.GET, requestEntity, responseType);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error(String.format(Constants.STANDARD_INFO, msg, url, response));
        }

        return response.getBody();

    }

    @Recover
    public Object handlePerformRestCallException(Exception exception, String url, String code) throws Exception {
        String msg = "RestPublisher.performRestCall()";
        log.error(Constants.STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(exception));
        throw exception;
    }

    public HttpHeaders prepareHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
