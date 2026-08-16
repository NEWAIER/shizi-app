package com.family.shizi.domain.engine

/** Logical segments for a tree that can grow beyond the current content pack. */
object InfiniteGrowthTreeModel {
    const val ENTRIES_PER_SEGMENT = 11

    data class TreeSegment(
        val segmentIndex: Int,
        val firstEntryIndex: Int,
        val lastEntryIndex: Int,
        val stageTestIndex: Int?,
        val visualSeed: Int,
    )

    fun segments(totalEntries: Int): List<TreeSegment> =
        (0 until totalEntries step ENTRIES_PER_SEGMENT).mapIndexed { segmentIndex, start ->
            val end = (start + ENTRIES_PER_SEGMENT - 1).coerceAtMost(totalEntries - 1)
            TreeSegment(
                segmentIndex = segmentIndex,
                firstEntryIndex = start,
                lastEntryIndex = end,
                stageTestIndex = (start..end).firstOrNull { it % ENTRIES_PER_SEGMENT == ENTRIES_PER_SEGMENT - 1 }
                    ?.let { it / ENTRIES_PER_SEGMENT },
                visualSeed = segmentIndex,
            )
        }

    fun visibleRange(currentEntryIndex: Int, totalEntries: Int, buffer: Int = 1): IntRange {
        val segmentCount = segments(totalEntries).size
        if (segmentCount == 0) return IntRange.EMPTY
        val currentSegment = (currentEntryIndex / ENTRIES_PER_SEGMENT).coerceIn(0, segmentCount - 1)
        return (currentSegment - buffer).coerceAtLeast(0)..
            (currentSegment + buffer).coerceAtMost(segmentCount - 1)
    }
}
