package com.meshlink.di

import com.meshlink.ble.data.BleRepositoryImpl
import com.meshlink.core.data.UserRepositoryImpl
import com.meshlink.core.data.source.UserLocalDataSource
import com.meshlink.core.data.source.UserLocalDataSourceImpl
import com.meshlink.database.data.source.ChatLocalDataSource
import com.meshlink.database.data.source.ChatLocalDataSourceImpl
import com.meshlink.domain.repository.ChatRepository
import com.meshlink.domain.repository.MeshRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.messaging.data.MessagingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindMeshRepository(
        impl: BleRepositoryImpl
    ): MeshRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: MessagingRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindUserLocalDataSource(
        impl: UserLocalDataSourceImpl
    ): UserLocalDataSource

    @Binds
    @Singleton
    abstract fun bindChatLocalDataSource(
        impl: ChatLocalDataSourceImpl
    ): ChatLocalDataSource
}
