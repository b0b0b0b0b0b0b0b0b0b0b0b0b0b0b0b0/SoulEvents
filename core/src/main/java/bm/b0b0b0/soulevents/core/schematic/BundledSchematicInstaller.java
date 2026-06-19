package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.message.CoreConsoleLog;
import bm.b0b0b0.soulevents.core.message.YamlMessageService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

final class BundledSchematicInstaller {

    private static final List<String> BUNDLED_SCHEMATICS = List.of(
            "schematics/test_arena.schem"
    );

    private BundledSchematicInstaller() {
    }

    static void installMissing(JavaPlugin plugin, YamlMessageService messages, Path schematicsRoot) {
        for (String resourcePath : BUNDLED_SCHEMATICS) {
            installIfMissing(plugin, messages, schematicsRoot, resourcePath);
        }
    }

    private static void installIfMissing(
            JavaPlugin plugin,
            YamlMessageService messages,
            Path schematicsRoot,
            String resourcePath
    ) {
        String fileName = Path.of(resourcePath).getFileName().toString();
        Path target = schematicsRoot.resolve(fileName);
        if (Files.exists(target)) {
            return;
        }
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                plugin.getLogger().warning("Bundled schematic missing in JAR: " + resourcePath);
                return;
            }
            Files.copy(stream, target);
            CoreConsoleLog.line(plugin, messages, "startup.schematic.bundled", Map.of("file", fileName));
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "Failed to install bundled schematic " + fileName,
                    exception
            );
        }
    }
}
