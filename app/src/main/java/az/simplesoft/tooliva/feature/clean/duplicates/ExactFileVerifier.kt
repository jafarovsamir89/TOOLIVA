package az.simplesoft.tooliva.feature.clean.duplicates

import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

object ExactFileVerifier {
    suspend fun verify(reference: File, candidate: File): Boolean {
        val beforeReference = snapshot(reference) ?: return false
        val beforeCandidate = snapshot(candidate) ?: return false
        if (beforeReference.size != beforeCandidate.size) return false
        return try {
            FileInputStream(reference).use { left ->
                FileInputStream(candidate).use { right ->
                    val leftBuffer = ByteArray(BUFFER_SIZE)
                    val rightBuffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val leftRead = left.read(leftBuffer)
                        val rightRead = right.read(rightBuffer)
                        if (leftRead != rightRead) return false
                        if (leftRead < 0) break
                        for (index in 0 until leftRead) {
                            if (leftBuffer[index] != rightBuffer[index]) return false
                        }
                    }
                }
            }
            val afterReference = snapshot(reference)
            val afterCandidate = snapshot(candidate)
            afterReference == beforeReference && afterCandidate == beforeCandidate
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: java.io.IOException) {
            false
        }
    }

    private data class Snapshot(val size: Long, val modifiedAt: Long)

    private fun snapshot(file: File): Snapshot? =
        if (file.isFile) Snapshot(file.length(), file.lastModified()) else null

    private const val BUFFER_SIZE = 64 * 1024
}
