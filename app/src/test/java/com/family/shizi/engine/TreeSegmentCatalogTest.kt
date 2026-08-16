package com.family.shizi.engine

import com.family.shizi.ui.home.tree.TreeSegmentCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeSegmentCatalogTest {
    @Test fun everyVariantHasTenAnchorsInNormalizedSpace() {
        TreeSegmentCatalog.allSpecs().forEach { spec ->
            assertEquals(10, spec.fruitAnchors.size)
            spec.fruitAnchors.forEach {
                assertTrue(it.x in 0f..1f)
                assertTrue(it.y in 0f..1f)
            }
            assertTrue(spec.stageHoleAnchor.x in 0f..1f)
            assertTrue(spec.stageHoleAnchor.y in 0f..1f)
        }
    }

    @Test fun repeatingVariantSequenceHasContinuousConnectors() {
        val specs = TreeSegmentCatalog.allSpecs()
        specs.indices.forEach { index ->
            assertTrue(
                "connector mismatch at $index",
                TreeSegmentCatalog.validateConnection(specs[index], specs[(index + 1) % specs.size]),
            )
        }
    }
}
