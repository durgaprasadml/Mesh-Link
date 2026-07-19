package com.meshlink.routing.data;

import com.meshlink.analytics.data.MeshAnalytics;
import com.meshlink.ble.data.BleGattManager;
import com.meshlink.database.data.local.RelayDao;
import com.meshlink.routing.engine.RoutingEngine;
import com.meshlink.security.data.TrustManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.meshlink.di.IoDispatcher")
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
public final class MeshRouter_Factory implements Factory<MeshRouter> {
  private final Provider<BleGattManager> gattManagerProvider;

  private final Provider<MeshAnalytics> analyticsProvider;

  private final Provider<RelayDao> relayDaoProvider;

  private final Provider<TrustManager> trustManagerProvider;

  private final Provider<RoutingEngine> routingEngineProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public MeshRouter_Factory(Provider<BleGattManager> gattManagerProvider,
      Provider<MeshAnalytics> analyticsProvider, Provider<RelayDao> relayDaoProvider,
      Provider<TrustManager> trustManagerProvider, Provider<RoutingEngine> routingEngineProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.gattManagerProvider = gattManagerProvider;
    this.analyticsProvider = analyticsProvider;
    this.relayDaoProvider = relayDaoProvider;
    this.trustManagerProvider = trustManagerProvider;
    this.routingEngineProvider = routingEngineProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public MeshRouter get() {
    return newInstance(gattManagerProvider.get(), analyticsProvider.get(), relayDaoProvider.get(), trustManagerProvider.get(), routingEngineProvider.get(), ioDispatcherProvider.get());
  }

  public static MeshRouter_Factory create(Provider<BleGattManager> gattManagerProvider,
      Provider<MeshAnalytics> analyticsProvider, Provider<RelayDao> relayDaoProvider,
      Provider<TrustManager> trustManagerProvider, Provider<RoutingEngine> routingEngineProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new MeshRouter_Factory(gattManagerProvider, analyticsProvider, relayDaoProvider, trustManagerProvider, routingEngineProvider, ioDispatcherProvider);
  }

  public static MeshRouter newInstance(BleGattManager gattManager, MeshAnalytics analytics,
      RelayDao relayDao, TrustManager trustManager, RoutingEngine routingEngine,
      CoroutineDispatcher ioDispatcher) {
    return new MeshRouter(gattManager, analytics, relayDao, trustManager, routingEngine, ioDispatcher);
  }
}
