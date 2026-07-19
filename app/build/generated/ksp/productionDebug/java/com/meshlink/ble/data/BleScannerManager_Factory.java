package com.meshlink.ble.data;

import android.content.Context;
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
public final class BleScannerManager_Factory implements Factory<BleScannerManager> {
  private final Provider<Context> contextProvider;

  private final Provider<DiscoveryEngine> discoveryEngineProvider;

  public BleScannerManager_Factory(Provider<Context> contextProvider,
      Provider<DiscoveryEngine> discoveryEngineProvider) {
    this.contextProvider = contextProvider;
    this.discoveryEngineProvider = discoveryEngineProvider;
  }

  @Override
  public BleScannerManager get() {
    return newInstance(contextProvider.get(), discoveryEngineProvider.get());
  }

  public static BleScannerManager_Factory create(Provider<Context> contextProvider,
      Provider<DiscoveryEngine> discoveryEngineProvider) {
    return new BleScannerManager_Factory(contextProvider, discoveryEngineProvider);
  }

  public static BleScannerManager newInstance(Context context, DiscoveryEngine discoveryEngine) {
    return new BleScannerManager(context, discoveryEngine);
  }
}
