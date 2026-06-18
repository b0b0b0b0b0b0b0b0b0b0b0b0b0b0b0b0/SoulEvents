package bm.b0b0b0.soulevents.api.world;

public enum WorldPlacementDenial {
    NONE(""),
    INVALID_LOCATION("airdrop.placement.invalid-location"),
    WORLD_NOT_WHITELISTED("airdrop.placement.world-not-whitelisted"),
    WORLD_BLACKLISTED("airdrop.placement.world-blacklisted"),
    WORLD_GUARD_MISSING("airdrop.placement.worldguard-missing"),
    REGION_NOT_WHITELISTED("airdrop.placement.region-not-whitelisted"),
    REGION_BLACKLISTED("airdrop.placement.region-blacklisted"),
    REGION_INSIDE("airdrop.placement.region-inside"),
    REGION_TOO_CLOSE("airdrop.placement.region-too-close"),
    OUTSIDE_MAP_BOUNDARY("airdrop.placement.outside-map-boundary");

    private final String messageKey;

    WorldPlacementDenial(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
