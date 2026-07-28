package com.meshlink.logging

import android.util.Log

/**
 * Outputs formatted log events to Android's built-in logcat.
 */
class AndroidLogSink(private val formatter: LogFormatter = PrettyFormatter()) : LogSink {
    
    override fun log(event: LogEvent) {
        val formattedMessage = formatter.format(event)
        val tag = "MeshLink-${event.category.name}"
        
        when (event.level) {
            LogLevel.VERBOSE -> Log.v(tag, formattedMessage)
            LogLevel.DEBUG -> Log.d(tag, formattedMessage)
            LogLevel.INFO -> Log.i(tag, formattedMessage)
            LogLevel.WARN -> Log.w(tag, formattedMessage)
            LogLevel.ERROR -> Log.e(tag, formattedMessage)
        }
    }
}
