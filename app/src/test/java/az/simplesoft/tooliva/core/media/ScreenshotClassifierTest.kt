package az.simplesoft.tooliva.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotClassifierTest {

    @Test
    fun recognizesScreenshotBucketAndPathNames() {
        assertTrue(ScreenshotClassifier.isScreenshotCandidate("IMG_1.png", "Pictures/Screenshots/", "Screenshots"))
        assertTrue(ScreenshotClassifier.isScreenshotCandidate("screen_shot_1.png", null, null))
    }

    @Test
    fun doesNotLabelOrdinaryPhotoAsScreenshot() {
        assertFalse(ScreenshotClassifier.isScreenshotCandidate("IMG_1.png", "DCIM/Camera/", "Camera"))
    }
}
