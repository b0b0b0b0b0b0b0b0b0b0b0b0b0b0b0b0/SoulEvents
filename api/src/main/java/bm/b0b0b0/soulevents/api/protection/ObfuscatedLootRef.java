package bm.b0b0b0.soulevents.api.protection;

import java.util.UUID;

public record ObfuscatedLootRef(UUID sessionId, int slotIndex) {
}
