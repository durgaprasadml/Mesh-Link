package com.meshlink.logging

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe, non-blocking object acting as the main logging facade for Mesh Link.
 */
object MeshLogger {

    @Volatile
    var config = LoggerConfig()

    private val sinks = CopyOnWriteArrayList<LogSink>()

    init {
        // Default to Android sink with pretty formatter
        addSink(AndroidLogSink())
    }

    /**
     * Adds a sink to route log events to.
     */
    fun addSink(sink: LogSink) {
        sinks.add(sink)
    }

    /**
     * Removes a sink.
     */
    fun removeSink(sink: LogSink) {
        sinks.remove(sink)
    }

    /**
     * Clears all sinks.
     */
    fun clearSinks() {
        sinks.clear()
    }

    inline fun v(
        category: LogCategory,
        context: LogContext = LogContext.EMPTY,
        exception: Throwable? = null,
        noinline metadata: (() -> Map<String, Any>)? = null,
        message: () -> String
    ) = log(LogLevel.VERBOSE, category, context, exception, metadata, message)

    inline fun d(
        category: LogCategory,
        context: LogContext = LogContext.EMPTY,
        exception: Throwable? = null,
        noinline metadata: (() -> Map<String, Any>)? = null,
        message: () -> String
    ) = log(LogLevel.DEBUG, category, context, exception, metadata, message)

    inline fun i(
        category: LogCategory,
        context: LogContext = LogContext.EMPTY,
        exception: Throwable? = null,
        noinline metadata: (() -> Map<String, Any>)? = null,
        message: () -> String
    ) = log(LogLevel.INFO, category, context, exception, metadata, message)

    inline fun w(
        category: LogCategory,
        context: LogContext = LogContext.EMPTY,
        exception: Throwable? = null,
        noinline metadata: (() -> Map<String, Any>)? = null,
        message: () -> String
    ) = log(LogLevel.WARN, category, context, exception, metadata, message)

    inline fun e(
        category: LogCategory,
        context: LogContext = LogContext.EMPTY,
        exception: Throwable? = null,
        noinline metadata: (() -> Map<String, Any>)? = null,
        message: () -> String
    ) = log(LogLevel.ERROR, category, context, exception, metadata, message)

    /**
     * Inline logging function to prevent parameter evaluation if the log level is not met.
     */
    inline fun log(
        level: LogLevel,
        category: LogCategory,
        context: LogContext = LogContext.EMPTY,
        exception: Throwable? = null,
        noinline metadata: (() -> Map<String, Any>)? = null,
        message: () -> String
    ) {
        val currentConfig = config
        if (!currentConfig.isEnabled) return
        if (level.priority < currentConfig.minLevel.priority) return
        if (!currentConfig.enabledCategories.contains(category)) return

        val evaluatedMessage = message()
        val evaluatedMetadata = metadata?.invoke()
        
        val event = LogEvent(
            timestamp = System.currentTimeMillis(),
            level = level,
            category = category,
            message = evaluatedMessage,
            context = context,
            threadName = Thread.currentThread().name,
            exception = exception,
            metadata = evaluatedMetadata
        )
        
        dispatch(event)
    }

    /**
     * Dispatch event to all registered sinks.
     */
    @PublishedApi
    internal fun dispatch(event: LogEvent) {
        sinks.forEach { sink ->
            try {
                sink.log(event)
            } catch (e: Exception) {
                // Ignore sink failures to avoid crashing the application on log calls
                e.printStackTrace()
            }
        }
    }
}
