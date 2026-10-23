# Telegram Video Downloader Bot

A Telegram bot that accepts a public Pornhub video-page URL, shows available video qualities, downloads the selected format, and sends the actual video file in the Telegram chat.

## Important limitations

- The standard hosted Telegram Bot API has an upload limit of about 50 MB. This bot limits uploads to 49 MiB; choose a lower quality if a video is too large.
- Downloading uses the bot host's internet connection and temporary disk space. The actual video is uploaded to Telegram; the bot does not merely return a direct stream URL.
- Some videos may be unavailable, private, session restricted, or unsupported by yt-dlp. The bot cannot guarantee every link works.
- Use this only for content you own or are authorized to download, and follow the source site's terms and applicable law.
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

GitHub Actions runners are temporary and are not permanent bot hosts. Deploy this folder to an always-on Python worker/service that supports long-running processes. Set the start command to:

```bash
python bot.py
```

Add `TELEGRAM_BOT_TOKEN` in the host's environment/secrets settings. Do not place the token in the repository. Free hosting tiers may sleep or have usage limits.

## Bot commands

- `/start` — usage and limitations
- `/help` — usage and limitations
- Send a public Pornhub video page URL — choose an available quality; the bot downloads and sends the actual video file.

## Development checks

```bash
pip install -r requirements.txt
python -m compileall -q .
python -m unittest discover -s tests -v
```
