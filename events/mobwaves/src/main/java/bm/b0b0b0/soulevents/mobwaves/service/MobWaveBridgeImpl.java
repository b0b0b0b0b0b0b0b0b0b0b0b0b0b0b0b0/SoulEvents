package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.api.mobwave.MobWaveAttachRequest;
import bm.b0b0b0.soulevents.api.mobwave.MobWaveBridge;
import bm.b0b0b0.soulevents.api.mobwave.MobWaveStatus;

import java.util.Optional;
import java.util.UUID;

public final class MobWaveBridgeImpl implements MobWaveBridge {

    private final MobWaveService service;

    public MobWaveBridgeImpl(MobWaveService service) {
        this.service = service;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void attach(MobWaveAttachRequest request) {
        service.attach(request);
    }

    @Override
    public void detach(UUID sessionId) {
        service.detach(sessionId);
    }

    @Override
    public boolean blocksChest(UUID sessionId) {
        return service.blocksChest(sessionId);
    }

    @Override
    public Optional<MobWaveStatus> status(UUID sessionId) {
        return service.status(sessionId);
    }
}
