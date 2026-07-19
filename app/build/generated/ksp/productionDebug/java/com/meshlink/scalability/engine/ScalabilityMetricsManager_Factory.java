package com.meshlink.scalability.engine;

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
public final class ScalabilityMetricsManager_Factory implements Factory<ScalabilityMetricsManager> {
  @Override
  public ScalabilityMetricsManager get() {
    return newInstance();
  }

  public static ScalabilityMetricsManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ScalabilityMetricsManager newInstance() {
    return new ScalabilityMetricsManager();
  }

  private static final class InstanceHolder {
    private static final ScalabilityMetricsManager_Factory INSTANCE = new ScalabilityMetricsManager_Factory();
  }
}
