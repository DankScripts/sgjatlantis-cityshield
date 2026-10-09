#!/usr/bin/env python3
"""Generate the original Atlantis City Shield activation sound.

The effect is synthesized from scratch.  It deliberately uses no audio from
Stargate Atlantis or any other copyrighted production.  Its 6.4-second timing
matches the shield's 120-tick materialization and short visual lock-in tail.
"""

from __future__ import annotations

import argparse
import wave
from pathlib import Path

import numpy as np
from scipy.signal import butter, sosfilt


SAMPLE_RATE = 48_000
DURATION = 6.4
FORMATION_END = 6.0
RANDOM_SEED = 0xA71A_715


def smoothstep(value: np.ndarray) -> np.ndarray:
    value = np.clip(value, 0.0, 1.0)
    return value * value * (3.0 - 2.0 * value)


def oscillator(frequency: np.ndarray, phase: float = 0.0) -> np.ndarray:
    angle = phase + 2.0 * np.pi * np.cumsum(frequency) / SAMPLE_RATE
    return np.sin(angle)


def bandpass(signal: np.ndarray, low: float, high: float, order: int = 4) -> np.ndarray:
    nyquist = SAMPLE_RATE * 0.5
    sos = butter(order, [low / nyquist, high / nyquist], btype="bandpass", output="sos")
    return sosfilt(sos, signal)


def highpass(signal: np.ndarray, cutoff: float) -> np.ndarray:
    sos = butter(3, cutoff / (SAMPLE_RATE * 0.5), btype="highpass", output="sos")
    return sosfilt(sos, signal)


def delayed_mix(signal: np.ndarray, delay_seconds: float, gain: float) -> np.ndarray:
    delay = int(delay_seconds * SAMPLE_RATE)
    result = np.zeros_like(signal)
    result[delay:] = signal[:-delay] * gain
    return result


