package com.family.shizi.domain.engine

/**
 * 成长等级纯逻辑：按成长星星数计算当前等级、当前等级起点、
 * 下一等级阈值与等级进度（0..1，满级恒为 1f）。
 * 阈值阶梯与 REWARD_SYSTEM_V1 保持一致：0/50/120/250/450/700。
 */
object HonorLevels {
    data class LevelProgress(
        val level: Int,          // 1 起
        val title: String,
        val currentLevelStart: Int,   // 当前等级起点星星
        val nextLevelThreshold: Int,  // 下一等级阈值星星（满级时等于当前起点）
        val progress: Float,          // 0..1，满级 1f
    )

    private val thresholds = listOf(0, 50, 120, 250, 450, 700)
    private val titles = listOf("字宝宝", "识字小芽", "汉字朋友", "识字探险家", "汉字收藏家", "汉字达人")

    fun progressFor(stars: Int): LevelProgress {
        val index = thresholds.indexOfLast { stars >= it }.coerceAtLeast(0)
        val start = thresholds[index]
        val next = thresholds.getOrNull(index + 1) ?: start
        val progress = if (next > start) {
            ((stars - start).toFloat() / (next - start)).coerceIn(0f, 1f)
        } else {
            1f
        }
        return LevelProgress(
            level = index + 1,
            title = titles[index],
            currentLevelStart = start,
            nextLevelThreshold = next,
            progress = progress,
        )
    }

    /** 距离下一等级还差的星星数（满级为 0）。 */
    fun starsToNext(stars: Int): Int {
        val p = progressFor(stars)
        return (p.nextLevelThreshold - stars).coerceAtLeast(0)
    }
}
