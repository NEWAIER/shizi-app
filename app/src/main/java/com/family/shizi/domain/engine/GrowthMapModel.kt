package com.family.shizi.domain.engine

/**
 * 成长森林地图纯逻辑：每 10 个字为一章（一个森林），50 字共 5 章。
 * 章节名、章节范围、节点状态全部在这里计算，便于单元测试与 UI 复用。
 */
object GrowthMapModel {
    const val CHAPTER_SIZE = 10

    val chapterNames = listOf("第一森林", "第二森林", "第三森林", "第四森林", "第五星光森林")

    data class Chapter(
        val index: Int,
        /** 章节起始序号（1 起）。 */
        val startNumber: Int,
        /** 章节结束序号（含）。 */
        val endNumber: Int,
        val name: String,
    )

    enum class NodeState { COMPLETED, CURRENT, UPCOMING, LOCKED }

    /** 总字数对应章节数（末尾不足一批也占一章，最少 0）。 */
    fun chapterCount(totalCharacters: Int): Int =
        if (totalCharacters <= 0) 0 else (totalCharacters + CHAPTER_SIZE - 1) / CHAPTER_SIZE

    /** 节点序号（1 起）所在章节。 */
    fun chapterFor(characterNumber: Int): Int = (characterNumber - 1) / CHAPTER_SIZE

    fun chapterName(index: Int): String =
        chapterNames.getOrElse(index) { "第${index + 1}森林" }

    fun chapters(totalCharacters: Int): List<Chapter> =
        (0 until chapterCount(totalCharacters)).map { index ->
            val start = index * CHAPTER_SIZE + 1
            val end = minOf(start + CHAPTER_SIZE - 1, totalCharacters)
            Chapter(index, start, end, chapterName(index))
        }

    /** 当前章节：包含下一个待学节点（learnedCount + 1）的章节。 */
    fun currentChapterIndex(learnedCount: Int, totalCharacters: Int): Int =
        if (learnedCount >= totalCharacters) chapterCount(totalCharacters) - 1
        else chapterFor(learnedCount + 1)

    /**
     * 节点状态：
     * - COMPLETED：已学会（果子已被吃掉）
     * - CURRENT：下一个要学的字（当前节点）
     * - UPCOMING：今日目标内可学的字
     * - LOCKED：未解锁或超出课程范围
     */
    fun nodeState(number: Int, learnedCount: Int, dailyTarget: Int, totalCharacters: Int = Int.MAX_VALUE): NodeState = when {
        number <= 0 || number > totalCharacters -> NodeState.LOCKED
        number <= learnedCount -> NodeState.COMPLETED
        number == learnedCount + 1 -> NodeState.CURRENT
        number <= learnedCount + dailyTarget -> NodeState.UPCOMING
        else -> NodeState.LOCKED
    }

    /** 该节点所在章节是否已整批解锁（树洞条件）。 */
    fun chapterUnlocked(chapterIndex: Int, learnedCount: Int): Boolean =
        learnedCount >= (chapterIndex + 1) * CHAPTER_SIZE
}
