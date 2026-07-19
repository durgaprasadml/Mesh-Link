package com.meshlink.routing.engine;

import android.content.Context;
import com.meshlink.ai.engine.BatteryPredictor;
import com.meshlink.emergency.EmergencyManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class BatteryAwareNetworking_Factory implements Factory<BatteryAwareNetworking> {
  private final Provider<Context> contextProvider;

  private final Provider<BatteryPredictor> batteryPredictorProvider;

  private final Provider<EmergencyManager> emergencyManagerProvider;

  public BatteryAwareNetworking_Factory(Provider<Context> contextProvider,
      Provider<BatteryPredictor> batteryPredictorProvider,
      Provider<EmergencyManager> emergencyManagerProvider) {
    this.contextProvider = contextProvider;
    this.batteryPredictorProvider = batteryPredictorProvider;
    this.emergencyManagerProvider = emergencyManagerProvider;
  }

  @Override
  public BatteryAwareNetworking get() {
    return newInstance(contextProvider.get(), batteryPredictorProvider.get(), emergencyManagerProvider.get());
  }

  public static BatteryAwareNetworking_Factory create(Provider<Context> contextProvider,
      Provider<BatteryPredictor> batteryPredictorProvider,
      Provider<EmergencyManager> emergencyManagerProvider) {
    return new BatteryAwareNetworking_Factory(contextProvider, batteryPredictorProvider, emergencyManagerProvider);
  }

  public static BatteryAwareNetworking newInstance(Context context,
      BatteryPredictor batteryPredictor, EmergencyManager emergencyManager) {
    return new BatteryAwareNetworking(context, batteryPredictor, emergencyManager);
  }
}
