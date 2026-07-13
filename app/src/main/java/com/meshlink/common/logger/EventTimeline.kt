package com.meshlink.common.logger

import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventTimeline @Inject constructor() {
    
    private val MAX_EVENTS = 500
    private val events = ConcurrentLinkedDeque<TimelineEvent>()

    data class TimelineEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val eventName: String,
        val details: String? = null
    )

    fun pushEvent(eventName: String, details: String? = null) {
        events.addLast(TimelineEvent(eventName = eventName, details = details))
        if (events.size > MAX_EVENTS) {
            events.pollFirst()
        }
    }

    fun getEvents(): List<TimelineEvent> {
        return events.toList()
    }
}
