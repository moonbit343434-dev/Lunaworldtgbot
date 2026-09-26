package com.lunatgbot.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Управляет привязками Minecraft UUID ↔ Telegram ID
 * и временными кодами подтверждения.
 */
public class LinkManager {

    private final LunaTGBot plugin;
    private final File dataFile;
    private FileConfiguration data;

    // uuid -> telegramId
    private final Map<UUID, Long> linkedAccounts = new HashMap<>();
    // telegramId -> uuid (обратный индекс)
    private final Map<Long, UUID> telegramToUuid = new HashMap<>();
    // code -> uuid (временные коды, живут 10 минут)
    private final Map<String, UUID> pendingCodes = new HashMap<>();
    private final Map<String, Long> codeExpiry   = new HashMap<>();

    private static final long CODE_TTL = 10 * 60 * 1000L; // 10 минут

    public LinkManager(LunaTGBot plugin) {
        this.plugin  = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "links.yml");
        load();
    }

    // ── Генерация кода ───────────────────────────────────────────────────

    public String generateCode(UUID playerUuid) {
        // Чистим старые коды этого игрока
        pendingCodes.entrySet().removeIf(e -> e.getValue().equals(playerUuid));

        String code = String.format("%06d", new Random().nextInt(1000000));
        pendingCodes.put(code, playerUuid);
        codeExpiry.put(code, System.currentTimeMillis() + CODE_TTL);
        return code;
    }

    public boolean isCodeValid(String code) {
        Long expiry = codeExpiry.get(code);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            pendingCodes.remove(code);
            codeExpiry.remove(code);
            return false;
        }
        return pendingCodes.containsKey(code);
    }

    /** Возвращает UUID игрока по коду и удаляет код */
    public UUID consumeCode(String code) {
        UUID uuid = pendingCodes.remove(code);
        codeExpiry.remove(code);
        return uuid;
    }

    // ── Привязка ─────────────────────────────────────────────────────────

    public void link(UUID playerUuid, long telegramId) {
        // Если у этого tg уже была привязка — убираем
        UUID old = telegramToUuid.get(telegramId);
        if (old != null) linkedAccounts.remove(old);

        linkedAccounts.put(playerUuid, telegramId);
        telegramToUuid.put(telegramId, playerUuid);
        save();
    }

    public void unlink(UUID playerUuid) {
        Long tgId = linkedAccounts.remove(playerUuid);
        if (tgId != null) telegramToUuid.remove(tgId);
        save();
    }

    public boolean isLinked(UUID playerUuid) {
        return linkedAccounts.containsKey(playerUuid);
    }

    public Long getTelegramId(UUID playerUuid) {
        return linkedAccounts.get(playerUuid);
    }

    public UUID getUuidByTelegram(long telegramId) {
        return telegramToUuid.get(telegramId);
    }

    // ── Сохранение / загрузка ────────────────────────────────────────────

    public void save() {
        data = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : linkedAccounts.entrySet()) {
            data.set("links." + e.getKey().toString(), e.getValue());
        }
        try { data.save(dataFile); }
        catch (IOException ex) { plugin.getLogger().warning("Не удалось сохранить links.yml: " + ex.getMessage()); }
    }

    private void load() {
        if (!dataFile.exists()) return;
        data = YamlConfiguration.loadConfiguration(dataFile);
        if (data.getConfigurationSection("links") == null) return;
        for (String key : data.getConfigurationSection("links").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long tgId = data.getLong("links." + key);
                linkedAccounts.put(uuid, tgId);
                telegramToUuid.put(tgId, uuid);
            } catch (Exception ignored) {}
        }
        plugin.getLogger().info("Загружено привязок: " + linkedAccounts.size());
    }
}
