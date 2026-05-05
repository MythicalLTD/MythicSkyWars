package systems.mythical.mythicskywars.utilities;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BuiltInVaultEconomy extends AbstractEconomy {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "MythicSkywars-BUILTIN";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return String.format(java.util.Locale.US, "%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return "Coins";
    }

    @Override
    public String currencyNameSingular() {
        return "Coin";
    }

    @Override
    public boolean hasAccount(String playerName) {
        return resolveOffline(playerName) != null;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null && player.getUniqueId() != null;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer offline = resolveOffline(playerName);
        return offline == null ? 0D : VaultUtils.get().getBalance(offline);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return VaultUtils.get().getBalance(player);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        OfflinePlayer offline = resolveOffline(playerName);
        if (offline == null) {
            return response(0D, 0D, EconomyResponse.ResponseType.FAILURE, "Unknown player");
        }
        return withdrawPlayer(offline, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null || player.getUniqueId() == null) {
            return response(0D, 0D, EconomyResponse.ResponseType.FAILURE, "Unknown player");
        }
        double current = VaultUtils.get().getBalance(player);
        if (current < amount) {
            return response(0D, current, EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
        }
        boolean ok = VaultUtils.get().setBalance(player.getUniqueId(), player.getName(), current - Math.max(0D, amount));
        double now = VaultUtils.get().getBalance(player);
        return response(ok ? amount : 0D, now, ok ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, ok ? null : "Failed to persist");
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        OfflinePlayer offline = resolveOffline(playerName);
        if (offline == null) {
            return response(0D, 0D, EconomyResponse.ResponseType.FAILURE, "Unknown player");
        }
        return depositPlayer(offline, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null || player.getUniqueId() == null) {
            return response(0D, 0D, EconomyResponse.ResponseType.FAILURE, "Unknown player");
        }
        double current = VaultUtils.get().getBalance(player);
        boolean ok = VaultUtils.get().setBalance(player.getUniqueId(), player.getName(), current + Math.max(0D, amount));
        double now = VaultUtils.get().getBalance(player);
        return response(ok ? amount : 0D, now, ok ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, ok ? null : "Failed to persist");
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return response(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
    }

    @Override
    public List<String> getBanks() {
        return new ArrayList<>();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        OfflinePlayer offline = resolveOffline(playerName);
        if (offline == null || offline.getUniqueId() == null) return false;
        return VaultUtils.get().setBalance(offline.getUniqueId(), offline.getName(), VaultUtils.get().getBalance(offline));
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) return false;
        return VaultUtils.get().setBalance(player.getUniqueId(), player.getName(), VaultUtils.get().getBalance(player));
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    private OfflinePlayer resolveOffline(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return null;
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(playerName));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private EconomyResponse response(double amount, double balance, EconomyResponse.ResponseType type, String error) {
        return new EconomyResponse(amount, balance, type, error);
    }
}
