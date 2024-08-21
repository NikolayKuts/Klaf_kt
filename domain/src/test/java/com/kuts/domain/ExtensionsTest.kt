package com.kuts.domain

import com.kuts.domain.common.CoroutineStateHolder.Companion.launchWithState
import com.kuts.domain.common.CoroutineStateHolder.Companion.onException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ExtensionsTest {

    @Test
    fun `calling handling block on onException function call`() =
        runTest(dispatchTimeoutMs = 5000) {
            var isCalled = false

            val testJob = launchWithState(Job()) {
                delay(100)
                throw Exception("Test exception")
            } onException { _, _ ->
                isCalled = true
            }

            testJob.join()
            assertTrue(actual = isCalled)
        }

    @Test
    fun `calling handling block on deferred onException function call `() {
        runTest(dispatchTimeoutMs = 5000) {
            var isCalled = false

            val testJob = launchWithState(Job()) {
                throw Exception("Test exception")
            } onException { _, _ ->
                isCalled = true
            }

            delay(100)
            testJob.join()
            assertTrue(actual = isCalled)
        }
    }

    @Test
    fun `number of calling handling block equals to number of throwing exceptions`() {
        runTest(dispatchTimeoutMs = 5000) {
            var callingCount = 0
            val exceptionCount = 100

            val stateHolder = launchWithState(Job()) {
                supervisorScope {
                    val jobs = mutableListOf<Job>()

                    repeat(times = exceptionCount) {
                        val firstJob = launch { throw Exception("first exception") }
                        val secondJob = launch { throw Exception("second exception") }

                        jobs.add(firstJob)
                        jobs.add(secondJob)
                        delay(30)
                    }

                    joinAll(*jobs.toTypedArray())
                }
            }

            delay(1000)
            stateHolder.onException { _, _ -> callingCount++ }
            stateHolder.job.join()
            assertEquals(expected = exceptionCount * 2, actual = callingCount)
        }
    }
}