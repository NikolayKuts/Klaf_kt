package com.kuts.klaf.networking.klafServer

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PENDING_SIGNUP_FILE_NAME = "pending-signup.json"
private const val PENDING_SIGNUP_TEMP_FILE_NAME = "pending-signup.tmp"

class DesktopPendingSignUpAttemptStore(
    private val directory: File = File(System.getProperty("user.home"), ".klaf_kt"),
) : PendingSignUpAttemptStore {

    override fun read(): PendingSignUpAttempt? {
        val file = File(directory, PENDING_SIGNUP_FILE_NAME)
        if (!file.exists()) return null
        return Json.decodeFromString<PendingSignUpAttempt>(file.readText(Charsets.UTF_8))
    }

    override fun write(attempt: PendingSignUpAttempt) {
        require(directory.isDirectory || directory.mkdirs()) { "Cannot create local account directory" }
        val temporary = File(directory, PENDING_SIGNUP_TEMP_FILE_NAME).toPath()
        val destination = File(directory, PENDING_SIGNUP_FILE_NAME).toPath()
        Files.writeString(temporary, Json.encodeToString(attempt))
        try {
            Files.move(temporary, destination, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, destination, REPLACE_EXISTING)
        }
    }

    override fun clear() {
        Files.deleteIfExists(File(directory, PENDING_SIGNUP_FILE_NAME).toPath())
    }
}
