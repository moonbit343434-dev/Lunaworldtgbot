package com.lunatgbot.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LinkCommand implements CommandExecutor {

    private final LunaTGBot plugin;

    public LinkCommand(LunaTGBot plugin) {
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

        // Уже привязан
        if (lm.isLinked(player.getUniqueId())) {
            String msg = plugin.getConfig().getString("messages.already-linked",
                "&eТвой аккаунт уже привязан к Telegram. Используй /tgunlink чтобы отвязать.")
                .replace("&e", "§e").replace("&a", "§a").replace("&c", "§c");
            player.sendMessage(msg);
            return true;
        }

        // Генерируем код
        String code = lm.generateCode(player.getUniqueId());
        String botName = plugin.getConfig().getString("bot-username", "your_bot");

        String msg1 = plugin.getConfig().getString("messages.link-code",
            "&aТвой код привязки: &e{code}")
            .replace("{code}", code)
            .replace("&a", "§a").replace("&e", "§e");

        String msg2 = plugin.getConfig().getString("messages.link-instruction",
            "&7Отправь этот код боту &b@{bot} &7в Telegram. Код действителен 10 минут.")
            .replace("{bot}", botName)
            .replace("&7", "§7").replace("&b", "§b");

        player.sendMessage(msg1);
        player.sendMessage(msg2);
        return true;
    }
}
