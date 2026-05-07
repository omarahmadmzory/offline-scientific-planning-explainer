# Offline Scientific Planning Explainer

**Offline AI explanation for deterministic scientific results — no internet, no server, no open chat.**

A submission for the [Gemma 4 Good Hackathon](https://www.kaggle.com/competitions/gemma-4-good-hackathon) — Future of Education track.

---

## Demo Video

[![Watch the demo](https://img.shields.io/badge/YouTube-Watch%20Demo-red)](https://www.youtube.com/shorts/M7QdLEfytSk)

---

## What This Proves

Scientific calculators produce correct numbers. Users also need trustworthy explanation of
what those numbers mean, what assumptions went into them, and what to verify before relying
on them — especially in low-connectivity environments, education settings, or resource-limited
research contexts.

This demo proves one architectural pattern:

> **Deterministic scientific result first. Offline Gemma 4 explanation second.**
> **The deterministic result is the source of truth.**

A two-proportion sample-size calculation runs locally and deterministically on the device.
Gemma 4 E2B then explains the result, its planning assumptions, and what to verify —
entirely offline, using LiteRT-LM with a GPU backend on Android.

**The AI does not calculate the final answer. It does not override the deterministic result. It explains only.**

---

## Demo Flow

```
1. App opens → deterministic sample-size result is displayed
2. User selects a guided explanation:
   • Explain Simply             — plain-language summary of the result
   • Where Do p1 and p2 Come From? — explains planning estimate sources
   • Explain Study Power        — alpha, beta, and power concepts
   • What Should I Verify?     — safe preparatory checks for the researcher
3. Gemma 4 generates the explanation offline on-device
4. User taps Translate → Arabic → Arabic translation of the current explanation
5. Show English → restores English instantly, no additional model call
```

---

## Why Offline On-Device AI

- No internet connection required after initial model setup.
- No data sent to any server.
- Supports education and research in low-connectivity environments.
- Privacy-preserving: the scientific result and explanation stay on the device.
- Arabic translation runs through the same offline pipeline — no separate service.

This is the kind of AI access that matters for researchers and learners who cannot
rely on cloud connectivity or paid API subscriptions.

---

## Screenshots

<!-- Add 2-3 screenshots here:
     1. Deterministic result with payload values visible
     2. Offline Gemma 4 explanation output
     3. Arabic translation output
-->

---

## Architecture

```
Deterministic Engine (formula)
        │
        ▼
    Result Payload (JSON)
        │
        ▼
  Bounded Prompt (explain only, no recalculate)
        │
        ▼
  Gemma 4 E2B — LiteRT-LM — GPU — Android
        │
        ▼
   Educational Explanation (offline, on-device)
```

The prompts and payload schema are published in [`prompts/`](prompts/) and
[`payloads/`](payloads/) for full transparency and reproducibility.

---

## Technical Stack

| Component | Details |
|-----------|---------|
| AI Model | Gemma 4 E2B Instruct (`gemma-4-E2B-it.litertlm`) |
| Runtime | LiteRT-LM Android |
| Backend | GPU |
| Language | Kotlin (Android) |
| Validated device | Samsung SM-S928B (Galaxy S24 Ultra class), Android 16 |
| Model load time | ~4.5 – 5.3 seconds |
| Generation time | ~6.5 – 18.7 seconds (varies by mode) |
| Network required | None after setup |

---

## Quick Start

### Requirements

- Android device with GPU support (Android 10 or later recommended)
- ~3 GB free internal storage for the model file
- ADB installed (for model placement)

### 1. Get the Model

The Gemma 4 E2B LiteRT model is **not bundled in this repository**.
Follow [MODEL_SETUP.md](MODEL_SETUP.md) to download and place the model file.

### 2. Build the APK

```bash
./gradlew assembleDebug
```

Or open the project in Android Studio and build from there.

### 3. Install and Run

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.gemma4benchmark/.MainActivity
```

### 4. Load and Generate

1. Tap **Load Gemma 4 Model** and wait (~5 seconds on validated device).
2. Tap any guided explanation button.
3. Wait for on-device generation to complete.
4. Optionally tap **Translate → Arabic**.

---

## Localization

| Language | Status |
|----------|--------|
| English | Primary — all guided explanations |
| Arabic | Validated — offline translation of current English output |
| Kurdish (Sorani) | Experimental — visible in picker but disabled; validation ongoing |

---

## Trust and Safety Boundary

- The AI explains deterministic results. It does not calculate, recalculate, or override them.
- This is **not** professional medical, statistical, or clinical advice.
- The demo does not validate study design or clinical sufficiency.
- Planning values (p1, p2) are for demonstration only and must be justified before any real study.
- Domain-specific decisions must be reviewed by qualified supervisors or statisticians.
- See [LIMITATIONS.md](LIMITATIONS.md) for the full list of constraints.

---

## Reproducibility

See [REPRODUCIBILITY.md](REPRODUCIBILITY.md) for step-by-step reproduction instructions,
expected timing, and offline verification procedure.

---

## Model Setup

See [MODEL_SETUP.md](MODEL_SETUP.md) for how to obtain, verify, and place the Gemma model file.

---

## License and Attribution

This demo project code is licensed under the [Apache License 2.0](LICENSE).

Model weights are not included in this repository.
See [NOTICE.md](NOTICE.md) for model license information and third-party attributions.

---

## What This Is Not

- Not a general AI chatbot.
- Not a medical advisor or clinical tool.
- Not a statistical consultant or replacement for a statistician.
- Not a validated study design tool.
- Not a production-ready application.
- Not a broad Android compatibility claim.
- Not an iOS application.

It is a proof-of-concept demonstrating one workflow where deterministic computation
and bounded offline AI explanation work together to support education and research.
