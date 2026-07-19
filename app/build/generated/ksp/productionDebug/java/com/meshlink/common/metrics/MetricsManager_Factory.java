package com.meshlink.common.metrics;

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
public final class MetricsManager_Factory implements Factory<MetricsManager> {
  @Override
  public MetricsManager get() {
    return newInstance();
  }

  public static MetricsManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MetricsManager newInstance() {
    return new MetricsManager();
  }

  private static final class InstanceHolder {
    private static final MetricsManager_Factory INSTANCE = new MetricsManager_Factory();
  }
}
