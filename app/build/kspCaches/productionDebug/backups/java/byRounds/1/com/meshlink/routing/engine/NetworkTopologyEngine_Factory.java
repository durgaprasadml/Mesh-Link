package com.meshlink.routing.engine;

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
public final class NetworkTopologyEngine_Factory implements Factory<NetworkTopologyEngine> {
  private final Provider<RouteCache> routeCacheProvider;

  public NetworkTopologyEngine_Factory(Provider<RouteCache> routeCacheProvider) {
    this.routeCacheProvider = routeCacheProvider;
  }

  @Override
  public NetworkTopologyEngine get() {
    return newInstance(routeCacheProvider.get());
  }

  public static NetworkTopologyEngine_Factory create(Provider<RouteCache> routeCacheProvider) {
    return new NetworkTopologyEngine_Factory(routeCacheProvider);
  }

  public static NetworkTopologyEngine newInstance(RouteCache routeCache) {
    return new NetworkTopologyEngine(routeCache);
  }
}
