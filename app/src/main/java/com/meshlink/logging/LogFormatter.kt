package com.meshlink.logging

/**
 * Interface for formatting a structured LogEvent into a string.
 */
interface LogFormatter {
    fun format(event: LogEvent): String
}
