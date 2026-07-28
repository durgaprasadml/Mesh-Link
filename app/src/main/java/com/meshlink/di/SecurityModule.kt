package com.meshlink.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindKeystoreManager(
        impl: com.meshlink.security.data.KeystoreManagerImpl
    ): com.meshlink.security.data.KeystoreManager
}
