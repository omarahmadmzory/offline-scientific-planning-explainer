# Model Setup

The Gemma 4 E2B Instruct model file is **not included in this repository**.
You must download it separately and place it in the correct location before running the demo.

---

## Model Details

| Field | Value |
|-------|-------|
| Artifact name | `gemma-4-E2B-it.litertlm` |
| File size | 2,588,147,712 bytes (~2.41 GB) |
| SHA-256 | `181938105E0EEFD105961417E8DA75903EACDA102C4FCE9CE90F50B97139A63C` |
| Format | LiteRT-LM |
| Source repository | [litert-community/gemma-4-E2B-it-litert-lm](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) |
| License shown by source | Apache 2.0 |
| Runtime | LiteRT-LM Android |
| Backend | GPU |

---

## License and Terms

Before downloading the model, review the applicable license and usage terms on the
Hugging Face model page and Google's Gemma Terms of Use. The license shown by the
source repository at time of development was Apache 2.0, but you are responsible for
verifying the current terms before use.

Model weights are subject to the terms of the model provider, not the Apache 2.0
license of this demo project code.

---

## Download

Download the model file from:

```
https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
```

Direct file URL (verify availability and terms before use):

```
https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm
```

---

## Verify Checksum

After downloading, verify the SHA-256 checksum before placing the file on device.

**On Windows (PowerShell):**
```powershell
Get-FileHash gemma-4-E2B-it.litertlm -Algorithm SHA256
```

**On Linux/macOS:**
```bash
sha256sum gemma-4-E2B-it.litertlm
```

Expected:
```
181938105E0EEFD105961417E8DA75903EACDA102C4FCE9CE90F50B97139A63C
```

---

## Place Model on Device

The app expects the model file at this path in app internal storage:

```
/data/user/0/com.example.gemma4benchmark/files/gemma4_litert_benchmark/gemma-4-E2B-it.litertlm
```

### Option A — ADB Push (recommended for sideloading)

1. Push to external storage first:

```bash
adb push gemma-4-E2B-it.litertlm /sdcard/Download/gemma-4-E2B-it.litertlm
```

2. Then use a file manager app on the device to copy it to the app's internal storage,
   or use adb shell:

```bash
adb shell mkdir -p /sdcard/Download/gemma4_litert_benchmark
adb push gemma-4-E2B-it.litertlm /sdcard/Download/gemma4_litert_benchmark/gemma-4-E2B-it.litertlm
```

3. The app includes internal logic to copy the model from external to internal storage
   on first launch if the file is found at the expected external path.

### Option B — Direct to app internal storage (requires root or device debugging)

If you have access to app internal storage via ADB:

```bash
adb shell run-as com.example.gemma4benchmark mkdir -p files/gemma4_litert_benchmark
adb push gemma-4-E2B-it.litertlm /data/data/com.example.gemma4benchmark/files/gemma4_litert_benchmark/gemma-4-E2B-it.litertlm
```

---

## Verify On-Device Checksum

After placement, verify the file on the device:

```bash
adb shell run-as com.example.gemma4benchmark \
  sha256sum files/gemma4_litert_benchmark/gemma-4-E2B-it.litertlm
```

Expected output matches:
```
181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c
```

---

## Expected Performance (Validated Device)

Measured on Samsung SM-S928B (Galaxy S24 Ultra class), Android 16, GPU backend:

| Event | Measured time |
|-------|--------------|
| First model load | 4,468 – 5,252 ms |
| Explain Simply | ~7.5 – 8 s |
| Where Do p1 and p2 Come From? | ~6.5 s |
| Explain Study Power | ~8 – 9.5 s |
| What Should I Verify? | ~18.7 s |
| Translate to Arabic | ~10 – 11 s |
| Show English (restore) | Instant — no model call |

Performance on other devices may differ significantly.

---

## Troubleshooting

| Problem | Likely cause | Resolution |
|---------|-------------|------------|
| "Model path is not readable" | File not placed at correct path | Verify path and permissions |
| Load takes very long or fails | Insufficient GPU memory | Try closing other apps |
| Checksum mismatch | Corrupted download | Re-download and verify |
| Out of memory during generation | Device has insufficient RAM | Not supported on low-RAM devices |
| File not found after ADB push | Wrong destination path | Check exact path shown above |
