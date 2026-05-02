package systems.mythical.mythicskywars.utilities;

import systems.mythical.mythicskywars.MythicSkywars;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
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
        return (econ != null) && (econ.getBalance(player) >= cost);
    }

    public boolean payCost(Player player, double cost) {
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
        if (econ == null) return 0.0D;
        try {
            return econ.getBalance(player);
        } catch (Exception e) {
            this.handleException(e);
        }
        return 0.0D;
    }

    public void give(Player win, int i) {
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
        return econ != null;
    }

    public boolean setBalance(UUID uuid, String playerName, double amount) {
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
