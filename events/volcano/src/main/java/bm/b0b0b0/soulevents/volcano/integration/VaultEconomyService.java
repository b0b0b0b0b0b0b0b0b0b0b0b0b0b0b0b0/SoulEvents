package bm.b0b0b0.soulevents.volcano.integration;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

public final class VaultEconomyService {

    private final JavaPlugin plugin;
    private Economy economy;
    private final DecimalFormat fallbackFormat;

    public VaultEconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        this.fallbackFormat = new DecimalFormat("#,##0.##", symbols);
    }

    public void hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return;
        }
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
    }

    public boolean available() {
        return economy != null;
    }

    public boolean isFree(double amount) {
        return amount <= 0.0;
    }

    public boolean has(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public double balance(Player player) {
        if (economy == null) {
            return 0.0;
        }
        return economy.getBalance(player);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null || amount <= 0.0) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null || amount <= 0.0) {
            return false;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (economy != null) {
            return economy.format(amount);
        }
        return fallbackFormat.format(amount);
    }

    public Optional<String> currencyName(double amount) {
        if (economy == null) {
            return Optional.empty();
        }
        if (Math.abs(amount - 1.0) < 0.001) {
            return Optional.ofNullable(economy.currencyNameSingular());
        }
        return Optional.ofNullable(economy.currencyNamePlural());
    }

    public void logMissingEconomy() {
        plugin.getLogger().warning("Vault economy not found — paid summon disabled.");
    }
}

