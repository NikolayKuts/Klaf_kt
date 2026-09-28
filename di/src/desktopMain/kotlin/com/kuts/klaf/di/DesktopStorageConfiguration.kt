package com.kuts.klaf.di

import java.io.File
import java.util.Properties

private const val TEST_ROOT_ENVIRONMENT_VARIABLE = "KLAF_REMOTE_STORAGE_TEST_DIR"
private const val TEST_DIRECTORY_PREFIX = "klaf-remote-storage-test-"
private const val LEGACY_DATABASE_NAME = "klaf_kt.db"

internal data class DesktopStorageConfiguration(
    val directory: File,
    val useAccountScopedStorage: Boolean,
) {

    companion object {

        fun fromEnvironment(): DesktopStorageConfiguration {
            val properties = Properties().apply {
                findDeveloperLocalProperties()?.inputStream()?.use(::load)
            }
            return fromInputs(
                rawTestDirectory = System.getenv(TEST_ROOT_ENVIRONMENT_VARIABLE),
                requestedMode = properties.getProperty("klaf.client.storage.mode"),
            )
        }

        fun fromTestDirectory(rawDirectory: String?): DesktopStorageConfiguration = fromInputs(
            rawTestDirectory = rawDirectory,
            requestedMode = null,
        )

        internal fun fromInputs(
            rawTestDirectory: String?,
            requestedMode: String?,
            ordinaryDirectory: File = File(System.getProperty("user.home"), ".klaf_kt"),
        ): DesktopStorageConfiguration {
            if (rawTestDirectory.isNullOrBlank()) {
                val directory = ordinaryDirectory.canonicalFile
                return DesktopStorageConfiguration(
                    directory = directory,
                    useAccountScopedStorage = useAccountScopedStorage(
                        requestedMode = requestedMode,
                        isolatedTestIdentity = false,
                        legacyDatabaseExists = File(directory, LEGACY_DATABASE_NAME).exists(),
                    ),
                )
            }
            val requested = File(rawTestDirectory)
            require(requested.isAbsolute) { "$TEST_ROOT_ENVIRONMENT_VARIABLE must be an absolute path" }
            val directory = requested.canonicalFile
            require(directory.name.startsWith(TEST_DIRECTORY_PREFIX) && directory.name.length > TEST_DIRECTORY_PREFIX.length) {
                "$TEST_ROOT_ENVIRONMENT_VARIABLE must name a disposable $TEST_DIRECTORY_PREFIX directory"
            }
            require(directory.isDirectory || directory.mkdirs()) { "Cannot create test data directory" }
            return DesktopStorageConfiguration(directory, true)
        }
    }
}
