package bm.b0b0b0.soulevents.api.protection;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface ArenaGuardService {

    boolean canModifyBlock(UUID sessionId, Block block, Player actor);

    boolean canPlaceFluid(UUID sessionId, Location location, Player actor);

    void protect(UUID sessionId, Location center, int radius);

    void unprotect(UUID sessionId);

    void reload();
}
