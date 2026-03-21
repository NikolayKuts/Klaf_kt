package com.kuts.klaf.navigation.navType

internal fun String.encodeNavValue(): String {
    val bytes = encodeToByteArray()

    return buildString(capacity = bytes.size) {
        bytes.forEach { byte ->
            val value = byte.toInt() and 0xFF
            val character = value.toChar()

            if (character.isUnreservedNavCharacter()) {
                append(character)
            } else {
                append('%')
                append("0123456789ABCDEF"[value ushr 4])
                append("0123456789ABCDEF"[value and 0x0F])
            }
        }
    }
}

internal fun String.decodeNavValue(): String {
    if ('%' !in this) return this

    val bytes = ArrayList<Byte>(length)
    var index = 0

    while (index < length) {
        val character = this[index]
        if (character == '%') {
            require(index + 2 < length) { "Invalid percent-encoded value: $this" }

            val high = this[index + 1].digitToInt(radix = 16)
            val low = this[index + 2].digitToInt(radix = 16)
            bytes += ((high shl 4) + low).toByte()
            index += 3
        } else {
            bytes += character.code.toByte()
            index += 1
        }
    }

    return bytes.toByteArray().decodeToString()
}

private fun Char.isUnreservedNavCharacter(): Boolean {
    return this in 'A'..'Z' ||
        this in 'a'..'z' ||
        this in '0'..'9' ||
        this == '-' ||
        this == '.' ||
        this == '_' ||
        this == '~'
}
