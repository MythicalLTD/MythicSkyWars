package com.walrusone.skywarsreloaded.nms;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NMSUtils {

    public static NMS loadNMS(SkyWarsReloaded plugin) {
        String packageName = plugin.getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);

        // On 26.1+ (and Paper 1.20.5+), CraftBukkit no longer has versioned packages.
        // The package is just "org.bukkit.craftbukkit" so version will be "craftbukkit".
        // In that case, fall back to parsing getBukkitVersion().
        if (version.equals("craftbukkit")) {
            version = resolveVersionFromBukkitVersion();
        }

        CompatibleNMSVersion selectedNMSVersion = null;
        for (CompatibleNMSVersion key : CompatibleNMSVersion.values()) {
            if (key.name().equals(version)) {
                selectedNMSVersion = key;
                break;
            }
        }

        plugin.getLogger().info("Trying to load NMS support for server version '" + version + " (" + packageName + ")'...");

        if (selectedNMSVersion == null) {
            Integer currentFeatureVersion = resolveFeatureVersion(version);

            selectedNMSVersion = CompatibleNMSVersion.getLatestSupported(currentFeatureVersion);
            int latestSupportedFeatureVersion = selectedNMSVersion.getFeatureVersion();

            if (currentFeatureVersion != null && currentFeatureVersion >= latestSupportedFeatureVersion) {
                plugin.getLogger().warning(
                        "===================\n" +
                                "It seems like we have not validated this newer version of Spigot/Bukkit (" + version + ").\n" +
                                "We will try to load the plugin with the latest supported version: " + selectedNMSVersion.name() + ", with handler: " + selectedNMSVersion.getNmsImplVersion() + ".\n" +
                                "Some features may not work as intended. However, this should be relatively stable on 1.19+\n" +
                                "If you experience any issues, please join our Discord server (https://www.gcnt.net/discord) and wait for an official release with proper support.\n" +
                                "===================");
            }
        }

        try {
            String nmsImplVersionStr = selectedNMSVersion.getNmsImplVersion();
            final Class<?> clazz = Class.forName("com.walrusone.skywarsreloaded.nms." + nmsImplVersionStr + ".NMSHandler");
            if (NMS.class.isAssignableFrom(clazz)) {
                plugin.getLogger().info("Loaded support for NMS server version " + version + " using handler: " + nmsImplVersionStr + ".");
                return (NMS) clazz.getConstructor().newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String versionLabel = version.startsWith("v") ? version : ("v" + version);
        plugin.getLogger().severe(
                "===================\n" +
                        "It seems like we do not support this version of Spigot/Bukkit (" + versionLabel + ").\n" +
                        "If you are on a supported version but see ClassNotFoundException for NMSHandler, deploy the shaded jar built from module skywars-assembly (SWR-*.jar under the repo target folder), not the SkyWarsReloadedCore jar alone.\n" +
                        "If you feel like this is a mistake, please join our Discord server (https://www.gcnt.net/discord).\n" +
                        "===================");
        return null;
    }

    /**
     * Resolves a version string suitable for matching against CompatibleNMSVersion enum
     * from Bukkit.getBukkitVersion(). Handles both old format (1.20.4-R0.1-SNAPSHOT)
     * and new format (26.1.2-R0.1-SNAPSHOT).
     */
    private static String resolveVersionFromBukkitVersion() {
        String bukkitVersion = Bukkit.getBukkitVersion(); // e.g. "26.1.2-R0.1-SNAPSHOT" or "1.21.1-R0.1-SNAPSHOT"
        String mcVersion = bukkitVersion.split("-")[0]; // e.g. "26.1.2" or "1.21.1"

        String[] parts = mcVersion.split("\\.");

        // New format: YY.D.H (e.g. 26.1.2) — major version is the year number (26)
        // Old format: 1.X.Y (e.g. 1.21.1) — starts with "1"
        if (parts.length >= 2 && !parts[0].equals("1")) {
            // New versioning (26.1, 26.1.2, etc.)
            // Map to enum name: v26_1_R1
            return "v" + parts[0] + "_" + parts[1] + "_R1";
        } else if (parts.length >= 2 && parts[0].equals("1")) {
            // Old versioning (1.21.1, 1.20.4, etc.)
            // Try to match known NMS revision patterns
            // We'll construct something like v1_21_R1 and let the fallback handle sub-revisions
            return "v1_" + parts[1] + "_R1";
        }

        return mcVersion;
    }

    /**
     * Resolves the feature version number from a version string.
     * Handles formats like "v1_21_R1" (returns 21), "v26_1_R1" (returns 26),
     * or raw version strings like "26.1.2" (returns 26).
     */
    private static Integer resolveFeatureVersion(String version) {
        // Try the old format: v1_XX_RY — extract XX
        try {
            String[] underscoreParts = version.split("_");
            if (underscoreParts.length >= 2) {
                // Could be "v1" + "21" + "R1" or "v26" + "1" + "R1"
                String firstPart = underscoreParts[0].replace("v", "");
                int first = Integer.parseInt(firstPart);
                if (first == 1) {
                    // Old format: v1_XX_RY, feature version is XX
                    return Integer.parseInt(underscoreParts[1]);
                } else {
                    // New format: v26_1_R1, feature version is 26
                    return first;
                }
            }
        } catch (Exception ignored) {
        }

        // Try raw version format: "26.1.2" or "1.21.1"
        try {
            String regex = "(\\d+)\\.(\\d+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(version);
            if (matcher.find()) {
                int major = Integer.parseInt(matcher.group(1));
                if (major == 1) {
                    // Old format, return minor as feature version
                    return Integer.parseInt(matcher.group(2));
                } else {
                    // New format, return major as feature version
                    return major;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

}
