package com.meshlink.ble.data;

import com.meshlink.ble.data.source.BleMeshDataSource;
import com.meshlink.ble.discovery.DiscoveryEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class BleConnectionManager_Factory implements Factory<BleConnectionManager> {
  private final Provider<BleMeshDataSource> bleDataSourceProvider;

  private final Provider<DiscoveryEngine> discoveryEngineProvider;

  public BleConnectionManager_Factory(Provider<BleMeshDataSource> bleDataSourceProvider,
      Provider<DiscoveryEngine> discoveryEngineProvider) {
    this.bleDataSourceProvider = bleDataSourceProvider;
    this.discoveryEngineProvider = discoveryEngineProvider;
  }

  @Override
  public BleConnectionManager get() {
    return newInstance(bleDataSourceProvider.get(), discoveryEngineProvider.get());
  }

  public static BleConnectionManager_Factory create(
      Provider<BleMeshDataSource> bleDataSourceProvider,
      Provider<DiscoveryEngine> discoveryEngineProvider) {
    return new BleConnectionManager_Factory(bleDataSourceProvider, discoveryEngineProvider);
  }

  public static BleConnectionManager newInstance(BleMeshDataSource bleDataSource,
      DiscoveryEngine discoveryEngine) {
    return new BleConnectionManager(bleDataSource, discoveryEngine);
  }
}
