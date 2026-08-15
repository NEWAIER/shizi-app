package com.family.shizi.domain.engine

/**
 * 树洞测试关卡批次规则：每学满 [BATCH_SIZE]（10）个字解锁一个树洞，
 * 每个树洞测试最近一批 [BATCH_SIZE] 个字。
 *
 * 批次号从 0 开始（0 = 第 1-10 字，1 = 第 11-20 字……），全部按学习顺序推进。
 * 纯逻辑对象，便于单元测试与 UI / Repository 复用。
 */
object StageTestBatches {
    const val BATCH_SIZE = 10

    /** 批次 [batchIndex] 覆盖的 learningOrder 索引区间（左闭右开）。 */
    fun rangeOf(batchIndex: Int): IntRange {
        require(batchIndex >= 0) { "batchIndex must be >= 0" }
        val start = batchIndex * BATCH_SIZE
        return start until start + BATCH_SIZE
    }

    /** 该批次是否已解锁：本批最后一个字（第 (batchIndex+1)*10 个字）已学完。 */
    fun isUnlocked(batchIndex: Int, learnedCount: Int): Boolean =
        learnedCount >= (batchIndex + 1) * BATCH_SIZE

    /** 已解锁的树洞批次号列表（按学习顺序从小到大）。 */
    fun unlockedBatches(learnedCount: Int): List<Int> {
        if (learnedCount < BATCH_SIZE) return emptyList()
        return (0..(learnedCount - 1) / BATCH_SIZE).toList()
    }

    /** 最近一个已解锁且本批全部学完的批次号；未学满一批时为 null。 */
    fun latestUsableBatch(learnedCount: Int): Int? =
        if (learnedCount < BATCH_SIZE) null else (learnedCount - 1) / BATCH_SIZE

    /** 从学习顺序中取出批次 [batchIndex] 的字 id（右边界按学习顺序长度收缩，支持末尾不足一批）。 */
    fun characterIdsOf(learningOrder: List<String>, batchIndex: Int): List<String> {
        val range = rangeOf(batchIndex)
        val start = range.first
        val end = (range.last + 1).coerceAtMost(learningOrder.size)
        return if (start >= learningOrder.size) emptyList() else learningOrder.subList(start, end)
    }
}
