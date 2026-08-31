package com.ecommerce.productservice.config;

import com.ecommerce.productservice.domain.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProductDataSeeder implements ApplicationRunner {

    private final ProductRepository productRepository;

    public ProductDataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Product> samples = List.of(
                new Product("SKU-LAPTOP", "Pro 14 Laptop", "14-inch developer laptop", new BigDecimal("1299.00"), true),
                new Product("SKU-PHONE", "128 GB Smartphone", "Unlocked smartphone in black", new BigDecimal("699.00"), true),
                new Product("SKU-HEADPHONES", "Noise-cancelling Headphones", "Wireless over-ear headphones", new BigDecimal("249.00"), true)
        );

        samples.stream()
                .filter(product -> !productRepository.existsBySku(product.getSku()))
                .forEach(productRepository::save);
    }
}
