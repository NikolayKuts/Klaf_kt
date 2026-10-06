package com.kuts.klaf.cardManagement.mnemonic

import com.kuts.domain.entities.KlafServerConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MnemonicConnectionHintTest {

    @Test
    fun readyConnectionNeedsNoHint() {
        assertNull(mnemonicConnectionHint(KlafServerConnectionState.Ready))
    }

    @Test
    fun unavailableConnectionExplainsRecoveryState() {
        assertEquals(MnemonicConnectionHint.RETRY,
            mnemonicConnectionHint(KlafServerConnectionState.Disconnected))
        assertEquals(MnemonicConnectionHint.RETRY,
            mnemonicConnectionHint(KlafServerConnectionState.Error("authorization expired")))
        assertEquals(MnemonicConnectionHint.CONNECTING,
            mnemonicConnectionHint(KlafServerConnectionState.Reconnecting()))
    }
}
