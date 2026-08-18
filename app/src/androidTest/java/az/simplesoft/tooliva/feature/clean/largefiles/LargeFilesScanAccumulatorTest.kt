package az.simplesoft.tooliva.feature.clean.largefiles

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import az.simplesoft.tooliva.core.media.LargeMediaFile
import az.simplesoft.tooliva.core.storage.StorageCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LargeFilesScanAccumulatorTest {

    @Test
    fun eachEntryIsAvailableBeforeScanCompletes() {
        val accumulator = LargeFilesScanAccumulator()
        val first = file("first.zip", 200L)
        val second = file("second.pdf", 300L)

        assertEquals(listOf(first), accumulator.add(first))
        assertEquals(listOf(first, second), accumulator.add(second))
        assertEquals(listOf(first, second), accumulator.snapshot())
    }

    private fun file(name: String, sizeBytes: Long) = LargeMediaFile(
        uri = Uri.parse("file:///Download/$name"),
        displayName = name,
        sizeBytes = sizeBytes,
        mimeType = null,
        modifiedEpochSeconds = 0L,
        category = StorageCategory.OTHER,
        path = "/Download/$name",
    )
}
