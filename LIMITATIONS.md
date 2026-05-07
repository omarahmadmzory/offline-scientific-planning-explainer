# Known Limitations

This document lists the known limitations of the Offline Scientific Planning Explainer demo.
Read this before drawing conclusions about the demo's scope or capabilities.

---

## Device Compatibility

- **Validated on a single device only:** Samsung SM-S928B (Galaxy S24 Ultra class), Android 16.
- No broad Android device compatibility has been tested or claimed.
- GPU backend requires a device with compatible GPU hardware.
- Performance (load time, generation time) will vary by device.
- Not available on iOS.

---

## Model and Inference

- Model file (`gemma-4-E2B-it.litertlm`) is approximately 2.41 GB and must be placed
  manually in app internal storage. It is not bundled in the APK or repository.
- First model load takes 4–6 seconds on the validated device; cold starts on other
  devices may differ significantly.
- Generation time ranges from ~6 seconds (short explanations) to ~19 seconds
  (detailed verification checklist) on the validated device.
- The model runs on the GPU backend. If GPU is unavailable, a CPU fallback may be
  needed but has not been tested.

---

## AI Explanation Quality

- Gemma 4 E2B is used for explanation only. It does not calculate or validate the
  deterministic result.
- Explanation quality depends on the model's understanding of the provided context.
  Outputs should be treated as educational guidance, not authoritative advice.
- The model may occasionally produce imprecise or incomplete explanations.
  Always verify domain-specific interpretations with qualified experts.

---

## Arabic Translation

- Arabic translation is implemented and functional on the validated device.
- Translation quality has not been formally benchmarked against professional
  Arabic translation standards.
- The translated output preserves numeric values and does not add new facts,
  but translation accuracy for scientific terminology may vary.

---

## Kurdish (Experimental)

- Kurdish (Sorani) translation is visible in the language picker but disabled.
- Testing revealed that the current model produces repetitive output for Kurdish
  and is not suitable for demonstration or educational use in its current form.
- Kurdish support is included as a future community direction, not a current claim.
- Do not interpret the presence of the Kurdish option as validation of Kurdish quality.

---

## Scientific and Statistical Scope

- The demo uses a single workflow: two-proportion sample-size calculation.
- p1 and p2 are planning estimates for demonstration only. They are not from a specific
  validated clinical study and must be justified before any real study.
- The deterministic result does not include dropout/non-response adjustment.
- This demo does not validate study design, sampling assumptions, or clinical conclusions.

---

## Not Medical or Professional Advice

- This tool is not a substitute for professional medical, statistical, or research advice.
- It is designed for educational purposes: helping learners and researchers understand
  deterministic sample-size planning outputs and prepare better questions for their
  supervisors or statisticians.
- Do not use the output to make clinical decisions, design clinical trials, or claim
  study validity without independent expert review.

---

## Infrastructure and Deployment

- This is a one-device hackathon demo, not a production-ready application.
- No cloud API, backend server, or external service is used.
- There is no user authentication, data persistence, or crash reporting.
- The demo has not undergone a security audit or privacy review.
- The APK is a debug build and should not be distributed as a production application.
