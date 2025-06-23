//package com.kuts.klaf.data.common
//
//import org.junit.jupiter.api.Assertions.*
//
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//
//class DateAssistantKtTest {
//
//    @BeforeEach
//    fun setUp() {
//    }
//
//    @Test
//    fun calculateDetailedScheduledInterval() {
//        // 15 часов в миллисекундах
//        val fifteenHoursMillis = 15 * 60 * 60 * 1000L
//
//        // вызываем функцию
//        val result = fifteenHoursMillis.calculateDetailedScheduledInterval()
//
//        // проверяем результат
//        assertEquals(0, result.year.value)
//        assertEquals(0, result.month.value)
//        assertEquals(0, result.week.value)
//        assertEquals(0, result.day.value)
//        assertEquals(15, result.hour.value)
//        assertEquals(0, result.minute.value)
//    }
//}