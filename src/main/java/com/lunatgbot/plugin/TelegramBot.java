package com.lunatgbot.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Простой Telegram Long Polling бот без внешних библиотек.
 * Работает через стандартный java.net.HttpURLConnection.
 */
public class TelegramBot {

    private final LunaTGBot plugin;
    private final Logger log;
    private final String token;
    private final String apiBase;

    private volatile boolean running = false;
    private Thread pollThread;
    private long lastUpdateId = 0;

    public TelegramBot(LunaTGBot plugin, String token) {
        this.plugin  = plugin;
        this.token   = token;
        this.apiBase = "https://api.telegram.org/bot" + token;
        this.log     = plugin.getLogger();
    }

    public void start() {
        running    = true;
        pollThread = new Thread(this::pollLoop, "LunaTGBot-Poll");
        pollThread.setDaemon(true);
        pollThread.start();
        log.info("Telegram polling запущен.");
    }

    public void stop() {
        running = false;
        if (pollThread != null) pollThread.interrupt();
    }

    // ── Long Polling loop ────────────────────────────────────────────────

    private void pollLoop() {
        while (running) {
            try {
                String url = apiBase + "/getUpdates?timeout=30&offset=" + (lastUpdateId + 1);
                String response = get(url);
                if (response != null) parseUpdates(response);
            } catch (Exception e) {
                if (running) log.warning("Ошибка polling: " + e.getMessage());
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            }
        }
    }

    // ── Простой JSON парсер (без библиотек) ──────────────────────────────

    private void parseUpdates(String json) {
        // Ищем все блоки update_id
        int pos = 0;
        while (true) {
            int idx = json.indexOf("\"update_id\"", pos);
            if (idx == -1) break;

            long updateId = parseLong(json, idx + 12);
            if (updateId > lastUpdateId) lastUpdateId = updateId;

            // Ищем текст сообщения
            int textIdx = json.indexOf("\"text\"", idx);
            int nextUpdate = json.indexOf("\"update_id\"", idx + 1);
            if (textIdx == -1 || (nextUpdate != -1 && textIdx > nextUpdate)) {
                pos = idx + 1;
                continue;
            }

            String text = parseString(json, textIdx + 7);

            // Ищем chat id
            int chatIdx = json.indexOf("\"id\"", textIdx);
            // Ищем первый "id" после "chat"
            int chatSection = json.indexOf("\"chat\"", idx);
            long chatId = 0;
            if (chatSection != -1) {
                int chatIdIdx = json.indexOf("\"id\"", chatSection);
                if (chatIdIdx != -1) chatId = parseLong(json, chatIdIdx + 5);
            }

            // Имя пользователя
            String firstName = "";
            int fnIdx = json.indexOf("\"first_name\"", idx);
            if (fnIdx != -1 && (nextUpdate == -1 || fnIdx < nextUpdate)) {
                firstName = parseString(json, fnIdx + 13);
            }

            if (text != null && chatId != 0) {
                handleMessage(chatId, firstName, text.trim());
            }

            pos = idx + 1;
        }
    }

