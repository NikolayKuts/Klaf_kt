package com.kuts.klaf

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

private fun withTemporaryReceiptDirectory(block: (java.io.File) -> Unit) {
    val root = Files.createTempDirectory("klaf-cleanup-receipts-").toRealPath()
    try {
        block(root.toFile())
    } finally {
        check(root.parent == java.nio.file.Path.of(System.getProperty("java.io.tmpdir")).toRealPath())
        check(root.fileName.toString().startsWith("klaf-cleanup-receipts-"))
        root.toFile().deleteRecursively()
    }
}

class TemporaryCleanupFileReceiptsTest {
    @Test
    fun completionSurvivesCreatingAnotherStoreInstance() = withTemporaryReceiptDirectory { directory ->
        TemporaryCleanupFileReceipts(directory).write("account:a@example.test", "completed")
        assertEquals("completed", TemporaryCleanupFileReceipts(directory).read("account:a@example.test"))
    }

    @Test
    fun databasesHaveIndependentCompletionRecords() = withTemporaryReceiptDirectory { directory ->
        val store = TemporaryCleanupFileReceipts(directory)
        store.write("guest", "guest completed")
        store.write("account:a@example.test", "account completed")
        assertEquals("guest completed", store.read("guest"))
        assertEquals("account completed", store.read("account:a@example.test"))
    }

    @Test
    fun failedWriteDoesNotReportCompletion() = withTemporaryReceiptDirectory { directory ->
        val notDirectory = directory.resolve("not-a-directory").apply { writeText("preserve") }
        val store = TemporaryCleanupFileReceipts(notDirectory)
        assertThrows(IllegalStateException::class.java) { store.write("guest", "completed") }
        assertNull(store.read("guest"))
        assertEquals("preserve", notDirectory.readText())
    }
}
