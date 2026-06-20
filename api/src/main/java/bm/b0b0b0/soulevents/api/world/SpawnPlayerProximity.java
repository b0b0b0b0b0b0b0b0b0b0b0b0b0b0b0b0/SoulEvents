package bm.b0b0b0.soulevents.api.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class SpawnPlayerProximity {

    private SpawnPlayerProximity() {
    }

    public static boolean isTooClose(Location location, int minBlocks) {
        if (minBlocks <= 0) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        long minSquared = (long) minBlocks * minBlocks;
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        for (Player player : world.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            Location playerLocation = player.getLocation();
            if (!world.equals(playerLocation.getWorld())) {
                continue;
            }
            double dx = playerLocation.getX() - x;
            double dy = playerLocation.getY() - y;
            double dz = playerLocation.getZ() - z;
            if ((dx * dx) + (dy * dy) + (dz * dz) < minSquared) {
                return true;
            }
        }
        return false;
    }

    public static int nearestDistanceBlocks(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return Integer.MAX_VALUE;
        }
        double nearestSquared = Double.MAX_VALUE;
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        for (Player player : world.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            Location playerLocation = player.getLocation();
            if (!world.equals(playerLocation.getWorld())) {
                continue;
            }
            double dx = playerLocation.getX() - x;
            double dy = playerLocation.getY() - y;
            double dz = playerLocation.getZ() - z;
            nearestSquared = Math.min(nearestSquared, (dx * dx) + (dy * dy) + (dz * dz));
        }
        if (nearestSquared == Double.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.floor(Math.sqrt(nearestSquared));
    }
}