    private void handleMessage(long chatId, String firstName, String text) {
        LinkManager lm = plugin.getLinkManager();

        // /start — приветствие
        if (text.equals("/start")) {
            sendMessage(chatId,
                "👋 Привет, " + firstName + "!\n\n" +
                "Я бот сервера Minecraft.\n" +
                "Чтобы привязать аккаунт:\n" +
                "1. Зайди на сервер\n" +
                "2. Напиши /tglink\n" +
                "3. Отправь мне полученный код"
            );
            return;
        }

        // Код привязки — 6 цифр
        if (text.matches("\\d{6}")) {
            if (!lm.isCodeValid(text)) {
                sendMessage(chatId, "❌ Код недействителен или устарел.\nПолучи новый командой /tglink на сервере.");
                return;
            }

            UUID playerUuid = lm.consumeCode(text);
            if (playerUuid == null) {
                sendMessage(chatId, "❌ Ошибка привязки. Попробуй снова.");
                return;
            }

            // Проверяем не привязан ли уже этот tg
            if (lm.getUuidByTelegram(chatId) != null) {
                sendMessage(chatId, "⚠ Этот Telegram уже привязан к другому аккаунту.");
                return;
            }

            lm.link(playerUuid, chatId);

            // Выдаём награду
            double reward = getRandomReward();
            final double finalReward = reward;
            final UUID finalUuid = playerUuid;
            final long finalChatId = chatId;

            // Выдаём валюту в основном потоке Bukkit
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(finalUuid);
                String playerName;
                if (player != null) {
                    playerName = player.getName();
                } else {
                    playerName = Bukkit.getOfflinePlayer(finalUuid).getName();
                    if (playerName == null) playerName = "Игрок";
                }

                plugin.getEconomy().depositPlayer(Bukkit.getOfflinePlayer(finalUuid), finalReward);

                String currency = plugin.getConfig().getString("messages.currency-name", "монет");
                String rewardFormatted = String.format("%.0f", finalReward);

                // Сообщение в игре
                if (player != null) {
                    String msg = plugin.getConfig().getString("messages.linked-ingame",
                        "&aТвой Telegram успешно привязан! Награда: &e{amount} {currency}")
                        .replace("{amount}", rewardFormatted)
                        .replace("{currency}", currency)
                        .replace("&a", "§a").replace("&e", "§e").replace("&b", "§b");
                    player.sendMessage(msg);
                }

                // Сообщение в Telegram
                String tgMsg = plugin.getConfig().getString("messages.linked-telegram",
                    "✅ Аккаунт {player} успешно привязан!\n💰 Награда: {amount} {currency}")
                    .replace("{player}", playerName)
                    .replace("{amount}", rewardFormatted)
                    .replace("{currency}", currency);
                sendMessage(finalChatId, tgMsg);

                plugin.getLogger().info("Привязка: " + playerName + " -> TG:" + finalChatId +
                    " | Награда: " + rewardFormatted);
            });
            return;
        }

        // /info — проверить привязку
        if (text.equals("/info")) {
            UUID uuid = lm.getUuidByTelegram(chatId);
            if (uuid == null) {
                sendMessage(chatId, "❌ Твой Telegram не привязан ни к одному аккаунту.");
            } else {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                sendMessage(chatId, "✅ Привязан к аккаунту: " + (name != null ? name : uuid.toString()));
            }
            return;
        }

        // Неизвестная команда
        sendMessage(chatId, "❓ Неизвестная команда.\nОтправь 6-значный код для привязки аккаунта.");
    }

    private double getRandomReward() {
        double min = plugin.getConfig().getDouble("reward.min", 100.0);
        double max = plugin.getConfig().getDouble("reward.max", 1000.0);
        if (min >= max) return min;
        return min + (new Random().nextDouble() * (max - min));
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────

    public void sendMessage(long chatId, String text) {
        try {
            String encoded = URLEncoder.encode(text, "UTF-8");
            String url = apiBase + "/sendMessage?chat_id=" + chatId +
                "&text=" + encoded + "&parse_mode=HTML";
            get(url);
        } catch (Exception e) {
            log.warning("Не удалось отправить сообщение: " + e.getMessage());
        }
    }

    private String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(35000);
        conn.setReadTimeout(35000);

        int code = conn.getResponseCode();
        InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return null;

        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        return sb.toString();
    }

    // ── JSON parsers ─────────────────────────────────────────────────────

    private long parseLong(String json, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || c == '-') sb.append(c);
            else if (sb.length() > 0) break;
        }
        try { return Long.parseLong(sb.toString()); }
        catch (Exception e) { return 0; }
    }

    private String parseString(String json, int from) {
        int start = json.indexOf('"', from);
        if (start == -1) return null;
        start++;
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) { sb.append(c); escape = false; }
            else if (c == '\\') escape = true;
            else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }
}
