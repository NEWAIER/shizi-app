package com.family.shizi.engine

import com.family.shizi.ui.home.tree.FullTreeAnchorCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullTreeAnchorCatalogTest {
    @Test fun fiftyFruitsAndFiveHolesStayWithinWholeTree() {
        (1..50).forEach { order ->
            val anchor = FullTreeAnchorCatalog.fruitAnchor(order)
            assertTrue(anchor.x in 0f..1f)
            assertTrue(anchor.y in 0f..1f)
        }
        (0..4).forEach { batch ->
            val anchor = FullTreeAnchorCatalog.stageHoleAnchor(batch)
            assertTrue(anchor.x in 0f..1f)
            assertTrue(anchor.y in 0f..1f)
        }
        assertEquals(0.90f, FullTreeAnchorCatalog.fruitAnchor(1).y)
        assertEquals(0.18f, FullTreeAnchorCatalog.fruitAnchor(50).y)
    }
}
