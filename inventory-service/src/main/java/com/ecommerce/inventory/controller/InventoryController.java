package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.models.request.CreateInventoryRequest;
import com.ecommerce.inventory.models.request.InventoryStockUpdateRequest;
import com.ecommerce.inventory.models.request.ReservationRequest;
import com.ecommerce.inventory.models.response.InventoryResponse;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryResponse> findAll() {
        return inventoryService.findAll();
    }

    @GetMapping("/{sku}")
    public InventoryResponse findBySku(@PathVariable String sku) {
        return inventoryService.findBySku(sku);
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody CreateInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.create(request));
    }

    @PutMapping("/{sku}")
    public InventoryResponse replaceAvailableQuantity(
            @PathVariable String sku,
            @Valid @RequestBody InventoryStockUpdateRequest request
    ) {
        return inventoryService.replaceAvailableQuantity(sku, request.availableQuantity());
    }

    @PostMapping("/{sku}/reservations")
    public InventoryResponse reserve(
            @PathVariable String sku,
            @Valid @RequestBody ReservationRequest request
    ) {
        return inventoryService.reserve(sku, request.quantity());
    }

    @DeleteMapping("/{sku}/reservations")
    public InventoryResponse release(@PathVariable String sku, @RequestParam long quantity) {
        return inventoryService.release(sku, quantity);
    }
}
