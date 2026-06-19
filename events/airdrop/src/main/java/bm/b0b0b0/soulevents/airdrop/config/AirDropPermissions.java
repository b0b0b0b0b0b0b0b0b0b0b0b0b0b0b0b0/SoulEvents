package bm.b0b0b0.soulevents.airdrop.config;

public final class AirDropPermissions {

    public static final String STAFF = "soulevents.airdrop.staff";
    public static final String SUMMON = "soulevents.airdrop.summon";
    public static final String BYPASS = "soulevents.airdrop.bypass";

    private AirDropPermissions() {
    }

    public static String summonForType(String typeId) {
        return SUMMON + "." + typeId;
    }

    public static String openForType(String typeId) {
        return "soulevents.airdrop.open." + typeId;
    }
}
