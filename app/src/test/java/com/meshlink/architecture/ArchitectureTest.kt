package com.meshlink.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class ArchitectureTest {

    @Test
    fun `clean architecture layers have correct dependencies`() {
        Konsist.scopeFromProject()
            .assertArchitecture {
                val domain = Layer("Domain", "com.meshlink.domain..")
                val data = Layer("Data", "com.meshlink.*.data..")
                val api = Layer("Api", "com.meshlink.*.api..")
                val ui = Layer("UI", "com.meshlink.ui..")
                val common = Layer("Common", "com.meshlink.common..")
                
                domain.dependsOnNothing()
                
                // Api layers can depend on domain and common
                api.dependsOn(domain, common)
                
                // Data layers depend on domain, api, and common
                data.dependsOn(domain, api, common)
                
                // UI layers depend on domain, api, and common (and maybe data indirectly via Hilt, but ideally just api and domain)
                // We'll be lenient with UI for now, but UI should primarily depend on domain and api
                ui.dependsOn(domain, api, common)
            }
    }

    @Test
    fun `interfaces reside in api packages`() {
        // Enforce that if a module has api/data separation, interfaces for services go to api
        Konsist.scopeFromProject()
            .interfaces()
            .withNameEndingWith("Repository", "Transport", "Router")
            .assertTrue {
                it.resideInPackage("..api..") || it.resideInPackage("..domain..")
            }
    }

    @Test
    fun `implementations reside in data packages and are internal`() {
        Konsist.scopeFromProject()
            .classes()
            .withNameEndingWith("Impl")
            .assertTrue {
                it.resideInPackage("..data..") && it.hasInternalModifier
            }
    }
}
