package com.family.shizi

import android.app.Application
import com.family.shizi.data.db.ShiziDatabase
import com.family.shizi.data.repository.ShiziRepository
import com.family.shizi.data.settings.ShiziSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShiziApplication : Application() {
    /**
     * true if database could not be opened even after destructive fallback.
     * When true, the app must show parent-recovery UI instead of child flows.
     */
    var databaseOpenFailed: Boolean = false
        private set

    val database: ShiziDatabase? by lazy {
        val db = ShiziDatabase.getInstance(this)
        if (db == null) databaseOpenFailed = true
        db
    }

    val settingsStore: ShiziSettingsStore by lazy { ShiziSettingsStore(this) }

    val repository: ShiziRepository? by lazy {
        val db = database ?: return@lazy null
        ShiziRepository(
            database = db,
            settingsStore = settingsStore,
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Eagerly trigger database initialization so failure is detected early
        val db = database
        if (db != null) {
            appScope.launch {
                runCatching {
                    settingsStore.updateSettings { it.copy(bootCount = it.bootCount + 1) }
                }
            }
        }
    }
}
