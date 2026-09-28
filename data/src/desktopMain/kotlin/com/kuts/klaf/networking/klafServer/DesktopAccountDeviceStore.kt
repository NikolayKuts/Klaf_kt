package com.kuts.klaf.networking.klafServer

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ACCOUNT_DEVICE_FILE_NAME = "account-device.json"
private const val ACCOUNT_DEVICE_TEMP_FILE_NAME = "account-device.tmp"

class DesktopAccountDeviceStore(
    private val directory: File = File(System.getProperty("user.home"), ".klaf_kt"),
) : AccountDeviceStore {

    override fun read(): AccountDevice? {
        val file = File(directory, ACCOUNT_DEVICE_FILE_NAME)
        if (!file.exists()) return null
        return Json.decodeFromString<AccountDevice>(file.readText(Charsets.UTF_8))
    }

    override fun write(device: AccountDevice) {
        require(directory.isDirectory || directory.mkdirs()) { "Cannot create local account directory" }
        val temporary = File(directory, ACCOUNT_DEVICE_TEMP_FILE_NAME).toPath()
        val destination = File(directory, ACCOUNT_DEVICE_FILE_NAME).toPath()
        Files.writeString(temporary, Json.encodeToString(device))
        try {
            Files.move(temporary, destination, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, destination, REPLACE_EXISTING)
        }
    }
}
