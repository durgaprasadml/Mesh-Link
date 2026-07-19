package com.meshlink.routing.engine;

import com.meshlink.ai.engine.FailurePredictor;
import com.meshlink.ai.engine.RoutePredictionEngine;
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
public final class RouteOptimizer_Factory implements Factory<RouteOptimizer> {
  private final Provider<RouteCache> routeCacheProvider;

  private final Provider<FailurePredictor> failurePredictorProvider;

  private final Provider<RoutePredictionEngine> routePredictionEngineProvider;

  public RouteOptimizer_Factory(Provider<RouteCache> routeCacheProvider,
      Provider<FailurePredictor> failurePredictorProvider,
      Provider<RoutePredictionEngine> routePredictionEngineProvider) {
    this.routeCacheProvider = routeCacheProvider;
    this.failurePredictorProvider = failurePredictorProvider;
    this.routePredictionEngineProvider = routePredictionEngineProvider;
  }

  @Override
  public RouteOptimizer get() {
    return newInstance(routeCacheProvider.get(), failurePredictorProvider.get(), routePredictionEngineProvider.get());
  }

  public static RouteOptimizer_Factory create(Provider<RouteCache> routeCacheProvider,
      Provider<FailurePredictor> failurePredictorProvider,
      Provider<RoutePredictionEngine> routePredictionEngineProvider) {
    return new RouteOptimizer_Factory(routeCacheProvider, failurePredictorProvider, routePredictionEngineProvider);
  }

  public static RouteOptimizer newInstance(RouteCache routeCache, FailurePredictor failurePredictor,
      RoutePredictionEngine routePredictionEngine) {
    return new RouteOptimizer(routeCache, failurePredictor, routePredictionEngine);
  }
}
