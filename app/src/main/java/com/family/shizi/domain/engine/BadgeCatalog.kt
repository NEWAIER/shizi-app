package com.family.shizi.domain.engine

/**
 * 徽章目录纯逻辑：按 [BadgeType] 分类判定解锁，杜绝"所有徽章统一按 learnedCount 判定"。
 *
 * 本轮真实启用：
 * - LEARNED_COUNT：字数类 1/5/10/20/30/40/50
 * - LEARNING_DAYS：学习天数类 1/3/7/14
 * - REVIEW_COUNT / CHALLENGE_COUNT / COLLECTION_COUNT：无可靠计数时不伪造解锁，显示"等待点亮"。
 */
enum class BadgeType { LEARNED_COUNT, LEARNING_DAYS, REVIEW_COUNT, CHALLENGE_COUNT, COLLECTION_COUNT }

data class BadgeDefinition(
    val id: String,
    val type: BadgeType,
    val title: String,
    val detail: String,
    val threshold: Int,
    /** 是否已有可靠计数源；无可靠计数时一律不解锁，只显示"等待点亮"。 */
    val hasReliableCount: Boolean = true,
)

/** 徽章解锁所需的各类真实计数。 */
data class BadgeCounts(
    val learnedCount: Int = 0,
    val learningDays: Int = 0,
    val reviewCount: Int? = null,
    val challengeCount: Int? = null,
    val collectionCount: Int? = null,
)

object BadgeCatalog {
    val all: List<BadgeDefinition> = listOf(
        // 字数类（真实启用）
        BadgeDefinition("learned_1", BadgeType.LEARNED_COUNT, "启蒙星", "认识第一个字", 1),
        BadgeDefinition("learned_5", BadgeType.LEARNED_COUNT, "五字小芽", "认识5个字", 5),
        BadgeDefinition("learned_10", BadgeType.LEARNED_COUNT, "十字花园", "认识10个字", 10),
        BadgeDefinition("learned_20", BadgeType.LEARNED_COUNT, "二十字树", "认识20个字", 20),
        BadgeDefinition("learned_30", BadgeType.LEARNED_COUNT, "三十字云朵", "认识30个字", 30),
        BadgeDefinition("learned_40", BadgeType.LEARNED_COUNT, "四十字星球", "认识40个字", 40),
        BadgeDefinition("learned_50", BadgeType.LEARNED_COUNT, "五十字达人", "认识50个字", 50),
        // 学习天数类（真实启用）
        BadgeDefinition("days_1", BadgeType.LEARNING_DAYS, "出发啦", "完成第1天学习", 1),
        BadgeDefinition("days_3", BadgeType.LEARNING_DAYS, "连续三天", "坚持3天", 3),
        BadgeDefinition("days_7", BadgeType.LEARNING_DAYS, "一周相伴", "坚持7天", 7),
        BadgeDefinition("days_14", BadgeType.LEARNING_DAYS, "两周闪耀", "坚持14天", 14),
        // 复习类（暂无可靠计数 → 等待点亮）
        BadgeDefinition("review_1", BadgeType.REVIEW_COUNT, "老朋友你好", "完成第一次复习", 1, hasReliableCount = false),
        BadgeDefinition("review_5", BadgeType.REVIEW_COUNT, "复习小能手", "完成5次复习", 5, hasReliableCount = false),
        // 挑战类（暂无可靠计数 → 等待点亮）
        BadgeDefinition("challenge_1", BadgeType.CHALLENGE_COUNT, "勇敢挑战", "完成第一次树洞闯关", 1, hasReliableCount = false),
        BadgeDefinition("challenge_5", BadgeType.CHALLENGE_COUNT, "挑战之星", "完成5次树洞闯关", 5, hasReliableCount = false),
        // 收藏类（暂无可靠计数 → 等待点亮）
        BadgeDefinition("collect_10", BadgeType.COLLECTION_COUNT, "小小收藏家", "收集10张字卡", 10, hasReliableCount = false),
        BadgeDefinition("collect_25", BadgeType.COLLECTION_COUNT, "字卡花园", "收集25张字卡", 25, hasReliableCount = false),
        BadgeDefinition("collect_50", BadgeType.COLLECTION_COUNT, "汉字收藏家", "收集50张字卡", 50, hasReliableCount = false),
    )

    fun byType(type: BadgeType): List<BadgeDefinition> = all.filter { it.type == type }

    fun isUnlocked(definition: BadgeDefinition, counts: BadgeCounts): Boolean {
        if (!definition.hasReliableCount) return false
        val actual = when (definition.type) {
            BadgeType.LEARNED_COUNT -> counts.learnedCount
            BadgeType.LEARNING_DAYS -> counts.learningDays
            BadgeType.REVIEW_COUNT -> counts.reviewCount ?: return false
            BadgeType.CHALLENGE_COUNT -> counts.challengeCount ?: return false
            BadgeType.COLLECTION_COUNT -> counts.collectionCount ?: return false
        }
        return actual >= definition.threshold
    }

    /** 已解锁徽章 id 列表（按目录顺序）。 */
    fun unlockedIds(counts: BadgeCounts): List<String> =
        all.filter { isUnlocked(it, counts) }.map { it.id }

    /** 缺少计数源（等待点亮）的徽章 id 列表。 */
    fun awaitingIds(): List<String> =
        all.filter { !it.hasReliableCount }.map { it.id }
}
