package systems.mythical.mythicskywars.utilities;

import systems.mythical.mythicskywars.MythicSkywars;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

/**
 * Checks for plugin updates via the GitHub Releases API.
 * Supports both stable releases and nightly/pre-release builds.
 * Can auto-download new versions for next server restart.
 */
public class UpdateChecker {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/%s/releases";
    private static final String USER_AGENT = "MythicSkywars-UpdateChecker/%s";

    private final MythicSkywars plugin;
    private final String repoOwner;
    private final String repoName;
    private final String currentVersion;
    private final boolean checkBeta;
    private final boolean autoUpdate;

    private String latestVersion = null;
    private String downloadUrl = null;
    private String releaseUrl = null;
    private boolean updateAvailable = false;
    private boolean updateDownloaded = false;
    private BukkitTask checkTask = null;

    public UpdateChecker(MythicSkywars plugin, String repoOwner, String repoName, boolean checkBeta, boolean autoUpdate) {
        this.plugin = plugin;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.currentVersion = plugin.getDescription().getVersion();
        this.checkBeta = checkBeta;
        this.autoUpdate = autoUpdate;
    }

    /**
     * Starts the periodic update check. Runs async every 4 hours.
     */
    public void start() {
        // Check 5 seconds after startup, then every 4 hours
        checkTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::check, 20L * 5, 20L * 60 * 60 * 4);
    }

    /**
     * Stops the periodic update check.
     */
    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    /**
     * Performs an immediate update check. Can be called from commands.
     */
    public void checkNow() {
        check();
    }

    /**
     * Downloads the update immediately. Returns true if successful.
     * Can be called from commands.
     */
    public boolean downloadUpdateNow() {
        if (downloadUrl == null) return false;
        if (updateDownloaded) return true;
        downloadUpdate(downloadUrl, latestVersion);
        return updateDownloaded;
    }

    /**
     * Performs the update check against GitHub Releases API.
     */
    private void check() {
        try {
            String endpoint = checkBeta
                    ? String.format(GITHUB_API_URL, repoOwner, repoName)
                    : String.format(GITHUB_API_URL + "/latest", repoOwner, repoName);

            URL url = new URL(endpoint);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", String.format(USER_AGENT, currentVersion));
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                if (MythicSkywars.getCfg().debugEnabled()) {
                    plugin.getLogger().warning("Update check failed with HTTP " + responseCode);
                }
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();

            String json = response.toString();

            // Parse release info — find the newest version from all releases
            String tagName = null;
            String htmlUrl = null;
            String jarDownloadUrl = null;

            if (checkBeta) {
                // Array response — find the release with the highest version
                String bestVersion = null;
                String bestHtml = null;
                int searchFrom = 0;
                while (true) {
                    String tag = extractJsonStringFrom(json, "tag_name", searchFrom);
                    if (tag == null) break;
                    int tagPos = json.indexOf("\"tag_name\":\"" + tag + "\"", searchFrom);

                    // Determine the version: use tag_name, or extract from "name" field if tag is generic
                    String version = tag.startsWith("v") ? tag.substring(1) : tag;
                    if ("nightly".equalsIgnoreCase(tag) || !version.contains(".")) {
                        // Tag is generic (e.g., "nightly") — extract version from release name
                        // Name format: "MythicSkywars Nightly #17 (5.6.40-nightly.17+e0bb71c)"
                        String releaseName = extractJsonStringFrom(json, "name", Math.max(0, tagPos - 100));
                        if (releaseName != null) {
                            int parenStart = releaseName.indexOf('(');
                            int parenEnd = releaseName.indexOf(')', parenStart);
                            if (parenStart >= 0 && parenEnd > parenStart) {
                                version = releaseName.substring(parenStart + 1, parenEnd);
                            }
                        }
                    }

                    if (bestVersion == null || isNewerVersion(version, bestVersion)) {
                        bestVersion = version;
                        bestHtml = extractJsonStringFrom(json, "html_url", Math.max(0, tagPos - 200));
                    }
                    searchFrom = tagPos + 1;
                    if (searchFrom <= 0) break;
                }
                if (bestVersion != null) {
                    tagName = bestVersion;
                    htmlUrl = bestHtml;
                }
            } else {
                // Single release response
                tagName = extractJsonString(json, "tag_name");
                htmlUrl = extractJsonString(json, "html_url");
            }

            if (tagName == null) {
                if (MythicSkywars.getCfg().debugEnabled()) {
                    plugin.getLogger().warning("Update check: could not parse tag_name from response");
                }
                return;
            }

            // Strip 'v' prefix if present
            String remoteVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;

            // Find the jar download URL from assets
            jarDownloadUrl = extractJarAssetUrl(json);

            // Compare versions
            if (isNewerVersion(remoteVersion, currentVersion)) {
                this.latestVersion = remoteVersion;
                this.releaseUrl = htmlUrl;
                this.downloadUrl = jarDownloadUrl;
                this.updateAvailable = true;

                plugin.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                plugin.getLogger().info("A new version is available: v" + latestVersion);
                plugin.getLogger().info("Current version: v" + currentVersion);
                plugin.getLogger().info("Download: " + (releaseUrl != null ? releaseUrl : "N/A"));
                plugin.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                // Auto-update: download the new jar
                if (autoUpdate && jarDownloadUrl != null && !updateDownloaded) {
                    downloadUpdate(jarDownloadUrl, remoteVersion);
                }
            } else {
                this.updateAvailable = false;
                if (MythicSkywars.getCfg().debugEnabled()) {
                    plugin.getLogger().info("Update check: running latest version (v" + currentVersion + ")");
                }
            }

        } catch (IOException e) {
            if (MythicSkywars.getCfg().debugEnabled()) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates", e);
            }
        }
    }

    /**
     * Downloads the updated jar to the plugins/update folder.
     * Spigot/Paper will automatically apply updates from this folder on restart.
     */
    private void downloadUpdate(String jarUrl, String version) {
        try {
            plugin.getLogger().info("Auto-update: Downloading v" + version + "...");

            // Use the plugins/update/ folder — Spigot/Paper picks this up on restart
            File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
            if (!updateFolder.exists() && !updateFolder.mkdirs()) {
                plugin.getLogger().warning("Auto-update: Failed to create update folder");
                return;
            }

            // The file must be named exactly like the plugin jar
            File pluginFile = getPluginFile();
            if (pluginFile == null) {
                plugin.getLogger().warning("Auto-update: Could not determine plugin jar name");
                return;
            }

            File updateFile = new File(updateFolder, pluginFile.getName());

            // Follow redirects (GitHub asset URLs redirect)
            URL url = new URL(jarUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", String.format(USER_AGENT, currentVersion));
            connection.setRequestProperty("Accept", "application/octet-stream");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();

            // Handle redirect manually if needed
            if (responseCode == 302 || responseCode == 301) {
                String redirectUrl = connection.getHeaderField("Location");
                connection.disconnect();
                url = new URL(redirectUrl);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", String.format(USER_AGENT, currentVersion));
                connection.setRequestProperty("Accept", "application/octet-stream");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                responseCode = connection.getResponseCode();
            }

            if (responseCode != 200) {
                plugin.getLogger().warning("Auto-update: Download failed with HTTP " + responseCode);
                connection.disconnect();
                return;
            }

            // Download to temp file first, then move atomically
            File tempFile = new File(updateFolder, pluginFile.getName() + ".tmp");
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                out.flush();

                if (totalBytes < 1024) {
                    // Suspiciously small — probably an error page
                    plugin.getLogger().warning("Auto-update: Downloaded file too small (" + totalBytes + " bytes), aborting");
                    tempFile.delete();
                    return;
                }
            }
            connection.disconnect();

            // Move temp to final location
            Files.move(tempFile.toPath(), updateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            updateDownloaded = true;
            plugin.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            plugin.getLogger().info("Auto-update: v" + version + " downloaded successfully!");
            plugin.getLogger().info("The update will be applied on the next server restart.");
            plugin.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Auto-update: Failed to download update", e);
        }
    }

    /**
     * Gets the plugin's jar file.
     */
    private File getPluginFile() {
        try {
            java.lang.reflect.Method getFileMethod = org.bukkit.plugin.java.JavaPlugin.class.getDeclaredMethod("getFile");
            getFileMethod.setAccessible(true);
            return (File) getFileMethod.invoke(plugin);
        } catch (Exception e) {
            // Fallback: search plugins folder for our jar
            File pluginsFolder = plugin.getDataFolder().getParentFile();
            File[] files = pluginsFolder.listFiles((dir, name) ->
                    name.toLowerCase().startsWith("mythicskywars-") && name.toLowerCase().endsWith(".jar"));
            if (files != null && files.length > 0) {
                return files[0];
            }
            return null;
        }
    }

    /**
     * Notify an admin/op player about available updates on join.
     */
    public void notifyPlayer(Player player) {
        if (!updateAvailable) return;
        if (!player.isOp() && !player.hasPermission("sw.admin")) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("");
            player.sendMessage(ChatColor.LIGHT_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + " MythicSkywars Update");
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + " New version: " + ChatColor.AQUA + "v" + latestVersion);
            player.sendMessage(ChatColor.GREEN + " Current:     " + ChatColor.GRAY + "v" + currentVersion);
            player.sendMessage("");

            if (updateDownloaded) {
                player.sendMessage(ChatColor.YELLOW + " ✓ Update downloaded! Restart to apply.");
            } else if (autoUpdate && downloadUrl != null) {
                player.sendMessage(ChatColor.YELLOW + " ⟳ Downloading update...");
            } else {
                player.sendMessage(ChatColor.GRAY + " " + (releaseUrl != null ? releaseUrl : "Check GitHub for download"));
            }

            player.sendMessage(ChatColor.LIGHT_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");
        }, 60L); // Delay so it doesn't get lost in join spam
    }

    /**
     * Extracts the first .jar asset browser_download_url from the GitHub API response.
     */
    private String extractJarAssetUrl(String json) {
        // Look for browser_download_url that ends with .jar
        String searchKey = "\"browser_download_url\":\"";
        int searchFrom = 0;
        while (true) {
            int start = json.indexOf(searchKey, searchFrom);
            if (start == -1) return null;
            start += searchKey.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return null;
            String url = json.substring(start, end);
            if (url.endsWith(".jar")) {
                return url;
            }
            searchFrom = end;
        }
    }

    /**
     * Simple version comparison. Supports formats like:
     * - 5.6.40
     * - 5.6.40-nightly.42+abc1234
     * Compares the numeric base version parts, then considers nightly as older than stable.
     */
    private boolean isNewerVersion(String remote, String current) {
        try {
            // Strip anything after a dash (pre-release suffix) for base comparison
            String remoteBase = remote.contains("-") ? remote.substring(0, remote.indexOf('-')) : remote;
            String currentBase = current.contains("-") ? current.substring(0, current.indexOf('-')) : current;

            String[] remoteParts = remoteBase.split("\\.");
            String[] currentParts = currentBase.split("\\.");

            int length = Math.max(remoteParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                int r = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
                int c = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                if (r > c) return true;
                if (r < c) return false;
            }

            // Base versions are equal
            // If we're on a nightly and remote is stable release, that's newer
            if (current.contains("-nightly") && !remote.contains("-")) {
                return true;
            }

            // If both are nightlies, compare the build number
            if (remote.contains("-nightly.") && current.contains("-nightly.")) {
                int remoteBuild = extractNightlyBuild(remote);
                int currentBuild = extractNightlyBuild(current);
                return remoteBuild > currentBuild;
            }

            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Extracts the build number from a nightly version string like "5.6.40-nightly.42+abc1234"
     */
    private int extractNightlyBuild(String version) {
        try {
            int nightlyIdx = version.indexOf("-nightly.");
            if (nightlyIdx == -1) return 0;
            String afterNightly = version.substring(nightlyIdx + "-nightly.".length());
            // Strip the +sha suffix if present
            if (afterNightly.contains("+")) {
                afterNightly = afterNightly.substring(0, afterNightly.indexOf('+'));
            }
            return Integer.parseInt(afterNightly);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Minimal JSON string value extractor. Avoids adding a JSON library dependency.
     * Finds the first occurrence of "key":"value" in the JSON string.
     */
    private String extractJsonString(String json, String key) {
        return extractJsonStringFrom(json, key, 0);
    }

    private String extractJsonStringFrom(String json, String key, int fromIndex) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search, fromIndex);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    // Getters

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public boolean isUpdateDownloaded() {
        return updateDownloaded;
    }

    /**
     * Clears the download state so a forced re-check can re-download if needed.
     */
    public void clearDownloadState() {
        updateDownloaded = false;
        updateAvailable = false;
        latestVersion = null;
        downloadUrl = null;
        releaseUrl = null;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getReleaseUrl() {
        return releaseUrl;
    }
}
