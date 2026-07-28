package com.meshlink.core.permissions

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionsModule {

    @Binds
    abstract fun bindBluetoothPermissionChecker(
        impl: BluetoothPermissionCheckerImpl
    ): BluetoothPermissionChecker
}
