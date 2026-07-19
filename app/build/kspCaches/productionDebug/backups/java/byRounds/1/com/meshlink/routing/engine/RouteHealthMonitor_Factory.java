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
public final class RouteHealthMonitor_Factory implements Factory<RouteHealthMonitor> {
  private final Provider<RouteCache> routeCacheProvider;

  private final Provider<RouteScorer> routeScorerProvider;

  public RouteHealthMonitor_Factory(Provider<RouteCache> routeCacheProvider,
      Provider<RouteScorer> routeScorerProvider) {
    this.routeCacheProvider = routeCacheProvider;
    this.routeScorerProvider = routeScorerProvider;
  }

  @Override
  public RouteHealthMonitor get() {
    return newInstance(routeCacheProvider.get(), routeScorerProvider.get());
  }

  public static RouteHealthMonitor_Factory create(Provider<RouteCache> routeCacheProvider,
      Provider<RouteScorer> routeScorerProvider) {
    return new RouteHealthMonitor_Factory(routeCacheProvider, routeScorerProvider);
  }

  public static RouteHealthMonitor newInstance(RouteCache routeCache, RouteScorer routeScorer) {
    return new RouteHealthMonitor(routeCache, routeScorer);
  }
}
