package com.meshlink.ble.data;

import android.content.Context;
import com.meshlink.ble.data.source.BleMeshDataSource;
import com.meshlink.ble.discovery.DiscoveryEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DiscoveryManager_Factory implements Factory<DiscoveryManager> {
  private final Provider<Context> contextProvider;

  private final Provider<BleMeshDataSource> bleDataSourceProvider;

  private final Provider<DiscoveryEngine> discoveryEngineProvider;

  public DiscoveryManager_Factory(Provider<Context> contextProvider,
      Provider<BleMeshDataSource> bleDataSourceProvider,
      Provider<DiscoveryEngine> discoveryEngineProvider) {
    this.contextProvider = contextProvider;
    this.bleDataSourceProvider = bleDataSourceProvider;
    this.discoveryEngineProvider = discoveryEngineProvider;
  }

  @Override
  public DiscoveryManager get() {
    return newInstance(contextProvider.get(), bleDataSourceProvider.get(), discoveryEngineProvider.get());
  }

  public static DiscoveryManager_Factory create(Provider<Context> contextProvider,
      Provider<BleMeshDataSource> bleDataSourceProvider,
      Provider<DiscoveryEngine> discoveryEngineProvider) {
    return new DiscoveryManager_Factory(contextProvider, bleDataSourceProvider, discoveryEngineProvider);
  }

  public static DiscoveryManager newInstance(Context context, BleMeshDataSource bleDataSource,
      DiscoveryEngine discoveryEngine) {
    return new DiscoveryManager(context, bleDataSource, discoveryEngine);
  }
}
