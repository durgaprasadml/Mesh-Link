package com.meshlink.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlowTestObserver<T>(
    coroutineScope: CoroutineScope,
    flow: Flow<T>
) {
    private val _values = mutableListOf<T>()
    val values: List<T> get() = _values.toList()

    private val job: Job = coroutineScope.launch {
        flow.collect {
            _values.add(it)
        }
    }

    fun assertNoValues() {
        assert(_values.isEmpty())
    }

    fun assertValues(vararg expected: T) {
        assert(_values == expected.toList())
    }
    
    fun finish() {
        job.cancel()
    }
}

fun <T> Flow<T>.test(scope: CoroutineScope): FlowTestObserver<T> {
    return FlowTestObserver(scope, this)
}
