package com.meshlink.common.power;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class PowerMetricsManager_Factory implements Factory<PowerMetricsManager> {
  @Override
  public PowerMetricsManager get() {
    return newInstance();
  }

  public static PowerMetricsManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PowerMetricsManager newInstance() {
    return new PowerMetricsManager();
  }

  private static final class InstanceHolder {
    private static final PowerMetricsManager_Factory INSTANCE = new PowerMetricsManager_Factory();
  }
}
