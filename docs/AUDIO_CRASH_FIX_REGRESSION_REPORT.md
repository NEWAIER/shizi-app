# AUDIO CRASH FIX REGRESSION REPORT

Date: 2026-08-12

## Exact tested APK

- APK: `deliverables/shizi-mobile-flow-debug.apk`
- Size: `16,529,627` bytes
- SHA-256: `6695b5a26fbc089eaf7c33e666edb26f74cd4703371245a9467e0b06d89d32c8`
- Device: Huawei Mate 30 `TAS-AN00`, HarmonyOS `4.2.0`

## Implementation

`AssetAudioPlayer` now uses Media3 ExoPlayer for bundled `asset:///content/v1/...` MP3 files. It gates callbacks with a playback token, releases only the current player, cancels stale sequences, handles player errors, rejects non-MP3 paths and supports idempotent stop/release. UI callers route failures to repository diagnostics. Media3's unnecessary `ACCESS_NETWORK_STATE` manifest contribution is explicitly removed because this app only plays bundled assets.

## Mate 30 results

Result: `PASS`

The exact APK above completed the full 33-test device suite in 102.277 seconds. The eight audio tests passed:

1. All 38 packaged MP3 assets reached natural completion.
2. One-character A/B/C teaching sequence completed.
3. Multi-character A/B/C sequence completed.
4. Rapid repeated play allowed only the latest completion callback.
5. Stop during playback cancelled without crash or false completion.
6. Missing MP3 reported a recoverable error.
7. Non-audio asset path was rejected.
8. Repeated stop/release remained idempotent.

Evidence:

- Full run: `docs/08_raw_logs/42_am_instrument_mate30_privacy_final_33.log`
- Earlier focused 8/8 run: `docs/08_raw_logs/32_am_instrument_mate30_audio_media3_final.log`
- Final cold start: `docs/08_raw_logs/43_mate30_final_apk_cold_start.log`
- Empty crash buffer: `docs/09_apk_inspection/FINAL_CRASH_BUFFER_EXACT_APK.txt`
- Final permissions: `docs/09_apk_inspection/AAPT_PERMISSIONS_FINAL.txt`

No `FATAL EXCEPTION`, native `D`-state hang, playback-completion process exit or application crash occurred in the final run.
