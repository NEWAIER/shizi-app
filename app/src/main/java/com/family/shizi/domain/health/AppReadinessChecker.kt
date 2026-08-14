package com.family.shizi.domain.health

import android.content.Context
import com.family.shizi.data.content.AndroidAssetByteSource
import com.family.shizi.data.content.AssetManifestLoader
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.content.ContentValidator
import com.family.shizi.data.content.G2ResourceValidator

data class AppReadiness(
    val ready: Boolean,
    val message: String,
)

class AppReadinessChecker(private val context: Context) {
    fun check(): AppReadiness {
        val content = ContentLoader.load(context)
        val g1 = ContentValidator.validate(content)
        if (!g1.isValid) {
            return AppReadiness(false, "内容结构校验失败：${g1.errors.first().code}")
        }
        val manifest = AssetManifestLoader.load(context)
        val g2 = G2ResourceValidator.validate(content, manifest, AndroidAssetByteSource(context))
        if (!g2.isValid) {
            val first = g2.errors.first()
            return AppReadiness(false, "资源校验失败：${first.code} · ${first.path}")
        }
        return AppReadiness(true, "OK")
    }
}
