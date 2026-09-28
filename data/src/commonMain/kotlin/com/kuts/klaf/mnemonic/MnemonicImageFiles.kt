package com.kuts.klaf.mnemonic

const val MAX_MNEMONIC_IMAGE_BYTES = 16 * 1024 * 1024
private val SAFE_IMAGE_ASSET_ID = Regex("[A-Za-z0-9_-]{1,128}")
private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10)

fun isSafeMnemonicImageId(value: String): Boolean = SAFE_IMAGE_ASSET_ID.matches(value)

fun mnemonicImageContentType(bytes: ByteArray): String? = when {
    bytes.size >= 8 && PNG_SIGNATURE.indices.all { bytes[it] == PNG_SIGNATURE[it] } -> "image/png"
    bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() -> "image/jpeg"
    bytes.size >= 12 && bytes.copyOfRange(0, 4).decodeToString() == "RIFF" && bytes.copyOfRange(8, 12).decodeToString() == "WEBP" -> "image/webp"
    else -> null
}
