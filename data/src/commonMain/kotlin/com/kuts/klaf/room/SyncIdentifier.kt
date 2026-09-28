package com.kuts.klaf.room

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal fun newSyncId(): String = Uuid.random().toString()
