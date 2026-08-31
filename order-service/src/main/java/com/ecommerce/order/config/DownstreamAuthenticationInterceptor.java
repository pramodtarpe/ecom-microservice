package com.ecommerce.order.config;

import com.ecommerce.platform.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Adds a short-lived machine credential and tracing context to downstream requests. */
@Component
public class DownstreamAuthenticationInterceptor implements ClientHttpRequestInterceptor {

    private final ServiceTokenProvider serviceTokenProvider;

    public DownstreamAuthenticationInterceptor(ServiceTokenProvider serviceTokenProvider) {
        this.serviceTokenProvider = serviceTokenProvider;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("An authenticated user is required for downstream calls");
        }

        // Never forward the caller's bearer token. Downstream services receive only a
        // narrowly scoped, short-lived machine token with the caller recorded for audit.
        request.getHeaders().setBearerAuth(
                serviceTokenProvider.createToken(jwtAuthentication.getToken().getSubject()));

        String correlationId = currentCorrelationId();
        if (correlationId != null) {
            request.getHeaders().set(CorrelationIdFilter.HEADER_NAME, correlationId);
        }
        return execution.execute(request, body);
    }

    private static String currentCorrelationId() {
        var requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest servletRequest = servletRequestAttributes.getRequest();
            String correlationId = CorrelationIdFilter.currentCorrelationId(servletRequest);
            if (correlationId != null) {
                return correlationId;
            }
        }
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
