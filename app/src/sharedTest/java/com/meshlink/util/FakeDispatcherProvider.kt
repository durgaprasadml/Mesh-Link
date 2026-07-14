package com.meshlink.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class FakeDispatcherProvider(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) {
    val main: CoroutineDispatcher = testDispatcher
    val io: CoroutineDispatcher = testDispatcher
    val default: CoroutineDispatcher = testDispatcher
    val unconfined: CoroutineDispatcher = testDispatcher
}
