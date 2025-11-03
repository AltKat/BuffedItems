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
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks for plugin updates from Modrinth API.
 * Modrinth provides a modern alternative to SpigotMC for plugin distribution.
 */
public class ModrinthUpdateChecker implements Listener {

    private final BuffedItems plugin;
    private final String modrinthProjectId;
    private String latestVersion;
    private String latestDownloadUrl;

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/%s/latest_version?loaders=[\"bukkit\",\"paper\",\"spigot\"]";

    /**
     * Creates a new ModrinthUpdateChecker instance.
     *
     * @param plugin The BuffedItems plugin instance
     * @param modrinthProjectId The Modrinth project slug (e.g., "buffeditems")
     */
    public ModrinthUpdateChecker(BuffedItems plugin, String modrinthProjectId) {
        this.plugin = plugin;
        this.modrinthProjectId = modrinthProjectId;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Fetches the latest version from Modrinth API asynchronously.
     *
     * @param consumer A callback function that receives the version string
     */
    public void getLatestVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            InputStream inputStream = null;
            Scanner scanner = null;
            try {
                String apiUrl = String.format(MODRINTH_API_URL, this.modrinthProjectId);
                URL url = new URL(apiUrl);
                URLConnection connection = url.openConnection();

                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("User-Agent", "BuffedItems-UpdateChecker/1.0");

                inputStream = connection.getInputStream();
                scanner = new Scanner(inputStream, StandardCharsets.UTF_8);
                StringBuilder response = new StringBuilder();

                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }

                String jsonResponse = response.toString();
                String version = parseVersionFromJson(jsonResponse);
                String downloadUrl = parseDownloadUrlFromJson(jsonResponse);

                if (version != null && !version.isEmpty()) {
                    this.latestVersion = version;
                    this.latestDownloadUrl = downloadUrl;
                    consumer.accept(version);
                } else {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                            () -> "[UpdateChecker] Could not parse version from Modrinth response");
                }

            } catch (IOException exception) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () ->
                        "[UpdateChecker] Unable to check for updates: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                ConfigManager.logInfo("&cUnable to check for updates on Modrinth: &e" + exception.getMessage());
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

    /**
     * Parses the version number from the Modrinth API JSON response.
     *
     * @param jsonResponse The JSON response from Modrinth API
     * @return The version string, or null if not found
     */
    private String parseVersionFromJson(String jsonResponse) {
        try {
            Pattern pattern = Pattern.compile("\"version_number\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[UpdateChecker] Error parsing version: " + e.getMessage());
        }
        return null;
    }

    /**
     * Parses the download URL from the Modrinth API JSON response.
     *
     * @param jsonResponse The JSON response from Modrinth API
     * @return The download URL, or null if not found
     */
    private String parseDownloadUrlFromJson(String jsonResponse) {
        try {
            Pattern pattern = Pattern.compile("\"url\":\"([^\"]*\\.jar)\"");
            Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[UpdateChecker] Error parsing download URL: " + e.getMessage());
        }
        return null;
    }

    /**
     * Sends an update notification to admin players when they join.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("buffeditems.admin") && latestVersion != null) {
            if (isNewerVersion(plugin.getDescription().getVersion(), latestVersion)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(ConfigManager.fromSection("&#FF6347[BuffedItems] A new version is available: &e" + latestVersion));
                    if (latestDownloadUrl != null) {
                        player.sendMessage(ConfigManager.fromSection("&#FF6347Download: &b" + latestDownloadUrl));
                    } else {
                        player.sendMessage(ConfigManager.fromSection("&#FF6347Download: &bhttps://modrinth.com/plugins/buffeditems"));
                    }
                }, 40L);
            }
        }
    }

    /**
     * Compares two version strings to determine if a new version is available.
     *
     * @param currentVersion The current plugin version
     * @param latestVersion The latest available version
     * @return true if latestVersion is newer than currentVersion
     */
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

    /**
     * Gets the latest version that was fetched.
     *
     * @return The latest version string, or null if not fetched yet
     */
    public String getLatestVersionCached() {
        return latestVersion;
    }

    /**
     * Gets the download URL for the latest version.
     *
     * @return The download URL, or null if not available
     */
    public String getLatestDownloadUrl() {
        return latestDownloadUrl;
    }
}