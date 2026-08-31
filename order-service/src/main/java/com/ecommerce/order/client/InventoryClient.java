package com.ecommerce.order.client;

import com.ecommerce.order.error.DownstreamServiceException;
import com.ecommerce.order.error.InventoryReservationException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(@Qualifier("inventoryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "inventory")
    @Bulkhead(name = "inventory", type = Bulkhead.Type.SEMAPHORE)
    public void reserve(String sku, int quantity) {
        try {
            restClient.post()
                    .uri("/api/inventory/{sku}/reservations", sku)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new QuantityRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            throw new InventoryReservationException(sku, exception);
        } catch (RestClientException exception) {
            throw new DownstreamServiceException(
                    "Inventory is unavailable while reserving SKU " + sku, exception);
        }
    }

    @CircuitBreaker(name = "inventory")
    @Bulkhead(name = "inventory", type = Bulkhead.Type.SEMAPHORE)
    public void release(String sku, int quantity) {
        try {
            restClient.delete()
                    .uri("/api/inventory/{sku}/reservations?quantity={quantity}", sku, quantity)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new DownstreamServiceException(
                    "Inventory is unavailable while releasing SKU " + sku, exception);
        }
    }

    private record QuantityRequest(int quantity) {
    }
}
