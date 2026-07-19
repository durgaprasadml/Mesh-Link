package com.meshlink.enterprise.deployment;

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
public final class DeploymentLifecycleManager_Factory implements Factory<DeploymentLifecycleManager> {
  @Override
  public DeploymentLifecycleManager get() {
    return newInstance();
  }

  public static DeploymentLifecycleManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DeploymentLifecycleManager newInstance() {
    return new DeploymentLifecycleManager();
  }

  private static final class InstanceHolder {
    private static final DeploymentLifecycleManager_Factory INSTANCE = new DeploymentLifecycleManager_Factory();
  }
}
