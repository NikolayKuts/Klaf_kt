package com.kuts.klaf.room.databases

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

suspend fun <R> KlafRoomDatabase.performInTransaction(block: suspend () -> R): R {
    return useWriterConnection { transactor ->
        transactor.immediateTransaction {
            block()
        }
    }
}
