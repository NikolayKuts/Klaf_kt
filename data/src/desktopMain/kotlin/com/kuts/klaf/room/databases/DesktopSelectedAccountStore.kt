package com.kuts.klaf.room.databases

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

private const val SELECTED_ACCOUNT_FILE_NAME = "selected-account.txt"
private const val TEMP_ACCOUNT_FILE_NAME = "selected-account.tmp"

class DesktopSelectedAccountStore(
    private val directory: File = File(System.getProperty("user.home"), ".klaf_kt"),
) : SelectedAccountStore {

    override fun read(): String? {
        val file = File(directory, SELECTED_ACCOUNT_FILE_NAME)
        if (!file.exists()) return null
        return file.readText(Charsets.UTF_8).takeIf(String::isNotBlank)
    }

    override fun write(email: String?) {
        require(directory.isDirectory || directory.mkdirs()) { "Cannot create local account directory" }
        val temporaryFile = File(directory, TEMP_ACCOUNT_FILE_NAME).toPath()
        val selectedFile = File(directory, SELECTED_ACCOUNT_FILE_NAME).toPath()
        Files.writeString(temporaryFile, email.orEmpty())
        try {
            Files.move(temporaryFile, selectedFile, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporaryFile, selectedFile, REPLACE_EXISTING)
        }
    }
}
