package com.meshlink.di

import android.content.Context
import com.meshlink.common.logger.CrashReporter
import com.meshlink.common.logger.FirebaseCrashReporterImpl
import com.meshlink.common.logger.NoOpCrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {

    @Provides
    @Singleton
    fun provideCrashReporter(
        @ApplicationContext context: Context
    ): CrashReporter {
        return try {
            FirebaseCrashReporterImpl(context)
        } catch (e: Throwable) {
            NoOpCrashReporter()
        }
    }
}
