package com.kuts.klaf.di

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopStorageConfigurationTest {

    @Test
    fun `normal launch uses Room and keeps its configured ordinary directory`() {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-default-launch-test-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-default-launch-test-"))
        try {
            val configuration = DesktopStorageConfiguration.fromInputs(null, null, directory.toFile())

            assertTrue(configuration.useAccountScopedStorage)
            assertEquals(directory.toFile(), configuration.directory)
            assertTrue(directory.toFile().listFiles().orEmpty().isEmpty())
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `test mode uses explicit isolated directory`() {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val parent = Files.createTempDirectory(tempRoot, "klaf-config-test-").toRealPath()
        require(parent.parent == tempRoot && parent.fileName.toString().startsWith("klaf-config-test-"))
        try {
            val directory = parent.resolve("klaf-remote-storage-test-client").toFile()
            val configuration = DesktopStorageConfiguration.fromTestDirectory(directory.absolutePath)

            assertTrue(configuration.useAccountScopedStorage)
            assertEquals(directory.canonicalFile, configuration.directory)
            assertTrue(directory.isDirectory)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun `test mode rejects broad and legacy targets`() {
        assertFailsWith<IllegalArgumentException> {
            DesktopStorageConfiguration.fromTestDirectory(File(System.getProperty("user.home")).absolutePath)
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopStorageConfiguration.fromTestDirectory(File(System.getProperty("user.home"), ".klaf_kt").absolutePath)
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopStorageConfiguration.fromTestDirectory("relative-test-data")
        }
    }

    @Test
    fun `ordinary Room mode requires a clean legacy database path`() {
        val tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath()
        val directory = Files.createTempDirectory(tempRoot, "klaf-cutover-test-").toRealPath()
        require(directory.parent == tempRoot && directory.fileName.toString().startsWith("klaf-cutover-test-"))
        try {
            val clean = DesktopStorageConfiguration.fromInputs(null, "room", directory.toFile())
            assertTrue(clean.useAccountScopedStorage)
            assertEquals(directory.toFile(), clean.directory)

            Files.createFile(directory.resolve("klaf_kt.db"))
            assertFailsWith<IllegalStateException> {
                DesktopStorageConfiguration.fromInputs(null, null, directory.toFile())
            }
            assertFailsWith<IllegalStateException> {
                DesktopStorageConfiguration.fromInputs(null, "room", directory.toFile())
            }
            assertFalse(DesktopStorageConfiguration.fromInputs(null, "legacy", directory.toFile()).useAccountScopedStorage)
            assertTrue(Files.exists(directory.resolve("klaf_kt.db")))
            assertEquals(listOf("klaf_kt.db"), directory.toFile().listFiles().orEmpty().map { it.name })
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `invalid ordinary storage mode fails before opening a database`() {
        assertFailsWith<IllegalArgumentException> {
            DesktopStorageConfiguration.fromInputs(null, "unknown")
        }
    }
}
