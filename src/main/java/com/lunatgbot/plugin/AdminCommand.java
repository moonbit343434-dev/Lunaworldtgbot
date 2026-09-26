package com.lunatgbot.plugin;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class AdminCommand implements CommandExecutor {

    private final LunaTGBot plugin;

    public AdminCommand(LunaTGBot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lunatgbot.admin")) {
            sender.sendMessage("§cНет доступа.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§aКонфиг перезагружен.");
                break;

            case "unlink":
                if (args.length < 2) { sender.sendMessage("§cУкажи ник игрока."); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (target == null) { sender.sendMessage("§cИгрок не найден."); return true; }
                if (!plugin.getLinkManager().isLinked(target.getUniqueId())) {
                    sender.sendMessage("§cЭтот игрок не привязан.");
                    return true;
                }
                plugin.getLinkManager().unlink(target.getUniqueId());
                sender.sendMessage("§aАккаунт " + args[1] + " отвязан.");
                break;

            case "check":
                if (args.length < 2) { sender.sendMessage("§cУкажи ник игрока."); return true; }
                OfflinePlayer check = Bukkit.getOfflinePlayer(args[1]);
                Long tgId = plugin.getLinkManager().getTelegramId(check.getUniqueId());
                if (tgId == null) {
                    sender.sendMessage("§e" + args[1] + " §cне привязан.");
                } else {
                    sender.sendMessage("§e" + args[1] + " §aпривязан. TG ID: §f" + tgId);
                }
                break;

            default:
                sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== LunaTGBot Admin ===");
        sender.sendMessage("§e/tgbot reload §7— перезагрузить конфиг");
        sender.sendMessage("§e/tgbot unlink <ник> §7— отвязать игрока");
        sender.sendMessage("§e/tgbot check <ник> §7— проверить привязку");
    }
}
