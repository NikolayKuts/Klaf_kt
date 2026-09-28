package com.kuts.klaf.networking.klafServer

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal fun newAccountProtocolId(): String = Uuid.random().toString()