def synthesize() -> np.ndarray:
    count = int(SAMPLE_RATE * DURATION)
    time = np.arange(count, dtype=np.float64) / SAMPLE_RATE
    rng = np.random.default_rng(RANDOM_SEED)
    white = rng.normal(0.0, 1.0, count)

    formation = np.clip(time / FORMATION_END, 0.0, 1.0)
    charge = smoothstep(time / 0.75)
    release = smoothstep((DURATION - time) / 0.38)
    master_envelope = charge * release

    # A low zero-point pressure bed with almost no periodic amplitude motion.
    # Alpha 19's rising resonances and regular tremolo read as a motor-like
    # whir, so this revision deliberately avoids pitch sweeps and rotating
    # modulation.  Filtered noise supplies irregular movement instead.
    generator_motion = bandpass(rng.normal(0.0, 1.0, count), 0.35, 2.2, order=2)
    generator_motion /= max(float(np.max(np.abs(generator_motion))), 1.0e-9)
    low_frequency = 43.0 + 0.8 * generator_motion
    generator = (
        0.100 * oscillator(low_frequency)
        + 0.036 * oscillator(np.full_like(time, 71.0), 0.7)
        + 0.017 * oscillator(np.full_like(time, 113.0), 1.8)
    )
    generator *= master_envelope * (0.90 + 0.10 * generator_motion)

    # Broad energy movement follows the leading edge upward.  Staggered filtered
    # bands replace a flat hiss with a wave that grows brighter as the dome nears
    # its crown.
    low_air = bandpass(white, 120.0, 620.0)
    mid_air = bandpass(white, 500.0, 2_200.0)
    high_air = bandpass(white, 1_800.0, 8_500.0)
    airflow = (
        0.090 * low_air * smoothstep((time - 0.08) / 1.2)
        + 0.080 * mid_air * smoothstep((time - 0.65) / 2.6)
        + 0.053 * high_air * smoothstep((time - 2.0) / 3.5)
    )
    airflow *= release * (0.40 + 0.60 * formation)

    # Successive wavefront surges suggest the huge field propagating across the
    # city.  Their spacing accelerates slightly toward completion.
    wavefronts = np.zeros_like(time)
    pulse_centers = [0.42, 0.96, 1.46, 1.92, 2.35, 2.76, 3.15, 3.53, 3.90, 4.26, 4.61, 4.95, 5.28, 5.59]
    pulse_noise = bandpass(rng.normal(0.0, 1.0, count), 260.0, 4_200.0)
    for index, center in enumerate(pulse_centers):
        width = 0.105 + index * 0.0025
        envelope = np.exp(-0.5 * ((time - center) / width) ** 2)
        pulse_frequency = 310.0 + index * 39.0
        wavefronts += envelope * (
            0.030 * pulse_noise
            + 0.022 * oscillator(np.full_like(time, pulse_frequency), index * 0.63)
        )

    # Sparse non-tonal electrical glints replace the continuous crystalline
    # tremolo.  They add detail to the upper field without creating a whine or
    # a rotating/whirring impression.
    glints = np.zeros_like(time)
    glint_noise = bandpass(rng.normal(0.0, 1.0, count), 2_200.0, 9_500.0)
    for center, width, gain in [
        (2.24, 0.060, 0.020),
        (2.91, 0.045, 0.018),
        (3.47, 0.070, 0.021),
        (4.08, 0.050, 0.019),
        (4.72, 0.075, 0.023),
        (5.31, 0.055, 0.021),
        (5.67, 0.040, 0.018),
    ]:
        glints += gain * glint_noise * np.exp(-0.5 * ((time - center) / width) ** 2)

    # At six seconds the dome locks: a restrained low impact, a short cyan-white
    # harmonic flare, and a breath of broadband energy.  This lands on the same
    # moment the visual front reaches the top center.
    lock_time = 5.90
    after_lock = np.maximum(0.0, time - lock_time)
    lock_gate = (time >= lock_time).astype(np.float64)
    lock_decay = lock_gate * np.exp(-after_lock * 7.2)
    lock_pitch = 82.0 * np.exp(-after_lock * 2.8) + 34.0
    lock_sub = 0.165 * oscillator(lock_pitch, 0.1) * lock_decay
    lock_tone = lock_decay * (
        0.060 * oscillator(np.full_like(time, 612.0), 0.5)
        + 0.042 * oscillator(np.full_like(time, 927.0), 1.7)
        + 0.025 * oscillator(np.full_like(time, 1_389.0), 2.4)
    )
    lock_noise = 0.080 * bandpass(rng.normal(0.0, 1.0, count), 380.0, 7_200.0) * lock_decay

    dry = generator + airflow + wavefronts + glints + lock_sub + lock_tone + lock_noise

    # A short multi-tap chamber makes the cue feel city-sized while retaining a
    # precise formation-front rhythm.  No external impulse response is used.
    wet = dry.copy()
    for delay, gain in [(0.057, 0.19), (0.113, 0.14), (0.191, 0.10), (0.307, 0.075), (0.463, 0.050)]:
        wet += delayed_mix(dry, delay, gain)

    wet = highpass(wet, 24.0)
    wet = np.tanh(wet * 1.65) / np.tanh(1.65)

    # Leave final loudness matching to ffmpeg's EBU R128 pass.  This guard only
    # ensures the intermediate WAV cannot clip.
    peak = float(np.max(np.abs(wet)))
    if peak > 0.0:
        wet *= 0.92 / peak
    return wet.astype(np.float32)


def write_wav(path: Path, signal: np.ndarray) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = np.clip(signal, -1.0, 1.0)
    pcm = np.round(pcm * 32_767.0).astype("<i2")
    with wave.open(str(path), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(pcm.tobytes())


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=Path, help="Destination mono 48 kHz WAV")
    arguments = parser.parse_args()
    write_wav(arguments.output, synthesize())


if __name__ == "__main__":
    main()
