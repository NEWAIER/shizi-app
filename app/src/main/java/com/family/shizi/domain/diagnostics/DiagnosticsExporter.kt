package com.family.shizi.domain.diagnostics

import android.content.Context
import com.family.shizi.BuildConfig
import com.family.shizi.data.content.AndroidAssetByteSource
import com.family.shizi.data.content.AssetManifestLoader
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.content.ContentValidator
import com.family.shizi.data.content.G2ResourceValidator
import com.family.shizi.data.db.ShiziDatabase
import com.family.shizi.data.settings.ShiziSettings
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DiagnosticsPayload(
    val schemaVersion: Int,
    val app: AppInfo,
    val database: DatabaseInfo,
    val contentValidation: ContentValidationInfo,
    val session: SessionInfo,
    val lastError: ErrorInfo?,
    val exportedAt: String,
    val bootCount: Int,
)

@Serializable
data class AppInfo(
    val versionName: String,
    val versionCode: Int,
    val applicationId: String,
    val buildType: String,
    val contentVersion: String,
    val contentSchemaVersion: Int,
    val manifestAssetVersion: String,
    val apkSha256: String,
)

@Serializable
data class DatabaseInfo(
    val roomVersion: Int,
    val roomIdentityHash: String,
    val tableCounts: Map<String, Int>,
    val fkIntegrityOk: Boolean,
)

@Serializable
data class ContentValidationInfo(
    val g1ContentOk: Boolean,
    val g2ResourcesOk: Boolean,
    val manifestHashOk: Boolean,
    val mp3CountExpected: Int,
    val mp3CountActual: Int,
    val webpCountExpected: Int,
    val webpCountActual: Int,
    val corruptedAssets: List<String>,
)

@Serializable
data class SessionInfo(
    val currentSessionActive: Boolean,
    val sessionStatus: String,
    val lastSessionEndedAt: String?,
    val lastSuccessfulSaveAt: Long?,
)

@Serializable
data class ErrorInfo(
    val code: String,
    val occurredAt: String,
)

class DiagnosticsExporter(private val database: ShiziDatabase) {
    suspend fun exportJson(
        context: Context,
        settings: ShiziSettings,
        appVersion: String,
        exportedAt: Instant,
    ): String {
        val today = java.time.LocalDate.now()
        val todaySession = database.learningSessionDao().getUsableByDate(today)
        val recentError = database.appErrorLogDao().latest(1).firstOrNull()

        // Database info
        val roomIdentityHash = readRoomIdentityHash()
        val fkIntegrityOk = checkForeignKeyIntegrity()
        val tables = mapOf(
            "character_progress" to database.characterProgressDao().countAll(),
            "learning_session" to database.learningSessionDao().countAll(),
            "session_item" to database.sessionItemDao().countAll(),
            "question_instance" to database.questionInstanceDao().countAll(),
            "practice_attempt" to database.practiceAttemptDao().countAll(),
            "oral_check" to database.oralCheckDao().countAll(),
            "app_error_log" to database.appErrorLogDao().countAll(),
        )

        // Content validation
        val contentValidation = runContentValidation(context)

        // APK hash
        val apkHash = computeApkSha256(context)

        // Manifest version from manifest asset if available
        val manifestVersion = runCatching {
            context.assets.open("content/v1/manifest.json").use { input ->
                val text = input.bufferedReader().readText()
                val versionMatch = Regex(""""version"\s*:\s*"([^"]+)""").find(text)
                versionMatch?.groupValues?.get(1) ?: settings.contentVersion
            }
        }.getOrDefault(settings.contentVersion)

        val payload = DiagnosticsPayload(
            schemaVersion = 1,
            app = AppInfo(
                versionName = appVersion,
                versionCode = BuildConfig.VERSION_CODE,
                applicationId = BuildConfig.APPLICATION_ID,
                buildType = BuildConfig.BUILD_TYPE,
                contentVersion = settings.contentVersion,
                contentSchemaVersion = settings.schemaVersion,
                manifestAssetVersion = manifestVersion,
                apkSha256 = apkHash,
            ),
            database = DatabaseInfo(
                roomVersion = 1,
                roomIdentityHash = roomIdentityHash,
                tableCounts = tables,
                fkIntegrityOk = fkIntegrityOk,
            ),
            contentValidation = contentValidation,
            session = SessionInfo(
                currentSessionActive = todaySession?.status?.name == "ACTIVE",
                sessionStatus = todaySession?.status?.name ?: "NO_ACTIVE_SESSION",
                lastSessionEndedAt = todaySession?.completedAt?.toString(),
                lastSuccessfulSaveAt = settings.lastSuccessfulSaveAt,
            ),
            lastError = recentError?.let { ErrorInfo(it.code, it.occurredAt.toString()) },
            exportedAt = exportedAt.toString(),
            bootCount = settings.bootCount,
        )
        return Json { prettyPrint = true }.encodeToString(payload)
    }

