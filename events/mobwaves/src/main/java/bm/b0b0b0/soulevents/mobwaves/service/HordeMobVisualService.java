package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeMobCombatSettings;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntityTags;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class HordeMobVisualService {

    private static final String TEAM_NORMAL = "se-mw-normal";
    private static final String TEAM_BOSS = "se-mw-boss";

    private final Plugin plugin;
    private MobWavesPluginConfig config;
    private MobWaveService waveService;
    private BukkitTask particleTask;
    private final Set<UUID> outlined = new HashSet<>();

    public HordeMobVisualService(Plugin plugin, MobWavesPluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void bindWaveService(MobWaveService waveService) {
        this.waveService = waveService;
    }

    public void reload(MobWavesPluginConfig config) {
        this.config = config;
    }

    public void start() {
        if (particleTask != null) {
            return;
        }
        ensureTeams();
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::spawnBossParticles, 5L, 5L);
    }

    public void stop() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        for (UUID mobId : outlined.toArray(UUID[]::new)) {
            clear(mobId);
        }
        outlined.clear();
    }

    public void applyOutline(LivingEntity entity, boolean superBoss) {
        if (entity == null || entity.isDead()) {
            return;
        }
        HordeMobCombatSettings combat = config.module().hordeCombat;
        if (superBoss && !combat.bossOutlineEnabled) {
            return;
        }
        if (!superBoss && !combat.mobOutlineEnabled) {
            return;
        }
        ensureTeams();
        Team team = teamFor(superBoss);
        if (team == null) {
            return;
        }
        removeFromOutlineTeams(entity);
        entity.setGlowing(true);
        team.addEntity(entity);
        outlined.add(entity.getUniqueId());
    }

    public void clear(UUID mobId) {
        if (mobId == null) {
            return;
        }
        outlined.remove(mobId);
        Entity entity = Bukkit.getEntity(mobId);
        if (entity instanceof LivingEntity living) {
            living.setGlowing(false);
            removeFromOutlineTeams(living);
        }
    }

    private void spawnBossParticles() {
        if (waveService == null) {
            return;
        }
        HordeMobCombatSettings combat = config.module().hordeCombat;
        if (!combat.bossParticlesEnabled) {
            return;
        }
        waveService.forEachAliveMob((living, ignored) -> {
            if (!MobWaveEntityTags.isSuperBoss(plugin, living)) {
                return;
            }
            Location center = living.getLocation().add(0.0, living.getHeight() * 0.55, 0.0);
            if (center.getWorld() == null) {
                return;
            }
            int count = Math.max(4, combat.bossParticleCount);
            double spread = Math.max(0.4, combat.bossParticleSpread);
            center.getWorld().spawnParticle(
                    Particle.FLAME,
                    center,
                    count,
                    spread,
                    spread * 0.6,
                    spread,
                    0.02
            );
            center.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    center,
                    Math.max(2, count / 2),
                    spread * 0.8,
                    spread * 0.5,
                    spread * 0.8,
                    0.01
            );
            center.getWorld().spawnParticle(
                    Particle.DUST_COLOR_TRANSITION,
                    center,
                    Math.max(2, count / 3),
                    spread,
                    spread * 0.4,
                    spread,
                    0.0,
                    new Particle.DustTransition(Color.RED, Color.MAROON, 2.0f)
            );
        });
    }

    private Team teamFor(boolean superBoss) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        return board.getTeam(superBoss ? TEAM_BOSS : TEAM_NORMAL);
    }

    private void ensureTeams() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        registerTeam(board, TEAM_NORMAL, NamedTextColor.WHITE);
        registerTeam(board, TEAM_BOSS, NamedTextColor.RED);
    }

    private static void registerTeam(Scoreboard board, String name, NamedTextColor color) {
        Team team = board.getTeam(name);
        if (team == null) {
            team = board.registerNewTeam(name);
        }
        team.color(color);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    private void removeFromOutlineTeams(LivingEntity entity) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team normal = board.getTeam(TEAM_NORMAL);
        if (normal != null) {
            normal.removeEntity(entity);
        }
        Team boss = board.getTeam(TEAM_BOSS);
        if (boss != null) {
            boss.removeEntity(entity);
        }
    }
}
