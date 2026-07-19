package com.meshlink.emergency;

import com.meshlink.routing.engine.RoutingEngine;
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
public final class DisasterRecoveryEngine_Factory implements Factory<DisasterRecoveryEngine> {
  private final Provider<EmergencyManager> emergencyManagerProvider;

  private final Provider<RoutingEngine> routingEngineProvider;

  public DisasterRecoveryEngine_Factory(Provider<EmergencyManager> emergencyManagerProvider,
      Provider<RoutingEngine> routingEngineProvider) {
    this.emergencyManagerProvider = emergencyManagerProvider;
    this.routingEngineProvider = routingEngineProvider;
  }

  @Override
  public DisasterRecoveryEngine get() {
    return newInstance(emergencyManagerProvider.get(), routingEngineProvider.get());
  }

  public static DisasterRecoveryEngine_Factory create(
      Provider<EmergencyManager> emergencyManagerProvider,
      Provider<RoutingEngine> routingEngineProvider) {
    return new DisasterRecoveryEngine_Factory(emergencyManagerProvider, routingEngineProvider);
  }

  public static DisasterRecoveryEngine newInstance(EmergencyManager emergencyManager,
      RoutingEngine routingEngine) {
    return new DisasterRecoveryEngine(emergencyManager, routingEngine);
  }
}
