plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.meshlink"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.meshlink"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.meshlink.util.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        resourceConfigurations += "en"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("internal") {
            dimension = "environment"
            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"
            buildConfigField("String", "ENVIRONMENT", "\"INTERNAL\"")
            buildConfigField("Boolean", "DEBUG_TOOLS_ENABLED", "true")
        }
        create("beta") {
            dimension = "environment"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            buildConfigField("String", "ENVIRONMENT", "\"BETA\"")
            buildConfigField("Boolean", "DEBUG_TOOLS_ENABLED", "false")
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "ENVIRONMENT", "\"PRODUCTION\"")
            buildConfigField("Boolean", "DEBUG_TOOLS_ENABLED", "false")
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("Boolean", "LOGGING_ENABLED", "true")
            // Disable PNG crunching in debug builds to speed up the build process
            isCrunchPngs = false
            // Speed up builds by avoiding split APKs in debug
            extra["alwaysUpdateBuildId"] = false
            
            // Disable Crashlytics mapping file generation for faster debug builds
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            buildConfigField("Boolean", "LOGGING_ENABLED", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("Boolean", "LOGGING_ENABLED", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        val sharedTestDir = "src/sharedTest/java"
        getByName("test") {
            java.srcDir(sharedTestDir)
        }
        getByName("androidTest") {
            java.srcDir(sharedTestDir)
        }
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xbackend-threads=4"
        )
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all { test ->
            test.maxHeapSize = "2048m"
            test.testLogging {
                showStandardStreams = true
            }
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        disable += "ExtractNativeLibs"
    }
}

dependencies {

    // Compose BOM
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // CameraX
    
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // WorkManager
    
    implementation(libs.androidx.work.runtime.ktx)

    // Room
    
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // Gson
    implementation(libs.gson)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // SQLCipher
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite.ktx)

    // Security
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.json)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation("com.lemonappdev:konsist:0.15.1")
    testImplementation(kotlin("test"))
    
    testImplementation(libs.robolectric)
    implementation(libs.kotlinx.coroutines.guava)
    
    // Hilt Testing
    testImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    // Room Testing
    testImplementation(libs.androidx.room.testing)
    
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Firebase Crashlytics & Analytics
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
}