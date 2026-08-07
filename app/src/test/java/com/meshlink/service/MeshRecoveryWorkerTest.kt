package com.meshlink.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MeshRecoveryWorkerTest {

    @Test
    fun testWorkNameConstant() {
        assertEquals("mesh_recovery_work", MeshRecoveryWorker.WORK_NAME)
    }
}
