# Telegram Video Link Bot

A small Telegram bot that accepts a public Pornhub video-page URL, inspects formats exposed by yt-dlp, asks the user to select a quality, and returns the selected direct stream URL when available.

## Important limitations

- A direct media URL is not guaranteed to work from every IP, device, or network. The source may use expiring signatures, session cookies, or IP restrictions. The bot does not proxy or re-host video data and does not attempt to bypass access controls.
- Use this only for videos you own or are authorized to download, and follow the source site's terms and applicable law.
- Some videos expose no direct formats, or may be unavailable/private/session restricted. In that case the bot returns the original page.
- This project is separate from the Unity game; it lives in `telegram-video-bot/`.

## 1. Create a Telegram bot

1. Open Telegram and chat with the official BotFather account.
2. Run `/newbot`, follow the prompts, and copy the bot token.
3. Keep the token private. Never commit it to GitHub or paste it into public issues.

## 2. Run locally or on a server

Python 3.11+ is recommended.

```bash
cd telegram-video-bot
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export TELEGRAM_BOT_TOKEN="paste-your-token-here"
python bot.py
```

On Android, the same Python code can run in Termux, but polling stops if Android kills Termux or the phone loses connectivity.

## 3. Deploy for continuous uptime

GitHub Actions is used for tests only; it is not a permanent bot host. Deploy this folder to an always-on Python worker/service that supports long-running processes. Set the start command to:

```bash
python bot.py
```

Add `TELEGRAM_BOT_TOKEN` in the host's environment/secrets settings. Do not place the token in the repository. Free hosting tiers may sleep or have usage limits.

## Bot commands

- `/start` — usage and limitations
- `/help` — usage and limitations
- Send a public Pornhub video page URL — inspect available qualities and choose one.

## Development checks

```bash
pip install -r requirements.txt
python -m compileall -q .
python -m unittest discover -s tests -v
```
