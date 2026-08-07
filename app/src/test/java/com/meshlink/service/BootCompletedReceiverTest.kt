package com.meshlink.service

import org.junit.Assert.assertNotNull
import org.junit.Test

class BootCompletedReceiverTest {

    @Test
    fun testReceiverInstantiation() {
        val receiver = BootCompletedReceiver()
        assertNotNull(receiver)
    }
}
