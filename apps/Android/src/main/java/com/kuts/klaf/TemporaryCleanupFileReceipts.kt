package com.kuts.klaf

import com.kuts.klaf.room.maintenance.TemporaryCleanupReceiptStore
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

// TEMPORARY: remove after user-confirmed cleanup. No Room schema changes.
internal class TemporaryCleanupFileReceipts(private val directory: File) : TemporaryCleanupReceiptStore {
    override fun read(scope: String): String? = receiptFile(scope).takeIf { it.isFile }?.readText(Charsets.UTF_8)

    override fun write(scope: String, receipt: String) {
        check(directory.isDirectory || directory.mkdirs()) { "Unable to create cleanup receipt directory" }
        val target = receiptFile(scope)
        if (target.exists()) {
            check(target.readText(Charsets.UTF_8) == receipt) { "Cleanup receipt already exists" }
            return
        }
        val temporary = File.createTempFile("receipt-", ".tmp", directory)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(receipt.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            check(temporary.renameTo(target)) { "Unable to persist cleanup receipt" }
            check(target.readText(Charsets.UTF_8) == receipt) { "Unable to verify cleanup receipt" }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun receiptFile(scope: String): File {
        val name = MessageDigest.getInstance("SHA-256").digest(scope.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
        return File(directory, "$name.json")
    }
}
