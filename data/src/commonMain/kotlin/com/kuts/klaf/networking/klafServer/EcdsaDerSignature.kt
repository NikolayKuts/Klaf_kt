package com.kuts.klaf.networking.klafServer

/** Converts Android Keystore's canonical ASN.1 DER ECDSA result to JWS's 64-byte ES256 signature. */
internal object EcdsaDerSignature {

    fun toJoseRaw(der: ByteArray): ByteArray {
        require(der.size in 8..72 && der[0].toInt() == 0x30)
        require(unsigned(der[1]) == der.size - 2) { "Invalid DER sequence length" }
        var position = 2
        val raw = ByteArray(64)
        for (component in 0..1) {
            require(position + 2 <= der.size && unsigned(der[position]) == 0x02)
            val length = unsigned(der[position + 1])
            position += 2
            require(length in 1..33 && position + length <= der.size)
            val first = unsigned(der[position])
            require(first and 0x80 == 0) { "Negative ECDSA integer" }
            if (length > 1 && first == 0) {
                require(unsigned(der[position + 1]) and 0x80 != 0) { "Noncanonical ECDSA integer" }
            }
            val start = if (length == 33) {
                require(first == 0)
                position + 1
            } else {
                position
            }
            val count = length - (start - position)
            require((start until position + length).any { der[it].toInt() != 0 })
            der.copyInto(raw, component * 32 + 32 - count, start, position + length)
            position += length
        }
        require(position == der.size) { "Trailing ECDSA signature bytes" }
        return raw
    }

    private fun unsigned(value: Byte): Int = value.toInt() and 0xff
}
