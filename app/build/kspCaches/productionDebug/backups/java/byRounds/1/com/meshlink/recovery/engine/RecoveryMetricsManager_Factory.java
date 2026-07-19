package com.meshlink.recovery.engine;

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
public final class RecoveryMetricsManager_Factory implements Factory<RecoveryMetricsManager> {
  @Override
  public RecoveryMetricsManager get() {
    return newInstance();
  }

  public static RecoveryMetricsManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RecoveryMetricsManager newInstance() {
    return new RecoveryMetricsManager();
  }

  private static final class InstanceHolder {
    private static final RecoveryMetricsManager_Factory INSTANCE = new RecoveryMetricsManager_Factory();
  }
}
