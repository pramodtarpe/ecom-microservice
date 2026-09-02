package com.ecommerce.events.inventory;

/** Versioned Kafka topic names for the inventory reservation workflow. */
public final class InventoryTopics {

    public static final String COMMANDS = "ecommerce.inventory.commands.v1";
    public static final String RESULTS = "ecommerce.inventory.results.v1";

    private InventoryTopics() {
    }
}
