package com.meshlink.ai.engine;

import com.meshlink.routing.engine.CongestionMonitor;
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
public final class MeshAIManager_Factory implements Factory<MeshAIManager> {
  private final Provider<RoutePredictionEngine> routePredictionEngineProvider;

  private final Provider<CongestionPredictor> congestionPredictorProvider;

  private final Provider<BatteryPredictor> batteryPredictorProvider;

  private final Provider<TransportPredictor> transportPredictorProvider;

  private final Provider<FailurePredictor> failurePredictorProvider;

  private final Provider<UserBehaviorEngine> userBehaviorEngineProvider;

  private final Provider<AnomalyDetector> anomalyDetectorProvider;

  private final Provider<RecommendationEngine> recommendationEngineProvider;

  private final Provider<CongestionMonitor> congestionMonitorProvider;

  public MeshAIManager_Factory(Provider<RoutePredictionEngine> routePredictionEngineProvider,
      Provider<CongestionPredictor> congestionPredictorProvider,
      Provider<BatteryPredictor> batteryPredictorProvider,
      Provider<TransportPredictor> transportPredictorProvider,
      Provider<FailurePredictor> failurePredictorProvider,
      Provider<UserBehaviorEngine> userBehaviorEngineProvider,
      Provider<AnomalyDetector> anomalyDetectorProvider,
      Provider<RecommendationEngine> recommendationEngineProvider,
      Provider<CongestionMonitor> congestionMonitorProvider) {
    this.routePredictionEngineProvider = routePredictionEngineProvider;
    this.congestionPredictorProvider = congestionPredictorProvider;
    this.batteryPredictorProvider = batteryPredictorProvider;
    this.transportPredictorProvider = transportPredictorProvider;
    this.failurePredictorProvider = failurePredictorProvider;
    this.userBehaviorEngineProvider = userBehaviorEngineProvider;
    this.anomalyDetectorProvider = anomalyDetectorProvider;
    this.recommendationEngineProvider = recommendationEngineProvider;
    this.congestionMonitorProvider = congestionMonitorProvider;
  }

  @Override
  public MeshAIManager get() {
    return newInstance(routePredictionEngineProvider.get(), congestionPredictorProvider.get(), batteryPredictorProvider.get(), transportPredictorProvider.get(), failurePredictorProvider.get(), userBehaviorEngineProvider.get(), anomalyDetectorProvider.get(), recommendationEngineProvider.get(), congestionMonitorProvider.get());
  }

  public static MeshAIManager_Factory create(
      Provider<RoutePredictionEngine> routePredictionEngineProvider,
      Provider<CongestionPredictor> congestionPredictorProvider,
      Provider<BatteryPredictor> batteryPredictorProvider,
      Provider<TransportPredictor> transportPredictorProvider,
      Provider<FailurePredictor> failurePredictorProvider,
      Provider<UserBehaviorEngine> userBehaviorEngineProvider,
      Provider<AnomalyDetector> anomalyDetectorProvider,
      Provider<RecommendationEngine> recommendationEngineProvider,
      Provider<CongestionMonitor> congestionMonitorProvider) {
    return new MeshAIManager_Factory(routePredictionEngineProvider, congestionPredictorProvider, batteryPredictorProvider, transportPredictorProvider, failurePredictorProvider, userBehaviorEngineProvider, anomalyDetectorProvider, recommendationEngineProvider, congestionMonitorProvider);
  }

  public static MeshAIManager newInstance(RoutePredictionEngine routePredictionEngine,
      CongestionPredictor congestionPredictor, BatteryPredictor batteryPredictor,
      TransportPredictor transportPredictor, FailurePredictor failurePredictor,
      UserBehaviorEngine userBehaviorEngine, AnomalyDetector anomalyDetector,
      RecommendationEngine recommendationEngine, CongestionMonitor congestionMonitor) {
    return new MeshAIManager(routePredictionEngine, congestionPredictor, batteryPredictor, transportPredictor, failurePredictor, userBehaviorEngine, anomalyDetector, recommendationEngine, congestionMonitor);
  }
}
