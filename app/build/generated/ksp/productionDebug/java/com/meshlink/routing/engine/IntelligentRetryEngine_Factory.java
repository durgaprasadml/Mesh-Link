package com.meshlink.routing.engine;

import com.meshlink.ai.data.LearningRepository;
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
public final class IntelligentRetryEngine_Factory implements Factory<IntelligentRetryEngine> {
  private final Provider<CongestionMonitor> congestionMonitorProvider;

  private final Provider<BatteryAwareNetworking> batteryAwareNetworkingProvider;

  private final Provider<LearningRepository> learningRepositoryProvider;

  public IntelligentRetryEngine_Factory(Provider<CongestionMonitor> congestionMonitorProvider,
      Provider<BatteryAwareNetworking> batteryAwareNetworkingProvider,
      Provider<LearningRepository> learningRepositoryProvider) {
    this.congestionMonitorProvider = congestionMonitorProvider;
    this.batteryAwareNetworkingProvider = batteryAwareNetworkingProvider;
    this.learningRepositoryProvider = learningRepositoryProvider;
  }

  @Override
  public IntelligentRetryEngine get() {
    return newInstance(congestionMonitorProvider.get(), batteryAwareNetworkingProvider.get(), learningRepositoryProvider.get());
  }

  public static IntelligentRetryEngine_Factory create(
      Provider<CongestionMonitor> congestionMonitorProvider,
      Provider<BatteryAwareNetworking> batteryAwareNetworkingProvider,
      Provider<LearningRepository> learningRepositoryProvider) {
    return new IntelligentRetryEngine_Factory(congestionMonitorProvider, batteryAwareNetworkingProvider, learningRepositoryProvider);
  }

  public static IntelligentRetryEngine newInstance(CongestionMonitor congestionMonitor,
      BatteryAwareNetworking batteryAwareNetworking, LearningRepository learningRepository) {
    return new IntelligentRetryEngine(congestionMonitor, batteryAwareNetworking, learningRepository);
  }
}
