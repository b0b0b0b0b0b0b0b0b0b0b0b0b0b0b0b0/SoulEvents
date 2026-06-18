package bm.b0b0b0.soulevents.api.protection;

public record GateResult(
        boolean allowed,
        String messageKey
) {

    public static GateResult allow() {
        return new GateResult(true, "");
    }

    public static GateResult deny(String messageKey) {
        return new GateResult(false, messageKey);
    }
}
