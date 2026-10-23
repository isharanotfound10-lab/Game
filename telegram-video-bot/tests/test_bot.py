import sys
import unittest
from pathlib import Path

# Support discovery both from the repository root and from telegram-video-bot/.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from bot import list_download_formats, valid_video_url


class UrlValidationTests(unittest.TestCase):
    def test_accepts_pornhub_video_pages(self):
        self.assertTrue(valid_video_url("https://www.pornhub.com/view_video.php?viewkey=abc"))
        self.assertTrue(valid_video_url("https://pornhub.com/video/example"))

    def test_rejects_other_hosts(self):
        self.assertFalse(valid_video_url("https://example.com/video"))
        self.assertFalse(valid_video_url("https://pornhub.com.example.org/video"))


class FormatTests(unittest.TestCase):
    def test_prefers_one_format_per_height_and_skips_audio_only(self):
        info = {"formats": [
            {"format_id": "360-a", "url": "https://cdn.example/a.mp4", "height": 360,
             "ext": "mp4", "vcodec": "h264", "acodec": "none", "tbr": 500},
            {"format_id": "360-b", "url": "https://cdn.example/b.mp4", "height": 360,
             "ext": "mp4", "vcodec": "h264", "acodec": "aac", "tbr": 450},
            {"format_id": "720", "url": "https://cdn.example/c.mp4", "height": 720,
             "ext": "mp4", "vcodec": "h264", "acodec": "aac", "tbr": 1200},
            {"format_id": "audio", "url": "https://cdn.example/audio.m4a", "height": 0,
             "ext": "m4a", "vcodec": "none", "acodec": "aac", "tbr": 128},
        ]}
        result = list_download_formats(info)
        self.assertEqual([fmt["height"] for fmt in result], [720, 360])
        self.assertEqual(result[1]["format_id"], "360-b")


if __name__ == "__main__":
    unittest.main()
