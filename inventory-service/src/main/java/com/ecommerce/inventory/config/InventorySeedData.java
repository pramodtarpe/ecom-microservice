package com.ecommerce.inventory.config;

import com.ecommerce.inventory.domain.InventoryItem;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InventorySeedData {

    @Bean
    CommandLineRunner seedInventory(InventoryRepository inventoryRepository) {
        return args -> List.of(
                        InventoryItem.create("SKU-LAPTOP", 10),
                        InventoryItem.create("SKU-PHONE", 25),
                        InventoryItem.create("SKU-HEADPHONES", 50)
                ).stream()
                .filter(item -> !inventoryRepository.existsBySku(item.getSku()))
                .forEach(inventoryRepository::save);
    }
}
