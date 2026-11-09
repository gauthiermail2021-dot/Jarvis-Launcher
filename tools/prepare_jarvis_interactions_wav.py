#!/usr/bin/env python3
"""Generate three original, quiet, short Jarvis UI sound effects.

All signals are synthesized at build time, 22,050 Hz mono 16-bit PCM.
There are no copyrighted samples and no network downloads.
"""
from pathlib import Path
import math
import struct
import wave

RATE = 22050
DEST = Path(__file__).resolve().parents[1] / "android/app/src/main/res/raw"

def write(name, length_ms, sample_fn):
    total = round(RATE * length_ms / 1000)
    out = bytearray()
    for i in range(total):
        t = i / RATE
        pos = i / max(1, total - 1)
        attack = min(1.0, t / 0.005)
        release = min(1.0, (length_ms / 1000 - t) / 0.018)
        env = max(0.0, min(attack, release))
        sample = max(-1., min(1., sample_fn(t, pos) * env))
        out.extend(struct.pack("<h", round(sample * 32767)))
    path = DEST / ("jarvis_" + name + ".wav")
    with wave.open(str(path), "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(RATE)
        wf.writeframes(out)
    with wave.open(str(path), "rb") as check:
        assert check.getnframes() == total and check.getnchannels() == 1
    print(f"{path.name}: {length_ms} ms")

def tap(t, p):
    # Fine glassy tick with a quick exponential decay; no harsh attack.
    return 0.29 * math.exp(-p * 8.0) * math.sin(2 * math.pi * 1160 * t)

def whoosh(t, p):
    # Rising airy two-part digital sweep with soft landing.
    phase = 2 * math.pi * (360 * t + (980 - 360) * t * t / (2 * 0.25))
    shimmer = math.sin(2 * math.pi * 1680 * t) * 0.07
    return 0.24 * math.sin(phase) * (0.3 + 0.7 * math.sin(math.pi * p)) + shimmer * math.sin(math.pi * p)

def menu(t, p):
    # Distinct tiny affirmative confirmation, not the refusal tone.
    return 0.23 * math.sin(2 * math.pi * (740 * t + 150 * t * t)) * (1 - .35 * p)

if __name__ == "__main__":
    DEST.mkdir(parents=True, exist_ok=True)
    write("tap", 60, tap)
    write("open", 250, whoosh)
    write("menu", 90, menu)
