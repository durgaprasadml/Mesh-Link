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
public final class RouteManager_Factory implements Factory<RouteManager> {
  private final Provider<RouteCache> routeCacheProvider;

  private final Provider<RouteScorer> routeScorerProvider;

  private final Provider<RouteOptimizer> routeOptimizerProvider;

  public RouteManager_Factory(Provider<RouteCache> routeCacheProvider,
      Provider<RouteScorer> routeScorerProvider, Provider<RouteOptimizer> routeOptimizerProvider) {
    this.routeCacheProvider = routeCacheProvider;
    this.routeScorerProvider = routeScorerProvider;
    this.routeOptimizerProvider = routeOptimizerProvider;
  }

  @Override
  public RouteManager get() {
    return newInstance(routeCacheProvider.get(), routeScorerProvider.get(), routeOptimizerProvider.get());
  }

  public static RouteManager_Factory create(Provider<RouteCache> routeCacheProvider,
      Provider<RouteScorer> routeScorerProvider, Provider<RouteOptimizer> routeOptimizerProvider) {
    return new RouteManager_Factory(routeCacheProvider, routeScorerProvider, routeOptimizerProvider);
  }

  public static RouteManager newInstance(RouteCache routeCache, RouteScorer routeScorer,
      RouteOptimizer routeOptimizer) {
    return new RouteManager(routeCache, routeScorer, routeOptimizer);
  }
}
