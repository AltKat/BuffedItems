package io.github.altkat.BuffedItems.Handlers;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker implements Listener {

    private final BuffedItems plugin;
    private final int resourceId;
    private String latestVersion;

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    public UpdateChecker(BuffedItems plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            InputStream inputStream = null;
            Scanner scanner = null;
            try {
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId);
                URLConnection connection = url.openConnection();

                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);

                inputStream = connection.getInputStream();
                scanner = new Scanner(inputStream);

                if (scanner.hasNext()) {
                    String version = scanner.next();
                    this.latestVersion = version;
                    consumer.accept(version);
                }
            } catch (IOException exception) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () ->
                        "[UpdateChecker] Unable to check for updates: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                ConfigManager.logInfo("&cUnable to check for updates: &e" + exception.getMessage());
            } finally {
                if (scanner != null) {
                    scanner.close();
                } else if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () ->
                                "[UpdateChecker] Error closing input stream: " + e.getMessage());
                    }
                }
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("BuffedItems.admin")) {
            if (latestVersion != null && isNewerVersion(plugin.getDescription().getVersion(), latestVersion)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(ConfigManager.fromSection("§eA new version of BuffedItems is available! (" + latestVersion + ")"));
                    player.sendMessage(ConfigManager.fromSection("§eDownload it from: §b" + "https://www.spigotmc.org/resources/buffeditems." + resourceId + "/"));
                }, 40L);
            }
        }
    }

    public static boolean isNewerVersion(String currentVersion, String latestVersion) {
        String current = currentVersion.replaceAll("[vV]", "");
        String latest = latestVersion.replaceAll("[vV]", "");

        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            try {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (latestPart > currentPart) {
                    return true;
                }
                if (latestPart < currentPart) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return !currentVersion.equalsIgnoreCase(latestVersion);
            }
        }
        return false;
    }
}
