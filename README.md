# 🌙 LunaTGBot

Плагин для Minecraft 1.21.4 (Paper/Spigot) — привязка Telegram аккаунта с выдачей рандомной награды в валюте.

## Установка

1. Скачай `LunaTGBot-1.0.0.jar` из раздела Actions
2. Положи в папку `plugins/` сервера
3. Убедись что установлен **Vault** + любой Economy плагин (EssentialsX, CMI и т.д.)
4. Запусти сервер — создастся `plugins/LunaTGBot/config.yml`
5. Укажи токен бота в `config.yml`:
```yaml
bot-token: "123456789:AAFxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
bot-username: "your_bot"
```
6. Перезапусти сервер

## Создание бота

1. Напиши @BotFather в Telegram
2. Команда `/newbot`
3. Выбери имя и username
4. Скопируй токен в config.yml

## Команды

| Команда | Описание |
|---------|----------|
| `/tglink` | Получить код привязки |
| `/tgunlink` | Отвязать аккаунт |
| `/tgbot reload` | Перезагрузить конфиг (admin) |
| `/tgbot unlink <ник>` | Отвязать игрока (admin) |
| `/tgbot check <ник>` | Проверить привязку (admin) |

## Как работает привязка

1. Игрок пишет `/tglink` на сервере
2. Получает 6-значный код (действует 10 минут)
3. Отправляет код боту в Telegram
4. Бот привязывает аккаунт и выдаёт рандомную валюту

## Настройка награды (config.yml)

```yaml
reward:
  min: 100.0   # минимум
  max: 1000.0  # максимум
```

## Зависимости

- Paper/Spigot 1.21.4
- Vault
- Любой Economy плагин
