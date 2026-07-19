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
public final class RoutingOptimizationManager_Factory implements Factory<RoutingOptimizationManager> {
  @Override
  public RoutingOptimizationManager get() {
    return newInstance();
  }

  public static RoutingOptimizationManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RoutingOptimizationManager newInstance() {
    return new RoutingOptimizationManager();
  }

  private static final class InstanceHolder {
    private static final RoutingOptimizationManager_Factory INSTANCE = new RoutingOptimizationManager_Factory();
  }
}
