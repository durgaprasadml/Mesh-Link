package com.meshlink.common.diagnostics;

import com.meshlink.routing.engine.RouteCache;
import com.meshlink.transfer.TransferCache;
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
public final class CrashPreventionManager_Factory implements Factory<CrashPreventionManager> {
  private final Provider<SystemResourceMonitor> resourceMonitorProvider;

  private final Provider<RouteCache> routeCacheProvider;

  private final Provider<TransferCache> transferCacheProvider;

  public CrashPreventionManager_Factory(Provider<SystemResourceMonitor> resourceMonitorProvider,
      Provider<RouteCache> routeCacheProvider, Provider<TransferCache> transferCacheProvider) {
    this.resourceMonitorProvider = resourceMonitorProvider;
    this.routeCacheProvider = routeCacheProvider;
    this.transferCacheProvider = transferCacheProvider;
  }

  @Override
  public CrashPreventionManager get() {
    return newInstance(resourceMonitorProvider.get(), routeCacheProvider.get(), transferCacheProvider.get());
  }

  public static CrashPreventionManager_Factory create(
      Provider<SystemResourceMonitor> resourceMonitorProvider,
      Provider<RouteCache> routeCacheProvider, Provider<TransferCache> transferCacheProvider) {
    return new CrashPreventionManager_Factory(resourceMonitorProvider, routeCacheProvider, transferCacheProvider);
  }

  public static CrashPreventionManager newInstance(SystemResourceMonitor resourceMonitor,
      RouteCache routeCache, TransferCache transferCache) {
    return new CrashPreventionManager(resourceMonitor, routeCache, transferCache);
  }
}
