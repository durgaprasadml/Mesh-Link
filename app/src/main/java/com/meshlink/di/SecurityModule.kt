package com.meshlink.di

import com.meshlink.security.data.source.CryptoDataSource
import com.meshlink.security.data.source.CryptoDataSourceImpl
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
    abstract fun bindCryptoDataSource(
        impl: CryptoDataSourceImpl
    ): CryptoDataSource

}
