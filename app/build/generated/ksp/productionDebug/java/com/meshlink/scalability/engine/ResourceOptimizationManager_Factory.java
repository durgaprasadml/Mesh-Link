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
public final class ResourceOptimizationManager_Factory implements Factory<ResourceOptimizationManager> {
  @Override
  public ResourceOptimizationManager get() {
    return newInstance();
  }

  public static ResourceOptimizationManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ResourceOptimizationManager newInstance() {
    return new ResourceOptimizationManager();
  }

  private static final class InstanceHolder {
    private static final ResourceOptimizationManager_Factory INSTANCE = new ResourceOptimizationManager_Factory();
  }
}
