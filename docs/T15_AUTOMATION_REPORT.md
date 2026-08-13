# T15 AUTOMATION REPORT

Result: PARTIAL.

Commands:
- ./gradlew --version: PASS
- ./gradlew clean testDebugUnitTest: PASS
- ./gradlew verifyContentG1: PASS
- ./gradlew verifyContentG2: PASS
- ./gradlew lintDebug: PASS
- ./gradlew connectedDebugAndroidTest: FAIL/NOT_RUN reason: no connected devices
- ./gradlew assembleDebug: PASS

APK SHA-256: be3dfa9c83449672fcb81ec415e8c371ebd248f678b7e0f2e98db3392e7c37e2
APK size: 13815948 bytes
