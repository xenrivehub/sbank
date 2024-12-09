package com.spearforge.sBank;

import com.spearforge.sBank.commands.BankCommands;
import com.spearforge.sBank.config.CustomFileConfiguration;
import com.spearforge.sBank.database.DatabaseConnection;
import com.spearforge.sBank.database.MySQLConnection;
import com.spearforge.sBank.database.SQLiteConnection;
import com.spearforge.sBank.listener.*;
import com.spearforge.sBank.model.Bank;
import com.spearforge.sBank.model.Debt;
import com.spearforge.sBank.modules.DebtModule;
import com.spearforge.sBank.modules.InterestModule;
import com.spearforge.sBank.utils.MiscUtils;
import com.spearforge.sBank.utils.TextUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class SBank extends JavaPlugin {

    @Getter
    private static Map<String, Debt> debts = new HashMap<>();
    @Getter
    private static Map<String, Bank> banks = new HashMap<>();
    @Getter
    private static Economy econ = null;
    @Getter
    private static DatabaseConnection db;
    @Getter
    private static SBank plugin;
    @Getter
    private static CustomFileConfiguration guiConfig;


    @SneakyThrows
    @Override
    public void onEnable() {

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        plugin = this;
        saveDefaultConfig();
        guiConfig = new CustomFileConfiguration(this, "gui.yml");
        guiConfig.createConfig();
        initializeDatabaseConnection();
        // can throw SQLException
        db.loadDebtsFromDatabase();
        db.loadOnlinePlayersBanks();
        autoSaveDatabase();

        if (getConfig().getBoolean("interest.enabled")){
            startInterestScheduler();
            getLogger().info("Interest module is active and scheduler is started.");
        }

        if (getConfig().getBoolean("loan.enabled")){
            startDebtCheckScheduler();
            getLogger().info("Loan module is active and debt scheduler is started.");
        }

        setListeners();
        setCommands();

    }

    @Override
    public void onDisable() {
        try {
            saveAllBanks();
            saveAllDebts();
            db.getConnection().close();
        } catch (SQLException e) {
            getLogger().warning("An error occurred while closing the database connection or saving all banks!");
            getLogger().warning(e.getMessage());
        }
    }

    private void initializeDatabaseConnection() {
        String dbType = getConfig().getString("database.type");

        if ("mysql".equalsIgnoreCase(dbType)) {
            db = new MySQLConnection();
        } else if ("sqlite".equalsIgnoreCase(dbType)) {
            db = new SQLiteConnection();
        } else {
            getLogger().severe("Unsupported database type: " + dbType);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            db.initializeDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Could not initialize database.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public void autoSaveDatabase() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveAllBanks();
                saveAllDebts();


                if(getConfig().getBoolean("auto-save.log-settings.enabled", true)) {
                    String msg = getConfig().getString("auto-save.log-settings.message", "All banks and debts saved to the database.").replace("&", "§");
                    Bukkit.getConsoleSender().sendMessage(msg);
                }
            }
        }.runTaskTimer(this, 0L, getConfig().getInt("auto-save.save-interval", 15) * 1200L);
    }

    public void startDebtCheckScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Debt debt : debts.values()) {
                   if (DebtModule.hasDefinedHoursPassed(debt.getUsername())){
                       if (Bukkit.getPlayer(debt.getUsername()) != null){
                           Player player = Bukkit.getPlayer(debt.getUsername());
                           if (banks.get(player.getName()).getBalance() < debt.getDaily()) {
                               debt.setRemaining(debt.getRemaining() + debt.getDaily());
                               DebtModule.updateLastPaymentDate(debt.getUsername());
                               TextUtils.sendMessageWithPrefix(player, getConfig().getString("messages.debt-cant-paid").replaceAll("%money%", MiscUtils.formatBalance(debt.getDaily())));
                           } else {
                               DebtModule.payDebt(player, debt.getDaily());
                           }
                       } else {
                           Bank bank = null;
                           try {
                               bank = db.getBank(debt.getUsername());
                           } catch (SQLException e) {
                               getLogger().warning("An error occurred while getting bank for debt payment for " + debt.getUsername());
                           }
                           assert bank != null;
                           if (bank.getBalance() < debt.getDaily()) {
                            debt.setRemaining(debt.getRemaining() + debt.getDaily());
                            DebtModule.updateLastPaymentDate(debt.getUsername());
                        } else {
                            bank.setBalance(bank.getBalance() - debt.getDaily());
                            debt.setRemaining(debt.getRemaining() - debt.getDaily());
                            try {
                                db.updateBankInDatabase(bank);
                            } catch (SQLException e) {
                                getLogger().warning("An error occurred while updating bank balance for debt payment for " + debt.getUsername());
                            }
                        }
                       }
                   }
                }
            }
        }.runTaskTimer(this, 0L, 1200L);
    }

    public void startInterestScheduler() {
        double interest = plugin.getConfig().getDouble("interest.default-interest-rate", plugin.getConfig().getDouble("interest.interest-rate"));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (InterestModule.hasDefinedHoursPassed()) {
                    try {
                        db.applyInterest(interest);
                    } catch (SQLException e) {
                        getLogger().warning("An error occurred while applying interest to all banks");
                    }
                    InterestModule.updateLastInterestDate();
                }
            }
        }.runTaskTimer(plugin, 600L, 1200L);
    }

    public void setCommands(){
        getCommand("sbank").setExecutor(new BankCommands());
    }

    public void setListeners(){
        getServer().getPluginManager().registerEvents(new BankGuiListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerBankChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerLoanChatListener(), this);
        getServer().getPluginManager().registerEvents(new LoanGuiListener(), this);
        getServer().getPluginManager().registerEvents(new DebtGuiListener(), this);
        


        if(getServer().getPluginManager().isPluginEnabled("Citizens")) {
            getLogger().info("Citizens found, initializing NPC support...");
            getServer().getPluginManager().registerEvents(new NPCClickEvent(), this);
        }
    }

    public void saveAllBanks(){
        if (!banks.isEmpty()){
            for (Bank bank : banks.values()) {
                try {
                    db.updateBankInDatabase(bank);
                } catch (SQLException e) {
                    getLogger().warning("An error occurred while saving bank with name " + bank.getUsername());
                }
            }
        }
    }

    public void saveAllDebts(){
        if (!debts.isEmpty()){
            for (Debt debt : debts.values()) {
                try {
                    db.updateDebtInDatabase(debt);
                } catch (SQLException e) {
                    getLogger().warning("An error occurred while saving debt with name " + debt.getUsername());
                }
            }
        }
    }
}
