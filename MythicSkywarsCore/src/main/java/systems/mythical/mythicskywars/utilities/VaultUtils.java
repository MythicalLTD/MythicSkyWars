package systems.mythical.mythicskywars.utilities;

import systems.mythical.mythicskywars.MythicSkywars;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import systems.mythical.mythicskywars.database.Database;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

public class VaultUtils {
    private static VaultUtils instance;
    private Economy econ = null;
    private Chat chat = null;

    private VaultUtils() {
        if ((!setupEconomy()) && (!setupChat())) {
            MythicSkywars.get().getLogger().info("ERROR: Vault Dependency was not found. Install Vault or turn off Economy in the Config!");
        }
    }

    public static VaultUtils get() {
        if (instance == null) {
            instance = new VaultUtils();
        }
        return instance;
    }

    private boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
        chat = rsp.getProvider();
        return chat != null;
    }


    public boolean canBuy(Player player, double cost) {
        if (player == null) return false;
        if (useBuiltInEconomy()) {
            return getBuiltInBalance(player.getUniqueId().toString(), player.getName()) >= cost;
        }
        return (econ != null) && (econ.getBalance(player) >= cost);
    }

    public boolean payCost(Player player, double cost) {
        if (player == null) return false;
        if (useBuiltInEconomy()) {
            double current = getBuiltInBalance(player.getUniqueId().toString(), player.getName());
            if (current < cost) return false;
            return setBuiltInBalance(player.getUniqueId().toString(), player.getName(), current - cost);
        }
        if (econ == null) return false;
        try {
            EconomyResponse rp = econ.withdrawPlayer(player, cost);
            return rp.transactionSuccess();
        } catch (Exception e) {
            this.handleException(e);
        }
        return false;
    }

    public double getBalance(Player player) {
        if (player == null) return 0.0D;
        if (useBuiltInEconomy()) {
            return getBuiltInBalance(player.getUniqueId().toString(), player.getName());
        }
        if (econ == null) return 0.0D;
        try {
            return econ.getBalance(player);
        } catch (Exception e) {
            this.handleException(e);
        }
        return 0.0D;
    }

    public double getBalance(OfflinePlayer player) {
        if (player == null) return 0.0D;
        if (useBuiltInEconomy()) {
            if (player.getUniqueId() == null) return 0.0D;
            return getBuiltInBalance(player.getUniqueId().toString(), player.getName());
        }
        if (econ == null) return 0.0D;
        try {
            Double byOffline = invokeDouble("getBalance", new Class<?>[]{OfflinePlayer.class}, new Object[]{player});
            if (byOffline != null) {
                return byOffline;
            }
            if (player.getName() != null && !player.getName().trim().isEmpty()) {
                Double byName = invokeDouble("getBalance", new Class<?>[]{String.class}, new Object[]{player.getName()});
                if (byName != null) {
                    return byName;
                }
            }
        } catch (Exception e) {
            this.handleException(e);
        }
        return 0.0D;
    }

    public void give(Player win, int i) {
        if (win == null) return;
        if (useBuiltInEconomy()) {
            final String uuid = win.getUniqueId().toString();
            final String name = win.getName();
            Bukkit.getScheduler().runTaskAsynchronously(MythicSkywars.get(), () -> {
                addBuiltInBalance(uuid, name, i);
            });
            return;
        }
        if (econ == null) return;
        try {
            econ.depositPlayer(win, i);
        } catch (Exception e) {
            this.handleException(e);
        }
    }

    public Chat getChat() {
        return chat;
    }

    public boolean isEconomyAvailable() {
        if (useBuiltInEconomy()) {
            return true;
        }
        return econ != null;
    }

    public boolean setBalance(UUID uuid, String playerName, double amount) {
        if (useBuiltInEconomy()) {
            if (uuid == null) return false;
            return setBuiltInBalance(uuid.toString(), playerName, amount);
        }
        if (econ == null) return false;
        amount = Math.max(0D, amount);
        try {
            double current = getBalance(uuid, playerName);
            double delta = amount - current;
            if (Math.abs(delta) < 0.000001D) {
                return true;
            }
            if (delta > 0D) {
                return deposit(uuid, playerName, delta);
            }
            return withdraw(uuid, playerName, -delta);
        } catch (Exception e) {
            this.handleException(e);
            return false;
        }
    }

    private boolean useBuiltInEconomy() {
        String provider = MythicSkywars.getCfg() == null ? "ESSENTIALSX" : MythicSkywars.getCfg().economyProvider();
        return "BUILTIN".equalsIgnoreCase(provider == null ? "" : provider.trim().toUpperCase(Locale.ROOT));
    }

    private double getBuiltInBalance(String uuid, String playerName) {
        Database db = MythicSkywars.getDb();
        if (db != null) {
            return db.getStoredEconomy(uuid, playerName);
        }
        return getYamlEconomy(uuid);
    }

    private boolean setBuiltInBalance(String uuid, String playerName, double amount) {
        Database db = MythicSkywars.getDb();
        if (db != null) {
            return db.setStoredEconomy(uuid, playerName, amount);
        }
        return setYamlEconomy(uuid, amount);
    }

    private boolean addBuiltInBalance(String uuid, String playerName, double delta) {
        double current = getBuiltInBalance(uuid, playerName);
        return setBuiltInBalance(uuid, playerName, current + delta);
    }

    private double getYamlEconomy(String uuid) {
        File f = getPlayerDataFile(uuid);
        if (f == null || !f.exists()) return 0D;
        FileConfiguration fc = YamlConfiguration.loadConfiguration(f);
        return Math.max(0D, fc.getDouble("economy", 0D));
    }

    private boolean setYamlEconomy(String uuid, double amount) {
        File f = getPlayerDataFile(uuid);
        if (f == null) return false;
        File parent = f.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return false;
        }
        FileConfiguration fc = YamlConfiguration.loadConfiguration(f);
        fc.set("economy", Math.max(0D, amount));
        try {
            fc.save(f);
            return true;
        } catch (IOException e) {
            handleException(e);
            return false;
        }
    }

    private File getPlayerDataFile(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return null;
        File playerDataDir = new File(MythicSkywars.get().getDataFolder(), "player_data");
        return new File(playerDataDir, uuid + ".yml");
    }

    // PRIVATE UTILS

    private void handleException(Exception e) {
        if (MythicSkywars.getCfg().debugEnabled()) e.printStackTrace();
        else MythicSkywars.get().getLogger().severe(
                "An exception was thrown while attempting to deposit eco: " + e.getMessage() +
                        ". Please enable debugMode in the config file before reporting this issue!");
    }

    private double getBalance(UUID uuid, String playerName) throws Exception {
        OfflinePlayer offline = uuid == null ? null : Bukkit.getOfflinePlayer(uuid);
        Double byOffline = invokeDouble("getBalance", new Class<?>[]{OfflinePlayer.class}, new Object[]{offline});
        if (byOffline != null) {
            return byOffline;
        }
        if (playerName != null && !playerName.trim().isEmpty()) {
            Double byName = invokeDouble("getBalance", new Class<?>[]{String.class}, new Object[]{playerName});
            if (byName != null) {
                return byName;
            }
        }
        return 0D;
    }

    private boolean deposit(UUID uuid, String playerName, double amount) throws Exception {
        OfflinePlayer offline = uuid == null ? null : Bukkit.getOfflinePlayer(uuid);
        Boolean byOffline = invokeTransaction("depositPlayer", new Class<?>[]{OfflinePlayer.class, double.class}, new Object[]{offline, amount});
        if (byOffline != null) {
            return byOffline;
        }
        if (playerName != null && !playerName.trim().isEmpty()) {
            Boolean byName = invokeTransaction("depositPlayer", new Class<?>[]{String.class, double.class}, new Object[]{playerName, amount});
            if (byName != null) {
                return byName;
            }
        }
        return false;
    }

    private boolean withdraw(UUID uuid, String playerName, double amount) throws Exception {
        OfflinePlayer offline = uuid == null ? null : Bukkit.getOfflinePlayer(uuid);
        Boolean byOffline = invokeTransaction("withdrawPlayer", new Class<?>[]{OfflinePlayer.class, double.class}, new Object[]{offline, amount});
        if (byOffline != null) {
            return byOffline;
        }
        if (playerName != null && !playerName.trim().isEmpty()) {
            Boolean byName = invokeTransaction("withdrawPlayer", new Class<?>[]{String.class, double.class}, new Object[]{playerName, amount});
            if (byName != null) {
                return byName;
            }
        }
        return false;
    }

    private Double invokeDouble(String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Method method = econ.getClass().getMethod(methodName, paramTypes);
            Object result = method.invoke(econ, args);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Boolean invokeTransaction(String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Method method = econ.getClass().getMethod(methodName, paramTypes);
            Object result = method.invoke(econ, args);
            if (result instanceof EconomyResponse) {
                return ((EconomyResponse) result).transactionSuccess();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

}
