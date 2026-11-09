#!/usr/bin/env python3
"""Generate Jarvis' one-shot refusal sound (one fixed tone, 250 ms).

Creates a mono 16-bit PCM WAV during the Android build. No melody, no multiple
notes, no loop, no downloads or third-party sound assets.
"""
from pathlib import Path
import math
import struct
import wave

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "android" / "app" / "src" / "main" / "res" / "raw" / "jarvis_refusal.wav"
SAMPLE_RATE = 22050
FREQUENCY_HZ = 220.0
DURATION_MS = 250
FRAMES = SAMPLE_RATE * DURATION_MS // 1000
ATTACK = SAMPLE_RATE * 8 // 1000
RELEASE = SAMPLE_RATE * 18 // 1000

def make_sample(i):
    # A single 220-Hz tone with gentle saturation for a raspy BZZT timbre.
    # Saturation adds harmonics to ONE pitch; it never changes the pitch or
    # generates a second tone. Keep a fade to prevent loud clicks.
    phase = 2.0 * math.pi * FREQUENCY_HZ * i / SAMPLE_RATE
    raw = math.tanh(3.0 * math.sin(phase)) / math.tanh(3.0)
    fade_in = min(1.0, i / max(1, ATTACK))
    fade_out = min(1.0, (FRAMES - 1 - i) / max(1, RELEASE))
    envelope = max(0.0, min(fade_in, fade_out))
    return round(32767 * 0.34 * envelope * raw)

def main():
    DEST.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(DEST), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(b"".join(struct.pack("<h", make_sample(i)) for i in range(FRAMES)))
    with wave.open(str(DEST), "rb") as check:
        assert check.getnchannels() == 1
        assert check.getsampwidth() == 2
        assert check.getframerate() == SAMPLE_RATE
        assert check.getnframes() == FRAMES
    print(f"Prepared one-shot Jarvis refusal tone: {FREQUENCY_HZ:.0f} Hz, {DURATION_MS} ms")

if __name__ == "__main__":
    main()
