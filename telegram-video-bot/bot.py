import asyncio
import logging
import os
from urllib.parse import urlparse

import yt_dlp
from telegram import InlineKeyboardButton, InlineKeyboardMarkup, Update
from telegram.constants import ChatAction
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
# Per-process state is intentionally temporary; no submitted URLs are persisted.
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
        "socket_timeout": 20,
        "retries": 1,
    }


def quality_label(fmt: dict) -> str:
    height = fmt.get("height")
    if height:
        return f"{height}p"
    label = fmt.get("format_note") or fmt.get("resolution")
    if label and label != "unknown":
        return str(label)[:24]
    return str(fmt.get("format_id", "Format"))[:24]


def list_download_formats(info: dict) -> list[dict]:
    """Return unique, direct HTTP(S) video formats, preferring common heights."""
    formats = info.get("formats") or []
    candidates = []
    seen = set()
    for fmt in formats:
        direct_url = fmt.get("url")
        if not direct_url or urlparse(direct_url).scheme not in {"http", "https"}:
            continue
        # Audio-only entries are not useful for this video-link bot.
        if fmt.get("vcodec") == "none":
            continue
        height = fmt.get("height") or 0
        if height <= 0:
            continue
        key = (height, fmt.get("ext"), fmt.get("vcodec"), fmt.get("acodec"))
        if key in seen:
            continue
        seen.add(key)
        candidates.append(fmt)

    # One useful option per height, preferring muxed audio/video and then larger bitrate.
    by_height = {}
    for fmt in candidates:
        height = fmt.get("height") or 0
        score = (
            1 if fmt.get("acodec") not in (None, "none") else 0,
            fmt.get("tbr") or 0,
        )
        if height not in by_height or score > (
            1 if by_height[height].get("acodec") not in (None, "none") else 0,
            by_height[height].get("tbr") or 0,
        ):
            by_height[height] = fmt

    return sorted(by_height.values(), key=lambda item: item.get("height") or 0, reverse=True)


def format_button_rows(formats: list[dict]) -> list[list[InlineKeyboardButton]]:
    buttons = []
    for index, fmt in enumerate(formats[:MAX_FORMAT_BUTTONS]):
        height = fmt.get("height") or "?"
        ext = fmt.get("ext") or "video"
        label = f"{height}p · {ext}"
        buttons.append(InlineKeyboardButton(label, callback_data=f"quality:{index}"))
    rows = [buttons[i:i + 2] for i in range(0, len(buttons), 2)]
    rows.append([InlineKeyboardButton("Open original page", callback_data="original")])
    return rows


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    if update.message:
        await update.message.reply_text(
            "Send a public Pornhub video page URL. I'll show the available qualities "
            "and return a direct stream URL when the extractor provides one.\n\n"
            "Only use this for content you own or are authorized to download. "
            "Direct stream URLs may expire or be restricted to the original session/IP."
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
        log.info("Video extraction failed (%s)", type(exc).__name__)
        await message.reply_text(
            "I couldn't read the available formats for that page. It may be private, "
            "unavailable, age/session restricted, or unsupported. Try the original page link."
        )
        return

    title = str(info.get("title") or "Video")[:180]
    key = (message.chat_id, str(message.from_user.id if message.from_user else "unknown"))
    PENDING[key] = {"url": url, "title": title, "formats": formats[:MAX_FORMAT_BUTTONS]}
    if not formats:
        await message.reply_text(
            f"{title}\n\nNo direct video formats were exposed. Open the original page: {url}",
            disable_web_page_preview=True,
        )
        return

    await message.reply_text(
        f"{title}\n\nChoose a quality:",
        reply_markup=InlineKeyboardMarkup(format_button_rows(formats)),
        disable_web_page_preview=True,
    )


def _extract_info(url: str) -> dict:
    with yt_dlp.YoutubeDL(ydl_options()) as ydl:
        return ydl.extract_info(url, download=False)


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
        await query.edit_message_text(
            f"Original page:\n{data['url']}",
            disable_web_page_preview=True,
        )
        return

    try:
        index = int((query.data or "").split(":", 1)[1])
        fmt = data["formats"][index]
    except (ValueError, IndexError, KeyError):
        await query.edit_message_text("That quality is no longer available. Send the URL again.")
        return

    direct_url = fmt.get("url")
    if not direct_url:
        await query.edit_message_text(f"Direct link unavailable. Original page:\n{data['url']}")
        return

    height = fmt.get("height") or "unknown"
    ext = fmt.get("ext") or "video"
    await query.edit_message_text(
        f"{data['title']}\nQuality: {height}p · {ext}\n\n"
        f"Direct link (may expire or be IP/session restricted):\n{direct_url}\n\n"
        f"Original page: {data['url']}",
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
