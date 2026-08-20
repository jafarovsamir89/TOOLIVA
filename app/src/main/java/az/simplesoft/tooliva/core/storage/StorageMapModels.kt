package az.simplesoft.tooliva.core.storage

import java.io.File

data class StorageMapNode(
    val path: String,
    val name: String,
    val totalBytes: Long,
    val directFileBytes: Long,
    val fileCount: Long,
    val folderCount: Long,
    val children: List<StorageMapNode>,
) {
    fun percentOf(parent: StorageMapNode?): Int =
        if (parent == null || parent.totalBytes <= 0L) 0 else ((totalBytes.toDouble() / parent.totalBytes) * 100.0).toInt().coerceIn(0, 100)
}

data class StorageMapResult(
    val roots: List<StorageMapNode>,
    val filesChecked: Long,
    val foldersFound: Long,
    val bytesCounted: Long,
    val skippedWarnings: Long,
) {
    fun find(path: String): StorageMapNode? = roots.asSequence().mapNotNull { findIn(it, path) }.firstOrNull()

    private fun findIn(node: StorageMapNode, path: String): StorageMapNode? = when {
        node.path == path -> node
        else -> node.children.asSequence().mapNotNull { findIn(it, path) }.firstOrNull()
    }
}

internal sealed interface StorageMapScanEvent {
    data object Started : StorageMapScanEvent
    data class FileFound(val rootPath: String, val path: String, val sizeBytes: Long) : StorageMapScanEvent
    data class Progress(val filesChecked: Long) : StorageMapScanEvent
    data class Warning(val path: String) : StorageMapScanEvent
    data object Completed : StorageMapScanEvent
}

/** Memory-bounded aggregation helper; it retains folders, never individual file entries. */
class StorageMapAggregator(private val rootNames: Map<String, String>) {
    private data class MutableNode(
        val path: String,
        val name: String,
        var totalBytes: Long = 0L,
        var directFileBytes: Long = 0L,
        var fileCount: Long = 0L,
        val children: LinkedHashMap<String, MutableNode> = linkedMapOf(),
    )

    private val roots = linkedMapOf<String, MutableNode>()
    var filesChecked: Long = 0L
        private set
    var bytesCounted: Long = 0L
        private set
    var skippedWarnings: Long = 0L
        private set

    fun addWarning() { skippedWarnings++ }

    fun addFile(rootPath: String, filePath: String, bytes: Long) {
        val root = roots.getOrPut(rootPath) { MutableNode(rootPath, rootNames[rootPath] ?: File(rootPath).name.ifBlank { "Storage" }) }
        val rootFile = File(rootPath).absoluteFile
        var directory: File? = File(filePath).parentFile?.absoluteFile
        val chain = mutableListOf<MutableNode>()
        while (directory != null && directory.toPath().startsWith(rootFile.toPath())) {
            val currentDirectory = directory
            val node = rootsNode(currentDirectory, root)
            chain += node
            if (currentDirectory == rootFile) break
            directory = currentDirectory.parentFile
        }
        if (chain.none { it.path == root.path }) chain += root
        val safeBytes = bytes.coerceAtLeast(0L)
        filesChecked++
        bytesCounted = safeAdd(bytesCounted, safeBytes)
        chain.forEachIndexed { index, node ->
            node.totalBytes = safeAdd(node.totalBytes, safeBytes)
            node.fileCount++
            if (index == 0) node.directFileBytes = safeAdd(node.directFileBytes, safeBytes)
        }
    }

    fun build(): StorageMapResult {
        val builtRoots = roots.values.map { buildNode(it) }.sortedByDescending(StorageMapNode::totalBytes)
        return StorageMapResult(
            roots = builtRoots,
            filesChecked = filesChecked,
            foldersFound = builtRoots.sumOf { it.folderCount + 1L },
            bytesCounted = bytesCounted,
            skippedWarnings = skippedWarnings,
        )
    }

    private fun rootsNode(directory: File, root: MutableNode): MutableNode {
        if (directory.path == root.path) return root
        val parent = directory.parentFile?.absoluteFile ?: File(root.path)
        val parentNode = rootsNode(parent, root)
        return parentNode.children.getOrPut(directory.path) { MutableNode(directory.path, directory.name.ifBlank { "Storage" }) }
    }

    private fun buildNode(node: MutableNode): StorageMapNode {
        val children = node.children.values.map(::buildNode).sortedByDescending(StorageMapNode::totalBytes)
        return StorageMapNode(
            path = node.path,
            name = node.name,
            totalBytes = node.totalBytes,
            directFileBytes = node.directFileBytes,
            fileCount = node.fileCount,
            folderCount = children.size.toLong() + children.sumOf(StorageMapNode::folderCount),
            children = children,
        )
    }
}

private fun safeAdd(first: Long, second: Long): Long =
    if (second <= 0L) first else if (Long.MAX_VALUE - first < second) Long.MAX_VALUE else first + second
