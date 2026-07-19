package com.meshlink.emergency.rescue;

import com.meshlink.routing.data.MeshRouter;
import com.meshlink.routing.engine.BatteryAwareNetworking;
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
public final class EmergencyBeacon_Factory implements Factory<EmergencyBeacon> {
  private final Provider<MeshRouter> meshRouterProvider;

  private final Provider<BatteryAwareNetworking> batteryAwareNetworkingProvider;

  public EmergencyBeacon_Factory(Provider<MeshRouter> meshRouterProvider,
      Provider<BatteryAwareNetworking> batteryAwareNetworkingProvider) {
    this.meshRouterProvider = meshRouterProvider;
    this.batteryAwareNetworkingProvider = batteryAwareNetworkingProvider;
  }

  @Override
  public EmergencyBeacon get() {
    return newInstance(meshRouterProvider.get(), batteryAwareNetworkingProvider.get());
  }

  public static EmergencyBeacon_Factory create(Provider<MeshRouter> meshRouterProvider,
      Provider<BatteryAwareNetworking> batteryAwareNetworkingProvider) {
    return new EmergencyBeacon_Factory(meshRouterProvider, batteryAwareNetworkingProvider);
  }

  public static EmergencyBeacon newInstance(MeshRouter meshRouter,
      BatteryAwareNetworking batteryAwareNetworking) {
    return new EmergencyBeacon(meshRouter, batteryAwareNetworking);
  }
}
