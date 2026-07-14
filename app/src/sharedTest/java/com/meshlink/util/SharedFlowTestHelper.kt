package com.meshlink.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SharedFlowTestHelper<T>(
    private val scope: TestScope,
    private val flow: SharedFlow<T>
) {
    val emittedValues = mutableListOf<T>()
    
    init {
        scope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
            flow.collect {
                emittedValues.add(it)
            }
        }
    }

    fun assertEmitted(vararg expected: T) {
        assert(emittedValues == expected.toList()) { "Expected ${expected.toList()}, but got $emittedValues" }
    }
}

fun <T> SharedFlow<T>.test(scope: TestScope): SharedFlowTestHelper<T> =
    SharedFlowTestHelper(scope, this)
