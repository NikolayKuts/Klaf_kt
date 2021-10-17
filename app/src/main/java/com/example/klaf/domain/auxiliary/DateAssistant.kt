package com.example.klaf.domain.auxiliary

import java.util.*

class DateAssistant {

    fun getCurrentDateLong(): Long {
        return Calendar.getInstance().time.time
    }
}