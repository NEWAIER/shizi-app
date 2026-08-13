# DEVICE CHECK

Date: 2026-08-12

Exact APK under test:

- Path: `deliverables/shizi-mobile-flow-debug.apk`
- SHA-256: `6695b5a26fbc089eaf7c33e666edb26f74cd4703371245a9467e0b06d89d32c8`
- Size: `16,529,627` bytes

Device identity:

- Device: Huawei Mate 30 standard edition
- ADB serial: `FEC5T19B07013055`
- Model/product: `TAS-AN00` / `TAS_AN00`
- Android platform report: Android 12
- HarmonyOS platform version: `4.2.0`

| Check | Result | Evidence |
|---|---|---|
| ADB online detection | PASS | `docs/08_raw_logs/43_mate30_final_apk_cold_start.log` |
| Exact APK update install | PASS | `adb install --no-streaming -r`; console result `Success` |
| Force-stop then cold start | PASS | PID `21816`, resumed `com.family.shizi/.MainActivity`; `docs/08_raw_logs/43_mate30_final_apk_cold_start.log` |
| Portrait activity launch | PASS | `MainActivity` resumed; Manifest locks portrait orientation |
| Full device automation | PASS, 33/33 | `docs/08_raw_logs/42_am_instrument_mate30_privacy_final_33.log` |
| Audio regression | PASS, 8/8 | Included in the 33-test run; all 38 MP3 assets reached natural completion |
| Room/DataStore regression | PASS, 14/14 | Included in the 33-test run; standalone evidence also in `docs/08_raw_logs/35_am_instrument_mate30_t05_defaults_fix.log` |
| Crash buffer after final cold start | PASS, empty | `docs/09_apk_inspection/FINAL_CRASH_BUFFER_EXACT_APK.txt` |

Manual child/parent walkthroughs not represented by the automated suite remain `NOT_RUN`; this report does not convert them to PASS.
