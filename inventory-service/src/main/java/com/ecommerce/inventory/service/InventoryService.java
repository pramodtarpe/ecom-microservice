package com.ecommerce.inventory.service;

import com.ecommerce.inventory.api.CreateInventoryRequest;
import com.ecommerce.inventory.api.InventoryResponse;
import com.ecommerce.inventory.domain.InventoryItem;
import com.ecommerce.inventory.exception.DuplicateSkuException;
import com.ecommerce.inventory.exception.InsufficientReservedQuantityException;
import com.ecommerce.inventory.exception.InsufficientStockException;
import com.ecommerce.inventory.exception.InvalidInventoryRequestException;
import com.ecommerce.inventory.exception.InventoryNotFoundException;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> findAll() {
        return inventoryRepository.findAll(Sort.by(Sort.Direction.ASC, "sku"))
                .stream()
                .map(InventoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse findBySku(String rawSku) {
        String sku = SkuNormalizer.normalize(rawSku);
        return InventoryResponse.from(inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new InventoryNotFoundException(sku)));
    }

    @Transactional
    public InventoryResponse create(CreateInventoryRequest request) {
        String sku = SkuNormalizer.normalize(request.sku());
        if (inventoryRepository.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }

        try {
            InventoryItem saved = inventoryRepository.saveAndFlush(
                    InventoryItem.create(sku, request.availableQuantity())
            );
            return InventoryResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSkuException(sku);
        }
    }

    @Transactional
    public InventoryResponse replaceAvailableQuantity(String rawSku, long availableQuantity) {
        if (availableQuantity < 0) {
            throw new InvalidInventoryRequestException("availableQuantity must be zero or greater");
        }

        InventoryItem item = findForUpdate(rawSku);
        item.replaceAvailableQuantity(availableQuantity);
        return InventoryResponse.from(inventoryRepository.saveAndFlush(item));
    }

    @Transactional
    public InventoryResponse reserve(String rawSku, long quantity) {
        requirePositiveQuantity(quantity);
        InventoryItem item = findForUpdate(rawSku);
        if (item.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(item.getSku(), quantity, item.getAvailableQuantity());
        }

        try {
            item.reserve(quantity);
        } catch (ArithmeticException exception) {
            throw new InvalidInventoryRequestException("reservation would exceed the supported quantity range");
        }
        return InventoryResponse.from(inventoryRepository.saveAndFlush(item));
    }

    @Transactional
    public InventoryResponse release(String rawSku, long quantity) {
        requirePositiveQuantity(quantity);
        InventoryItem item = findForUpdate(rawSku);
        if (item.getReservedQuantity() < quantity) {
            throw new InsufficientReservedQuantityException(item.getSku(), quantity, item.getReservedQuantity());
        }

        try {
            item.release(quantity);
        } catch (ArithmeticException exception) {
            throw new InvalidInventoryRequestException("release would exceed the supported quantity range");
        }
        return InventoryResponse.from(inventoryRepository.saveAndFlush(item));
    }

    private InventoryItem findForUpdate(String rawSku) {
        String sku = SkuNormalizer.normalize(rawSku);
        return inventoryRepository.findBySkuForUpdate(sku)
                .orElseThrow(() -> new InventoryNotFoundException(sku));
    }

    private static void requirePositiveQuantity(long quantity) {
        if (quantity <= 0) {
            throw new InvalidInventoryRequestException("quantity must be greater than zero");
        }
    }
}
