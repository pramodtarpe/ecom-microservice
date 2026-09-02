package com.ecommerce.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    void preservesAndForwardsSafeClientCorrelationId() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products")
                .header(CorrelationIdGlobalFilter.HEADER_NAME, "client-order_42:retry-1"));
        var forwardedCorrelationId = new AtomicReference<String>();

        filter.filter(exchange, filteredExchange -> {
            forwardedCorrelationId.set(filteredExchange.getRequest().getHeaders()
                    .getFirst(CorrelationIdGlobalFilter.HEADER_NAME));
            return filteredExchange.getResponse().setComplete();
        }).block();

        assertThat(forwardedCorrelationId).hasValue("client-order_42:retry-1");
        assertThat(exchange.getResponse().getHeaders()
                .getFirst(CorrelationIdGlobalFilter.HEADER_NAME))
                .isEqualTo("client-order_42:retry-1");
        assertThat(exchange.<String>getAttribute(CorrelationIdGlobalFilter.EXCHANGE_ATTRIBUTE))
                .isEqualTo("client-order_42:retry-1");
    }

    @Test
    void replacesUnsafeClientCorrelationIdWithUuid() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders")
                .header(CorrelationIdGlobalFilter.HEADER_NAME, "unsafe correlation id"));
        var forwardedCorrelationId = new AtomicReference<String>();

        filter.filter(exchange, filteredExchange -> {
            forwardedCorrelationId.set(filteredExchange.getRequest().getHeaders()
                    .getFirst(CorrelationIdGlobalFilter.HEADER_NAME));
            return filteredExchange.getResponse().setComplete();
        }).block();

        assertThat(forwardedCorrelationId.get())
                .isNotBlank()
                .isNotEqualTo("unsafe correlation id")
                .hasSize(36);
        assertThatCode(() -> UUID.fromString(forwardedCorrelationId.get()))
                .doesNotThrowAnyException();
        assertThat(exchange.getResponse().getHeaders()
                .getFirst(CorrelationIdGlobalFilter.HEADER_NAME))
                .isEqualTo(forwardedCorrelationId.get());
    }
}