    private fun readRoomIdentityHash(): String {
        return try {
            database.openHelper.writableDatabase.query(
                "SELECT identity_hash FROM room_master_table"
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "unknown"
            }
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun checkForeignKeyIntegrity(): Boolean {
        return try {
            val cursor = database.openHelper.writableDatabase.query("PRAGMA foreign_key_check")
            val ok = cursor.count == 0
            cursor.close()
            ok
        } catch (_: Exception) {
            false
        }
    }

    private fun runContentValidation(context: Context): ContentValidationInfo {
        return try {
            val content = ContentLoader.load(context)
            val g1 = ContentValidator.validate(content)
            val manifest = AssetManifestLoader.load(context)
            val g2 = G2ResourceValidator.validate(content, manifest, AndroidAssetByteSource(context))

            val corruptedAssets = g2.errors.map { it.path }
            val mp3Expected = manifest.resources.count { it.path.startsWith("audio/") && it.path.endsWith(".mp3") }
            val webpExpected = manifest.resources.count { it.path.startsWith("images/") && it.path.endsWith(".webp") }

            // Count actual assets by scanning manifest (actual files validated by G2)
            val mp3Actual = if (g2.isValid) mp3Expected else {
                manifest.resources.count { it.path.startsWith("audio/") && it.path.endsWith(".mp3") && it.path !in corruptedAssets }
            }
            val webpActual = if (g2.isValid) webpExpected else {
                manifest.resources.count { it.path.startsWith("images/") && it.path.endsWith(".webp") && it.path !in corruptedAssets }
            }

            val manifestHash = computeManifestHash(context)
            val manifestJson = context.assets.open("content/v1/manifest.json").use { it.readBytes() }
            val expectedManifestHash = manifest.resources.firstOrNull { it.path == "manifest.json" }?.sha256
            val manifestHashOk = expectedManifestHash == null || manifestHash == expectedManifestHash

            ContentValidationInfo(
                g1ContentOk = g1.isValid,
                g2ResourcesOk = g2.isValid,
                manifestHashOk = manifestHashOk,
                mp3CountExpected = mp3Expected,
                mp3CountActual = mp3Actual,
                webpCountExpected = webpExpected,
                webpCountActual = webpActual,
                corruptedAssets = corruptedAssets,
            )
        } catch (_: Exception) {
            ContentValidationInfo(
                g1ContentOk = false,
                g2ResourcesOk = false,
                manifestHashOk = false,
                mp3CountExpected = 38,
                mp3CountActual = 0,
                webpCountExpected = 12,
                webpCountActual = 0,
                corruptedAssets = listOf("validation_exception"),
            )
        }
    }

    private fun computeManifestHash(context: Context): String {
        return try {
            context.assets.open("content/v1/manifest.json").use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun computeApkSha256(context: Context): String {
        return try {
            val apkFile = File(context.packageCodePath)
            if (!apkFile.exists()) return "not_found"
            val digest = MessageDigest.getInstance("SHA-256")
            apkFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "error"
        }
    }
}
