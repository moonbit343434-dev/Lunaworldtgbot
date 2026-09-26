package com.lunatgbot.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.economy.Economy;

public class LunaTGBot extends JavaPlugin {

    private static LunaTGBot instance;
    private Economy economy;
    private TelegramBot telegramBot;
    private LinkManager linkManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Vault
        if (!setupEconomy()) {
            getLogger().severe("Vault не найден! Установи Vault и Economy плагин.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Менеджер привязок
        linkManager = new LinkManager(this);

        // Telegram бот
        String token = getConfig().getString("bot-token", "");
        if (token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            getLogger().severe("Укажи токен бота в config.yml (bot-token)");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        telegramBot = new TelegramBot(this, token);
        telegramBot.start();

        // Команды
        getCommand("tglink").setExecutor(new LinkCommand(this));
        getCommand("tgunlink").setExecutor(new UnlinkCommand(this));
        getCommand("tgbot").setExecutor(new AdminCommand(this));

        getLogger().info("╔══════════════════════════╗");
        getLogger().info("║   LunaTGBot запущен!     ║");
        getLogger().info("╚══════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (telegramBot != null) telegramBot.stop();
        if (linkManager != null) linkManager.save();
        getLogger().info("LunaTGBot остановлен.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
            getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static LunaTGBot getInstance() { return instance; }
    public Economy getEconomy() { return economy; }
    public TelegramBot getTelegramBot() { return telegramBot; }
    public LinkManager getLinkManager() { return linkManager; }
}
