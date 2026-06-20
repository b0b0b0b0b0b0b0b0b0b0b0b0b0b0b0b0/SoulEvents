package bm.b0b0b0.soulevents.api.mobwave;

import java.util.Optional;
import java.util.UUID;

public interface MobWaveBridge {

    boolean isEnabled();

    void attach(MobWaveAttachRequest request);

    void detach(UUID sessionId);

    boolean blocksChest(UUID sessionId);

    Optional<MobWaveStatus> status(UUID sessionId);
}
