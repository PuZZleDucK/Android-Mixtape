#!/usr/bin/env python3
"""Measure on-phone reel rotation from the actual screen recordings, clockwise positive."""
from pathlib import Path
import json
import cv2
import numpy as np

ROOT = Path(__file__).resolve().parent
cx, cy = 92 + 174 / 560 * 734, 221 + 190 / 356 * 467
theta = np.arange(720, dtype=np.float32) * (2 * np.pi / 720)
r = np.linspace(14, 34, 21, dtype=np.float32)[:, None]
mx = (cx + r * np.cos(theta)).astype(np.float32)
my = (cy + r * np.sin(theta)).astype(np.float32)
shifts = np.arange(-60, 61)


def polar(frame):
    grey = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY).astype(np.float32)
    band = cv2.remap(grey, mx, my, cv2.INTER_LINEAR)
    return band - band.mean(axis=1, keepdims=True)


def measure(name):
    capture = cv2.VideoCapture(str(ROOT / f"reels-{name}.mp4"))
    previous = None
    speeds = []
    saved = False
    while True:
        ok, frame = capture.read()
        if not ok:
            break
        t = capture.get(cv2.CAP_PROP_POS_MSEC) / 1000
        if .9 <= t <= 3.5:
            if not saved:
                cv2.imwrite(str(ROOT / f"reels-{name}-frame.png"), frame)
                saved = True
            band = polar(frame)
            if previous is not None:
                before_t, before_band = previous
                dt = t - before_t
                if .004 < dt < .055:
                    errors = np.array([np.mean((np.roll(before_band, int(s), axis=1) - band) ** 2) for s in shifts])
                    best = int(errors.argmin())
                    shift = float(shifts[best])
                    if 0 < best < len(shifts) - 1:
                        a, b, c = errors[best - 1:best + 2]
                        denominator = a - 2 * b + c
                        if denominator > 0:
                            shift += float(.5 * (a - c) / denominator)
                        speeds.append(shift * .5 / dt)
            previous = t, band
    capture.release()
    assert len(speeds) >= 20, (name, len(speeds))
    return {"median_degrees_per_second": round(float(np.median(speeds)), 2), "frame_pairs": len(speeds)}


result = {name: measure(name) for name in ["play", "forward", "rewind"]}
play, forward, reverse = [result[name]["median_degrees_per_second"] for name in ["play", "forward", "rewind"]]
assert play < -40 and forward < -300 and reverse > 300, result
assert 4.5 < forward / play < 7.5 and 4.5 < -reverse / play < 7.5, result
result["forward_to_play_ratio"] = round(forward / play, 2)
result["rewind_to_play_ratio"] = round(-reverse / play, 2)
(ROOT / "reel-motion-checks.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps(result, indent=2))
