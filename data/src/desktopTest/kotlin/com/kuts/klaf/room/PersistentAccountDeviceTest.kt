package com.kuts.klaf.room

import com.kuts.klaf.networking.klafServer.DesktopAccountDeviceStore
import com.kuts.klaf.networking.klafServer.PersistentAccountDeviceProvider
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.coroutines.runBlocking

class PersistentAccountDeviceTest {

    @Test
    fun `device identity survives provider restart and stays the same for multiple accounts`() = runBlocking {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-device-test-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-device-test-"))
        try {
            val first = PersistentAccountDeviceProvider(DesktopAccountDeviceStore(directory.toFile()))
                .getOrCreate("Laptop", "DESKTOP")
            val restarted = PersistentAccountDeviceProvider(DesktopAccountDeviceStore(directory.toFile()))
                .getOrCreate("Renamed laptop", "DESKTOP")
            assertEquals(first, restarted)

            val otherDirectory = Files.createTempDirectory(tempRoot, "klaf-device-test-").toRealPath()
            try {
                val other = PersistentAccountDeviceProvider(DesktopAccountDeviceStore(otherDirectory.toFile()))
                    .getOrCreate("Other laptop", "DESKTOP")
                assertNotEquals(first.id, other.id)
            } finally {
                otherDirectory.toFile().deleteRecursively()
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
