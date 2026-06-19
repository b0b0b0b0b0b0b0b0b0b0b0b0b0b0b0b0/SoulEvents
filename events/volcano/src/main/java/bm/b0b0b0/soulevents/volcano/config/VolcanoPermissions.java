package bm.b0b0b0.soulevents.volcano.config;

public final class VolcanoPermissions {

    public static final String STAFF = "soulevents.volcano.staff";
    public static final String SUMMON = "soulevents.volcano.summon";
    public static final String BYPASS = "soulevents.volcano.bypass";

    private VolcanoPermissions() {
    }

    public static String summonForType(String typeId) {
        return SUMMON + "." + typeId;
    }
}
