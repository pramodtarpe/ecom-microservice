package com.ecommerce.productservice.service;

import com.ecommerce.productservice.domain.Product;
import com.ecommerce.productservice.exception.DuplicateProductSkuException;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.models.request.ProductUpsertRequest;
import com.ecommerce.productservice.models.response.ProductResponse;
import com.ecommerce.productservice.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "id");

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll(DEFAULT_SORT).stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(requireById(id));
    }

    public ProductResponse findBySku(String sku) {
        String normalizedSku = Product.normalizeSku(sku);
        return productRepository.findBySku(normalizedSku)
                .map(ProductResponse::from)
                .orElseThrow(() -> ProductNotFoundException.forSku(normalizedSku));
    }

    @Transactional
    public ProductResponse create(ProductUpsertRequest request) {
        String normalizedSku = Product.normalizeSku(request.sku());
        ensureSkuAvailable(normalizedSku, null);

        Product product = new Product(
                normalizedSku,
                request.name(),
                request.description(),
                request.price(),
                request.active()
        );
        return ProductResponse.from(saveAndFlush(product, normalizedSku));
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpsertRequest request) {
        Product product = requireById(id);
        String normalizedSku = Product.normalizeSku(request.sku());
        ensureSkuAvailable(normalizedSku, id);

        product.update(
                normalizedSku,
                request.name(),
                request.description(),
                request.price(),
                request.active()
        );
        return ProductResponse.from(saveAndFlush(product, normalizedSku));
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(requireById(id));
    }

    private Product requireById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ProductNotFoundException.forId(id));
    }

    private void ensureSkuAvailable(String sku, Long currentProductId) {
        boolean alreadyExists = currentProductId == null
                ? productRepository.existsBySku(sku)
                : productRepository.existsBySkuAndIdNot(sku, currentProductId);
        if (alreadyExists) {
            throw new DuplicateProductSkuException(sku);
        }
    }

    private Product saveAndFlush(Product product, String sku) {
        try {
            return productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            // The database constraint remains authoritative if concurrent requests race.
            throw new DuplicateProductSkuException(sku);
        }
    }
}
