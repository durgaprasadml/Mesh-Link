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
public final class BroadcastOptimizationManager_Factory implements Factory<BroadcastOptimizationManager> {
  @Override
  public BroadcastOptimizationManager get() {
    return newInstance();
  }

  public static BroadcastOptimizationManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BroadcastOptimizationManager newInstance() {
    return new BroadcastOptimizationManager();
  }

  private static final class InstanceHolder {
    private static final BroadcastOptimizationManager_Factory INSTANCE = new BroadcastOptimizationManager_Factory();
  }
}
