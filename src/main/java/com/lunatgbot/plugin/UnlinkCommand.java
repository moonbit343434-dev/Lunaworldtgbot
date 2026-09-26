package com.lunatgbot.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnlinkCommand implements CommandExecutor {

    private final LunaTGBot plugin;

    public UnlinkCommand(LunaTGBot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько игроки могут использовать эту команду.");
            return true;
        }

        Player player = (Player) sender;
        LinkManager lm = plugin.getLinkManager();

        if (!lm.isLinked(player.getUniqueId())) {
            player.sendMessage("§cТвой аккаунт не привязан к Telegram.");
            return true;
        }

        lm.unlink(player.getUniqueId());
        String msg = plugin.getConfig().getString("messages.unlinked",
            "&aТвой Telegram аккаунт успешно отвязан.")
            .replace("&a", "§a").replace("&e", "§e");
        player.sendMessage(msg);
        return true;
    }
}
