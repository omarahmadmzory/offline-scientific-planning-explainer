# Reproducibility

This document describes how to reproduce the demo shown in the hackathon video.

---

## Validated Environment

| Field | Value |
|-------|-------|
| Device | Samsung SM-S928B (Galaxy S24 Ultra class) |
| Android version | 16 |
| Chip architecture | ARM64 |
| Backend | GPU (LiteRT-LM Android GPU backend) |
| Model | `gemma-4-E2B-it.litertlm` |
| Model SHA-256 | `181938105E0EEFD105961417E8DA75903EACDA102C4FCE9CE90F50B97139A63C` |
| APK type | Debug build |
| Network state during demo | Airplane mode / offline |

---

## Step-by-Step Reproduction

### 1. Build the APK

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Set Up the Model

Follow [MODEL_SETUP.md](MODEL_SETUP.md) completely, including:
- Downloading the model
- Verifying the SHA-256 checksum
- Placing the file in the correct app internal storage path

### 3. Install the APK

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Enable Offline Mode

For authentic reproduction matching the demo:
1. Enable airplane mode on the device.
2. Confirm Wi-Fi and mobile data are both disabled.

The app does not require network access. All inference runs locally.

### 5. Launch the App

```bash
adb shell am start -n com.example.gemma4benchmark/.MainActivity
```

Or tap the "Offline Scientific Planning Explainer" icon on the device.

### 6. Load the Model

Tap **Load Gemma 4 Model**. Wait for the model to load (~5 seconds on the validated device).
The Model Status card will update to show "Loaded in X ms".

### 7. Generate an Explanation

Tap any guided button. Expected behavior:

| Button | Expected output first section | Approx. time |
|--------|-------------------------------|-------------|
| Explain Simply | "What Was Calculated" | ~8 s |
| Where Do p1 and p2 Come From? | "What Are p1 and p2?" | ~7 s |
| Explain Study Power | "Alpha — Type I Error" | ~9 s |
| What Should I Verify? | "Before Using This Estimate" | ~19 s |

### 8. Test Arabic Translation

After any English explanation is generated:
1. Tap **Translate**.
2. Select **Arabic** from the picker.
3. Wait ~10 seconds for offline translation.
4. Tap **Show English** to restore English output instantly.

---

## Deterministic Payload

The payload used for all explanations is fixed and visible in the app UI:

```
Tool: Two-Proportion Sample Size
p1 (planning estimate): 0.167
p2 (planning target):   0.08
alpha:                  0.05
power:                  0.80

Deterministic output:
Sample size per group:  221
Total sample size:      442
```

The full JSON payload is in [`payloads/two_proportion_sample_size.json`](payloads/two_proportion_sample_size.json).

The prompts sent to Gemma 4 for each mode are in [`prompts/`](prompts/).

---

## Offline Verification

To confirm the app runs without network access:

1. Enable airplane mode before launching.
2. The app will still load the model and generate explanations.
3. Verify by checking: Settings → Wi-Fi (off) and Settings → Mobile Data (off).
4. The generation time and output quality should be identical to online state.

---

## What Counts as Successful Reproduction

- Model loads within 10 seconds on a comparable device.
- Explanations generate and display within expected time ranges.
- Arabic translation runs and returns Arabic text without network access.
- No crash or out-of-memory error during any guided button interaction.
- The deterministic values (p1=0.167, p2=0.08, n=221, total=442) are visible in the
  payload card and referenced correctly in generated explanations.

---

## Known Reproduction Constraints

- Exact timing will vary by device.
- GPU backend is required for acceptable performance.
- The model file must be placed manually; it cannot be downloaded from within the app.
- Reproduction on devices other than the validated SM-S928B is not guaranteed.
