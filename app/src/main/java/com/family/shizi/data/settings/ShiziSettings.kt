package com.family.shizi.data.settings

data class ShiziSettings(
    val schemaVersion: Int = 2,
    // First-run parent setup was removed: children can begin immediately.
    val onboardingCompleted: Boolean = true,
    val nickname: String = "",
    val avatarId: String = "bear",
    val dailyNewCharacterCount: Int = 3,
    val sessionLimitMinutes: Int = 10,
    val volumePercent: Int = 80,
    val isMuted: Boolean = false,
    val lastKnownLocalDate: String? = null,
    val lastSuccessfulSaveAt: Long? = null,
    val contentVersion: String = "1.0.0",
    val bootCount: Int = 0,
)
