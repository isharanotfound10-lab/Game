import asyncio
import logging
import os
import tempfile
from pathlib import Path
from urllib.parse import urlparse

import yt_dlp
from telegram import InlineKeyboardButton, InlineKeyboardMarkup, Update
from telegram.constants import ChatAction
from telegram.error import TelegramError
from telegram.ext import (
    Application,
    CallbackQueryHandler,
    CommandHandler,
    ContextTypes,
    MessageHandler,
    filters,
)

logging.basicConfig(
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    level=logging.INFO,
)
logging.getLogger("httpx").setLevel(logging.WARNING)
log = logging.getLogger("video-link-bot")

TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
MAX_FORMAT_BUTTONS = 8
# Telegram's hosted Bot API has a 50 MB upload limit; stay below it.
MAX_UPLOAD_BYTES = 49 * 1024 * 1024
PENDING: dict[tuple[int, str], dict] = {}


def valid_video_url(value: str) -> bool:
    try:
        parsed = urlparse(value.strip())
        host = (parsed.hostname or "").lower().rstrip(".")
        return (
            parsed.scheme in {"http", "https"}
            and (host == "pornhub.com" or host.endswith(".pornhub.com"))
        )
    except ValueError:
        return False


def ydl_options() -> dict:
    return {
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        "skip_download": True,
        "socket_timeout": 25,
        "retries": 2,
        # Leave impersonation unset: current yt-dlp Python API expects an
        # ImpersonateTarget object, not the CLI-style string "chrome".
    }


def list_download_formats(info: dict) -> list[dict]:
    """Return direct MP4 formats likely to play in Telegram (H.264 video + AAC audio)."""
    by_height = {}
    for fmt in info.get("formats") or []:
        direct_url = fmt.get("url")
        if not direct_url or urlparse(direct_url).scheme not in {"http", "https"}:
            continue
        # Telegram/mobile players are most compatible with MP4 containing H.264 + AAC.
        if fmt.get("ext") != "mp4":
            continue
        if not str(fmt.get("vcodec") or "").startswith("avc1"):
            continue
        if not str(fmt.get("acodec") or "").startswith("mp4a"):
            continue
        height = fmt.get("height") or 0
        if height <= 0:
            continue
        old = by_height.get(height)
        if old is None or (fmt.get("tbr") or 0) > (old.get("tbr") or 0):
            by_height[height] = fmt
    return sorted(by_height.values(), key=lambda item: item.get("height") or 0, reverse=True)


def format_button_rows(formats: list[dict]) -> list[list[InlineKeyboardButton]]:
    buttons = []
    for index, fmt in enumerate(formats[:MAX_FORMAT_BUTTONS]):
        height = fmt.get("height") or "?"
        ext = fmt.get("ext") or "video"
        buttons.append(InlineKeyboardButton(f"{height}p · {ext}", callback_data=f"quality:{index}"))
    rows = [buttons[i:i + 2] for i in range(0, len(buttons), 2)]
    rows.append([InlineKeyboardButton("Open original page", callback_data="original")])
    return rows


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    if update.message:
        await update.message.reply_text(
            "Send a public Pornhub video page URL. I'll show available qualities; "
            "when you choose one, I'll download it and send the actual video file here.\n\n"
            "Telegram's standard bot upload limit is about 50 MB, so larger files may not send. "
            "Only use this for content you own or are authorized to download."
        )


async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    await start(update, context)


