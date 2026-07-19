package com.meshlink.ai.engine;

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
public final class RecommendationEngine_Factory implements Factory<RecommendationEngine> {
  private final Provider<CongestionPredictor> congestionPredictorProvider;

  private final Provider<BatteryPredictor> batteryPredictorProvider;

  private final Provider<TransportPredictor> transportPredictorProvider;

  public RecommendationEngine_Factory(Provider<CongestionPredictor> congestionPredictorProvider,
      Provider<BatteryPredictor> batteryPredictorProvider,
      Provider<TransportPredictor> transportPredictorProvider) {
    this.congestionPredictorProvider = congestionPredictorProvider;
    this.batteryPredictorProvider = batteryPredictorProvider;
    this.transportPredictorProvider = transportPredictorProvider;
  }

  @Override
  public RecommendationEngine get() {
    return newInstance(congestionPredictorProvider.get(), batteryPredictorProvider.get(), transportPredictorProvider.get());
  }

  public static RecommendationEngine_Factory create(
      Provider<CongestionPredictor> congestionPredictorProvider,
      Provider<BatteryPredictor> batteryPredictorProvider,
      Provider<TransportPredictor> transportPredictorProvider) {
    return new RecommendationEngine_Factory(congestionPredictorProvider, batteryPredictorProvider, transportPredictorProvider);
  }

  public static RecommendationEngine newInstance(CongestionPredictor congestionPredictor,
      BatteryPredictor batteryPredictor, TransportPredictor transportPredictor) {
    return new RecommendationEngine(congestionPredictor, batteryPredictor, transportPredictor);
  }
}
