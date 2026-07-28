package com.meshlink.di

import com.meshlink.routing.api.Router
import com.meshlink.routing.data.MeshRouter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RoutingModule {
    @Binds
    @Singleton
    internal abstract fun bindRouter(
        impl: MeshRouter
    ): Router
}
