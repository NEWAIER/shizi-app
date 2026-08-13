# AT-01—AT-43 MATRIX

Date: 2026-08-12

Current APK SHA-256: `6695b5a26fbc089eaf7c33e666edb26f74cd4703371245a9467e0b06d89d32c8`

Allowed values: `PASS`, `FAIL`, `NOT_RUN`. Automated evidence is not used to claim manual visual checks.

| AT | Result | Evidence / reason |
|---|---|---|
| AT-01 | PASS | Device navigation smoke test displays Home without debug routes. |
| AT-02 | NOT_RUN | Manual adult-gate hold timing not captured. |
| AT-03 | NOT_RUN | Full Home-to-session UI walkthrough not captured. |
| AT-04 | NOT_RUN | Five practice components not manually walked through in this run. |
| AT-05 | NOT_RUN | Image-option visual evidence not captured. |
| AT-06 | NOT_RUN | Listen-and-choose UI not manually captured. |
| AT-07 | NOT_RUN | Character-and-audio UI not manually captured. |
| AT-08 | NOT_RUN | First wrong-answer visual feedback not manually captured. |
| AT-09 | NOT_RUN | Second wrong-answer visual feedback not manually captured. |
| AT-10 | PASS | `accidentalTapDoesNotCreateAttemptOrAdvanceAttemptNumber`. |
| AT-11 | PASS | Duplicate-attempt and completed-question idempotency tests pass. |
| AT-12 | PASS | `at12FirstLearningMovesThroughRealEntryPoints`. |
| AT-13 | NOT_RUN | A/B/C visual teaching flow not manually captured; audio sequence is separately PASS. |
| AT-14 | PASS | Session cursor advancement is covered by the idempotency/cursor device test. |
| AT-15 | PASS | `at15DailyTaskPriorityIsDueReviewThenUnfinishedNewThenFreshNew`. |
| AT-16 | NOT_RUN | Complete D1/D3/D7/D14 schedule trace not emitted in this run. |
| AT-17 | NOT_RUN | D14 plus oral recheck UI path not manually captured. |
| AT-18 | PASS | Premature session completion is rejected by device test. |
| AT-19 | NOT_RUN | Explicit process-kill-at-transaction-boundary injection not run. |
| AT-20 | PASS | Room schema, cascade and reopen persistence device tests pass. |
| AT-21 | PASS | DataStore defaults, validation and reset tests pass. |
| AT-22 | PASS | G1/G2 resource validation remains covered by local tests and packaged assets. |
| AT-23 | PASS | APK asset set is present and the exact APK is hash-bound. |
| AT-24 | PASS | All 38 packaged MP3 assets reach natural completion on Mate 30. |
| AT-25 | PASS | Exact APK size, SHA-256 and signing certificate are recorded. |
| AT-26 | NOT_RUN | Adult-gate manual proof not captured. |
| AT-27 | NOT_RUN | Parent tabs not manually walked through. |
| AT-28 | PASS | AAPT shows no sensitive permission. |
| AT-29 | PASS | Backup and cleartext flags remain locked in Manifest source. |
| AT-30 | PASS | Exact APK completed 33/33 instrumented tests on TAS-AN00. |
| AT-31 | PASS | Exact APK installed, force-stopped and cold-started; MainActivity resumed. |
| AT-32 | NOT_RUN | Multi-touch physical gesture proof not captured; accidental-tap logic test is PASS under AT-10. |
| AT-33 | PASS | Diagnostics whitelist/export device test passes. |
| AT-34 | PASS | No INTERNET, network-state, recording, camera, location, contacts or storage permission. |
| AT-35 | PASS | Fatigue and time-limit persistence/finish-after-current-question tests pass. |
| AT-36 | PASS | `at36FiveCharactersReachStableMasteredFromOneEmptyDatabase`. |
| AT-37 | NOT_RUN | Parent report visual walkthrough not captured. |
| AT-38 | NOT_RUN | Error-prone page visual walkthrough not captured. |
| AT-39 | NOT_RUN | Oral check/revision UI walkthrough not captured. |
| AT-40 | PASS | Single failure date remains stable; second date rolls back. |
| AT-41 | PASS | Oral PASS revision to FAIL rolls back with anchor. |
| AT-42 | NOT_RUN | Safe repository clear passes, but two-confirm UI path was not manually captured. |
| AT-43 | NOT_RUN | Startup corruption/readiness fault injection not run in this exact device session. |

Primary raw evidence: `docs/08_raw_logs/42_am_instrument_mate30_privacy_final_33.log` and `docs/08_raw_logs/43_mate30_final_apk_cold_start.log`.