async def receive_url(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    message = update.message
    if not message or not message.text:
        return
    url = message.text.strip()
    if not valid_video_url(url):
        await message.reply_text("Please send a valid https://www.pornhub.com/... video page URL.")
        return

    await message.chat.send_action(ChatAction.TYPING)
    try:
        info = await asyncio.to_thread(_extract_info, url)
        formats = list_download_formats(info)
    except Exception as exc:
        log.exception("Video extraction failed (%s): %s", type(exc).__name__, exc)
        error_text = str(exc).lower()
        if "410" in error_text or "gone" in error_text:
            user_error = (
                "Pornhub returned HTTP 410 (Gone) before video qualities could be read. "
                "I retried with a browser-like request, but the site may still be rejecting this server "
                "or yt-dlp may need a site-side fix. This is not a Telegram upload error."
            )
        else:
            user_error = (
                "I couldn't read that page. It may be private, unavailable, session restricted, "
                "or unsupported. Check the bot's GitHub Actions log for the detailed error."
            )
        await message.reply_text(user_error)
        return

    title = str(info.get("title") or "Video")[:180]
    key = (message.chat_id, str(message.from_user.id if message.from_user else "unknown"))
    PENDING[key] = {"url": url, "title": title, "formats": formats[:MAX_FORMAT_BUTTONS]}
    if not formats:
        await message.reply_text(
            f"{title}\n\nNo downloadable video formats were exposed. Original page: {url}",
            disable_web_page_preview=True,
        )
        return

    await message.reply_text(
        f"{title}\n\nChoose a quality. I'll send the actual video file (up to about 49 MB):",
        reply_markup=InlineKeyboardMarkup(format_button_rows(formats)),
        disable_web_page_preview=True,
    )


def _extract_info(url: str) -> dict:
    with yt_dlp.YoutubeDL(ydl_options()) as ydl:
        return ydl.extract_info(url, download=False)


def _download_format(url: str, format_id: str, output_dir: str) -> str:
    options = {
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        "socket_timeout": 30,
        "retries": 2,
        "format": format_id,
        "outtmpl": str(Path(output_dir) / "video.%(ext)s"),
        "max_filesize": MAX_UPLOAD_BYTES,
    }
    with yt_dlp.YoutubeDL(options) as ydl:
        ydl.download([url])
    files = [p for p in Path(output_dir).glob("video.*") if p.is_file()]
    if not files:
        raise FileNotFoundError("The downloader did not produce a video file.")
    path = max(files, key=lambda p: p.stat().st_size)
    if path.stat().st_size > MAX_UPLOAD_BYTES:
        raise ValueError("The video is larger than Telegram's upload limit.")
    return str(path)


async def choose_quality(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    query = update.callback_query
    if not query:
        return
    await query.answer()
    user_id = str(query.from_user.id)
    key = (query.message.chat_id, user_id) if query.message else None
    data = PENDING.get(key) if key else None
    if not data:
        await query.edit_message_text("That selection expired. Send the video URL again.")
        return

    if query.data == "original":
        await query.edit_message_text(f"Original page:\n{data['url']}", disable_web_page_preview=True)
        return

    try:
        index = int((query.data or "").split(":", 1)[1])
        fmt = data["formats"][index]
        format_id = str(fmt["format_id"])
    except (ValueError, IndexError, KeyError):
        await query.edit_message_text("That quality is no longer available. Send the URL again.")
        return

    height = fmt.get("height") or "unknown"
    ext = str(fmt.get("ext") or "video").lower()
    try:
        await query.edit_message_text(
            f"Downloading {height}p now… I'll send the video here when it's ready."
        )
        with tempfile.TemporaryDirectory(prefix="telegram-video-") as temp_dir:
            path = await asyncio.to_thread(_download_format, data["url"], format_id, temp_dir)
            with open(path, "rb") as video_file:
                if ext == "mp4" or path.lower().endswith(".mp4"):
                    await query.message.reply_video(
                        video=video_file,
                        caption=f"{data['title']} · {height}p"[:1024],
                        read_timeout=180,
                        write_timeout=180,
                        connect_timeout=30,
                        pool_timeout=30,
                    )
                else:
                    await query.message.reply_document(
                        document=video_file,
                        filename=Path(path).name,
                        caption=f"{data['title']} · {height}p"[:1024],
                        read_timeout=180,
                        write_timeout=180,
                        connect_timeout=30,
                        pool_timeout=30,
                    )
        await query.message.reply_text("✅ Video file sent.")
    except ValueError:
        await query.message.reply_text(
            "That quality is too large for Telegram's standard bot upload limit (about 50 MB). "
            "Choose a lower quality."
        )
    except Exception as exc:
        log.info("Download/send failed (%s)", type(exc).__name__)
        await query.message.reply_text(
            "I couldn't download or send that quality. Try a lower quality or open the original page: "
            + data["url"],
            disable_web_page_preview=True,
        )


def main() -> None:
    if not TOKEN:
        raise SystemExit("Missing TELEGRAM_BOT_TOKEN environment variable.")
    app = Application.builder().token(TOKEN).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("help", help_command))
    app.add_handler(CallbackQueryHandler(choose_quality, pattern=r"^(quality:\d+|original)$"))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, receive_url))
    log.info("Starting Telegram bot polling.")
    app.run_polling(drop_pending_updates=True)


if __name__ == "__main__":
    main()
