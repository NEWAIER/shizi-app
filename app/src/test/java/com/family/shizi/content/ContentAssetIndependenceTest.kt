package com.family.shizi.content

import com.family.shizi.data.content.ContentValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentAssetIndependenceTest {
    @Test fun g1PassesWithoutAnyReferencedImageOrAudioFiles() {
        // G1 receives only the decoded content model and deliberately has no file source.
        // Physical resources can be absent or corrupt without forming a G1/G2 cycle.
        assertTrue(ContentValidator.validate(TestContent.packageData()).isValid)
    }
}
