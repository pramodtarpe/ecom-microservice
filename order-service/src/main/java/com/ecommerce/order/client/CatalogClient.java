package com.ecommerce.order.client;

import com.ecommerce.order.error.DownstreamServiceException;
import com.ecommerce.order.error.ProductUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Qualifier("catalogRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(name = "catalog")
    @CircuitBreaker(name = "catalog")
    @Bulkhead(name = "catalog", type = Bulkhead.Type.SEMAPHORE)
    public BigDecimal priceForSku(String sku) {
        try {
            var product = restClient.get()
                    .uri("/api/products/sku/{sku}", sku)
                    .retrieve()
                    .body(CatalogProductResponse.class);

            if (product == null || !Boolean.TRUE.equals(product.active())) {
                throw new ProductUnavailableException(sku);
            }
            if (product.price() == null || product.price().signum() < 0) {
                throw new DownstreamServiceException(
                        "Catalog returned an invalid price for SKU " + sku);
            }
            return product.price();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new ProductUnavailableException(sku);
            }
            throw new DownstreamServiceException(
                    "Catalog rejected the lookup for SKU " + sku, exception);
        } catch (RestClientException exception) {
            throw new DownstreamServiceException(
                    "Catalog is unavailable while looking up SKU " + sku, exception);
        }
    }

    private record CatalogProductResponse(String sku, BigDecimal price, Boolean active) {
    }
}
