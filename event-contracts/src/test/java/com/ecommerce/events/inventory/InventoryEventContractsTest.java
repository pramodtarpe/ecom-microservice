package com.ecommerce.events.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryEventContractsTest {

    @Test
    void commandDefensivelyCopiesItsItems() {
        var source = new ArrayList<>(List.of(new InventoryLine("SKU-ONE", 2)));
        var event = command(InventoryCommandType.RESERVE, source);

        source.clear();

        assertEquals(List.of(new InventoryLine("SKU-ONE", 2)), event.items());
        assertThrows(
                UnsupportedOperationException.class,
                () -> event.items().add(new InventoryLine("SKU-TWO", 1)));
    }

    @Test
    void reserveRequiresAtLeastOneItem() {
        assertThrows(
                IllegalArgumentException.class,
                () -> command(InventoryCommandType.RESERVE, List.of()));
    }

    @Test
    void releaseAllowsAnEmptyItemList() {
        var event = command(InventoryCommandType.RELEASE, List.of());

        assertEquals(List.of(), event.items());
    }

    @Test
    void rejectedResultRequiresAReason() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryResultEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        InventoryResultEvent.CURRENT_SCHEMA_VERSION,
                        InventoryResultType.REJECTED,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        "contract-test",
                        42,
                        1,
                        List.of(new InventoryLine("SKU-ONE", 2)),
                        null,
                        null));
    }

    @Test
    void successfulResultRejectsFailureDetails() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryResultEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        InventoryResultEvent.CURRENT_SCHEMA_VERSION,
                        InventoryResultType.RESERVED,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        "contract-test",
                        42,
                        1,
                        List.of(new InventoryLine("SKU-ONE", 2)),
                        "NOT_USED",
                        "Not used"));
    }

    @Test
    void contractOnlyAcceptsSchemaVersionOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryCommandEvent(
                        UUID.randomUUID(),
                        2,
                        InventoryCommandType.RESERVE,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        "contract-test",
                        42,
                        1,
                        List.of(new InventoryLine("SKU-ONE", 2))));
    }

    private static InventoryCommandEvent command(
            InventoryCommandType type, List<InventoryLine> items) {
        return new InventoryCommandEvent(
                UUID.randomUUID(),
                InventoryCommandEvent.CURRENT_SCHEMA_VERSION,
                type,
                Instant.parse("2026-01-01T00:00:00Z"),
                "contract-test",
                42,
                1,
                items);
    }
}
