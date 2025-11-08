package io.github.altkat.BuffedItems.Handlers;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks for updates using only the GitHub Releases API.
 * Handles all logic (fetching, comparing, logging) internally.
 */
public class UpdateChecker implements Listener {

    private final BuffedItems plugin;
    private final String githubRepo;
    private String latestVersion;
    private String latestDownloadUrl;
    private final String GITHUB_REPO_URL;
    private static final String SPIGOT_URL = "https://www.spigotmc.org/resources/129550/";

    private static final long CONSOLE_LOG_DELAY_TICKS = 30L;
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 3000;

    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/releases/latest";
    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\":\"([^\"]+)\"");
    private static final Pattern URL_PATTERN = Pattern.compile("\"browser_download_url\":\"([^\"]*?\\.jar)\"");

    /**
     * @param plugin The main plugin instance
     * @param githubRepo The GitHub repository path (e.g., "altkat/BuffedItems")
     */
    public UpdateChecker(BuffedItems plugin, String githubRepo) {
        this.plugin = plugin;
        this.githubRepo = githubRepo;
        this.GITHUB_REPO_URL = "https://github.com/" + githubRepo + "/releases/latest";
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Starts the asynchronous update check.
     * Logs the result directly to the console.
     */
    public void checkAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            HttpURLConnection connection = null;
            try {
                String apiUrl = String.format(GITHUB_API_URL, this.githubRepo);
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                        () -> "[UpdateChecker] Fetching from GitHub: " + apiUrl);

                connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("User-Agent", "BuffedItems-UpdateChecker (" + this.githubRepo + ")");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int statusCode = connection.getResponseCode();
                if (statusCode != 200) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                            () -> "[UpdateChecker] GitHub API returned status: " + statusCode);

                    if (statusCode == 404) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                                () -> "[UpdateChecker] Repository not found (404): " + this.githubRepo);
                    } else if (statusCode == 403) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                                () -> "[UpdateChecker] GitHub API rate limit exceeded (403)");
                    }
                    return;
                }

                String jsonResponse;
                try (InputStream inputStream = connection.getInputStream();
                     Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                    jsonResponse = scanner.useDelimiter("\\A").next();
                }

                Matcher tagMatcher = TAG_PATTERN.matcher(jsonResponse);
                if (tagMatcher.find()) {
                    this.latestVersion = tagMatcher.group(1);

                    Matcher urlMatcher = URL_PATTERN.matcher(jsonResponse);
                    if (urlMatcher.find()) {
                        this.latestDownloadUrl = urlMatcher.group(1);
                    }

                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                            () -> "[UpdateChecker] Latest GitHub release found: " + this.latestVersion);

                    final String currentVersion = plugin.getDescription().getVersion();
                    if (isNewerVersion(currentVersion, this.latestVersion)) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                ConfigManager.logInfo("&eA new update is available! Version: &a" + latestVersion);
                                ConfigManager.logInfo("&eDownload from GitHub: &a" + GITHUB_REPO_URL);
                                ConfigManager.logInfo("&eDownload from Spigot: &6" + SPIGOT_URL);
                            }
                        }.runTaskLater(plugin, CONSOLE_LOG_DELAY_TICKS);

                    } else {
                        ConfigManager.logInfo("&aYou are using the latest version. (&e" + currentVersion + "&a)");
                    }

                } else {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                            () -> "[UpdateChecker] Could not parse 'tag_name' from GitHub response.");
                }

            } catch (IOException exception) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () ->
                        "[UpdateChecker] Unable to check for updates: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
                ConfigManager.logInfo("&cUnable to check for updates on GitHub: &e" + exception.getMessage());
            } catch (Exception exception) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () ->
                        "[UpdateChecker] Unexpected error: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            } finally {
                if (connection != null) {
                    try {
                        connection.disconnect();
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                                () -> "[UpdateChecker] Connection closed successfully.");
                    } catch (Exception e) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () ->
                                "[UpdateChecker] Error disconnecting: " + e.getMessage());
                    }
                }
            }
        });
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

                    player.sendMessage(ConfigManager.fromLegacy("&#FF6347[BuffedItems] &eA new version is available on GitHub: &a" + latestVersion));
                    Component openGitHub = ConfigManager.fromLegacy("&aClick to open the GitHub releases page.");

                    Component downloadFrom = Component.text("Download from: ", NamedTextColor.YELLOW);
                    Component openBracket = Component.text("[", NamedTextColor.GRAY, TextDecoration.BOLD);
                    Component closeBracket = Component.text("]", NamedTextColor.GRAY, TextDecoration.BOLD);
                    Component separator = Component.text(" | ", NamedTextColor.GRAY);

                    Component githubLink = Component.text("GitHub Page", NamedTextColor.GREEN, TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.openUrl(GITHUB_REPO_URL))
                            .hoverEvent(HoverEvent.showText(openGitHub));

                    Component spigotLink = Component.text("Spigot", NamedTextColor.GOLD, TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.openUrl(SPIGOT_URL))
                            .hoverEvent(HoverEvent.showText(ConfigManager.fromLegacy("&6Click to open SpigotMC page.")));

                    Component fullMessage = downloadFrom
                            .append(openBracket).append(githubLink).append(closeBracket)
                            .append(separator)
                            .append(openBracket).append(spigotLink).append(closeBracket);

                    player.sendMessage(fullMessage);

                }, 40L);
            }
        }
    }

    /**
     * Compares two version strings (supports 1.10 > 1.9).
     * @param currentVersion The current plugin version
     * @param latestVersion The latest available version
     * @return true if latestVersion is newer than currentVersion
     */
    public static boolean isNewerVersion(String currentVersion, String latestVersion) {
        if (currentVersion == null || latestVersion == null) {
            return false;
        }

        String current = currentVersion.replaceAll("[vV]", "").split("-")[0].trim();
        String latest = latestVersion.replaceAll("[vV]", "").split("-")[0].trim();

        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            try {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (latestPart > currentPart) return true;
                if (latestPart < currentPart) return false;

            } catch (NumberFormatException e) {
                return !current.equalsIgnoreCase(latest);
            }
        }
        return false;
    }

    /**
     * Gets the cached latest version (for player join event).
     */
    public String getLatestVersionCached() {
        return latestVersion;
    }

    /**
     * Gets the cached download URL (for player join event).
     */
    public String getLatestDownloadUrl() {
        return latestDownloadUrl;
    }
}