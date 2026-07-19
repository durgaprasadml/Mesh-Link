package com.meshlink.routing.engine;

import com.meshlink.ai.engine.TransportPredictor;
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
public final class IntelligentTransportManager_Factory implements Factory<IntelligentTransportManager> {
  private final Provider<RouteOptimizer> routeOptimizerProvider;

  private final Provider<TransportPredictor> transportPredictorProvider;

  public IntelligentTransportManager_Factory(Provider<RouteOptimizer> routeOptimizerProvider,
      Provider<TransportPredictor> transportPredictorProvider) {
    this.routeOptimizerProvider = routeOptimizerProvider;
    this.transportPredictorProvider = transportPredictorProvider;
  }

  @Override
  public IntelligentTransportManager get() {
    return newInstance(routeOptimizerProvider.get(), transportPredictorProvider.get());
  }

  public static IntelligentTransportManager_Factory create(
      Provider<RouteOptimizer> routeOptimizerProvider,
      Provider<TransportPredictor> transportPredictorProvider) {
    return new IntelligentTransportManager_Factory(routeOptimizerProvider, transportPredictorProvider);
  }

  public static IntelligentTransportManager newInstance(RouteOptimizer routeOptimizer,
      TransportPredictor transportPredictor) {
    return new IntelligentTransportManager(routeOptimizer, transportPredictor);
  }
}
