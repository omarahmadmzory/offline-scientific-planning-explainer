# Prompts

This directory contains the prompts used by the Offline Scientific Planning Explainer demo.
They are published here for transparency, reproducibility, and hackathon review.

All prompts follow the same pattern:
1. A shared system prompt with the deterministic payload and AI boundary rules.
2. A mode-specific instruction appended to the system prompt.

The AI is instructed to explain only — it may not recalculate, override, or invent facts.

## Files

| File | Mode | Description |
|------|------|-------------|
| `system_prompt.txt` | Shared | System context + JSON payload + AI boundary rules |
| `explain_simply.txt` | SIMPLE | Beginner-friendly explanation |
| `p1p2_sources.txt` | P1P2_SOURCES | Explains planning estimate sources |
| `study_power.txt` | STUDY_POWER | Explains alpha, beta, statistical power |
| `verify.txt` | VERIFY | Safe preparatory verification checks |
| `arabic_translation.txt` | ARABIC | Translates current English output to Arabic |
