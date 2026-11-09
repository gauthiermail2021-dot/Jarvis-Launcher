#!/usr/bin/env python3
"""Prepare standalone 16-bit PCM WAV resources for Jarvis SoundPool.

If the four original tested WAV files are available in android/audio-sources/,
copy them unchanged. Otherwise do not silently replace them with synthesized
samples: fail the build explicitly until the originals are staged.
"""
from pathlib import Path
import shutil
import wave

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "android" / "audio-sources"
DEST = ROOT / "android" / "app" / "src" / "main" / "res" / "raw"
FILES = {
    "Jarvis_Xylophone_Do5.wav": "jarvis_do5.wav",
    "Jarvis_Xylophone_Re5.wav": "jarvis_re5.wav",
    "Jarvis_Xylophone_Mi5.wav": "jarvis_mi5.wav",
    "Jarvis_Xylophone_Sol5.wav": "jarvis_sol5.wav",
}

def main():
    missing = [src for src in FILES if not (SOURCE / src).is_file()]
    if missing:
        raise SystemExit(
            "Four original WAVs are required in android/audio-sources/. "
            "Missing: " + ", ".join(missing)
        )
    DEST.mkdir(parents=True, exist_ok=True)
    for name, target in FILES.items():
        source = SOURCE / name
        with wave.open(str(source), "rb") as audio:
            if audio.getcomptype() != "NONE" or audio.getsampwidth() != 2:
                raise SystemExit("Expected uncompressed 16-bit PCM WAV: " + name)
            if audio.getnchannels() not in (1, 2):
                raise SystemExit("Invalid WAV channels: " + name)
        shutil.copyfile(source, DEST / target)
        print(f"Prepared {target}: {source.stat().st_size} bytes")

if __name__ == "__main__":
    main()
