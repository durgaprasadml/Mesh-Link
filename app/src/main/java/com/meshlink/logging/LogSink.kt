package com.meshlink.logging

/**
 * Interface for consuming and outputting formatted log events.
 */
interface LogSink {
    fun log(event: LogEvent)
}
