package com.luluroute.ms.carrier.filter;

import com.luluroute.ms.carrier.fedex.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static com.luluroute.ms.carrier.fedex.util.Constants.*;

@Slf4j
@RequiredArgsConstructor
public class RequestLogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        if (httpServletRequest.getRequestURI().contains("actuator")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        executeRequestLogFilter(httpServletRequest, httpServletResponse, filterChain);
    }

    private void executeRequestLogFilter(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            FilterChain filterChain) throws IOException, ServletException {

        Instant startTime = Instant.now();
        ContentCachingRequestWrapper contentCachingRequestWrapper = new ContentCachingRequestWrapper(httpServletRequest);

        try {
            addIdsToMDC(httpServletRequest);
            filterChain.doFilter(contentCachingRequestWrapper, httpServletResponse);
        } finally {
            logResolution(startTime, contentCachingRequestWrapper);
            removeIdsFromMDC();
        }
    }

    private void addIdsToMDC(HttpServletRequest httpServletRequest) {
        MDC.put(X_SHIPMENT_CORRELATION_ID, httpServletRequest.getHeader(Constants.X_CORRELATION_ID));
        MDC.put(X_CARRIER_STREAM_FUNCTION_ID, httpServletRequest.getHeader(Constants.X_CARRIER_STREAM_FUNCTION_ID));
        String messageCorrelationId = httpServletRequest.getHeader(Constants.X_MESSAGE_CORRELATION_ID);
        MDC.put(X_MESSAGE_CORRELATION_ID, StringUtils.isBlank(messageCorrelationId) ? getCorrelationId() : messageCorrelationId);
    }

    private void removeIdsFromMDC() {
        MDC.remove(X_SHIPMENT_CORRELATION_ID);
        MDC.remove(X_CARRIER_STREAM_FUNCTION_ID);
        MDC.remove(X_MESSAGE_CORRELATION_ID);
    }

    private void logResolution(Instant startTime, ContentCachingRequestWrapper contentCachingRequestWrapper) {
        Instant endTime = Instant.now();
        log.info("Processed request: {} >> ::Time taken to process message in milliseconds: {} ",
                new String(contentCachingRequestWrapper.getContentAsByteArray()),
                Duration.between(startTime, endTime).toMillis());
    }
}
