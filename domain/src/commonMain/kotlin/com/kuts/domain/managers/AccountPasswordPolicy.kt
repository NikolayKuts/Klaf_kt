package com.kuts.domain.managers

/** Exact account password rules; the entered value is never trimmed or normalized. */
object AccountPasswordPolicy {

    private const val MIN_CODE_POINTS = 15
    private const val MAX_CODE_POINTS = 128
    private const val MAX_UTF8_BYTES = 1_024

    fun isValid(value: String): Boolean {
        var codePoints = 0
        var index = 0
        while (index < value.length) {
            val current = value[index]
            if (current == '\u0000' || current.isWhitespace()) return false
            if (current.isHighSurrogate()) {
                if (index + 1 >= value.length || !value[index + 1].isLowSurrogate()) return false
                index += 2
            } else {
                if (current.isLowSurrogate()) return false
                index++
            }
            codePoints++
            if (codePoints > MAX_CODE_POINTS) return false
        }
        return codePoints >= MIN_CODE_POINTS && value.encodeToByteArray().size <= MAX_UTF8_BYTES
    }
}
