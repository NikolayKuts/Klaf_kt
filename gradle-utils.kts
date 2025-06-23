

fun loadLocalProperties(rootDir: File): java.util.Properties {
    return java.util.Properties().apply {
        val localPropsFile = rootDir.resolve("local.properties")
        if (localPropsFile.exists()) {
            load(localPropsFile.inputStream())
        }
    }
}

fun getLocalProperty(key: String): String = localProperties.getProperty(key)
    ?: error("Missing local.properties key: $key")