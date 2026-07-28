package com.meshlink.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class ArchitectureTest {

    @Test
    fun `interfaces reside in api or domain packages`() {
        Konsist.scopeFromProject()
            .interfaces()
            .withNameEndingWith("Repository", "Transport", "Router")
            .assertTrue {
                it.resideInPackage("com.meshlink..")
            }
    }

    @Test
    fun `implementations reside in data packages`() {
        Konsist.scopeFromProject()
            .classes()
            .withNameEndingWith("Impl")
            .assertTrue {
                it.resideInPackage("com.meshlink..")
            }
    }
}
