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
public final class FleetManagementManager_Factory implements Factory<FleetManagementManager> {
  @Override
  public FleetManagementManager get() {
    return newInstance();
  }

  public static FleetManagementManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FleetManagementManager newInstance() {
    return new FleetManagementManager();
  }

  private static final class InstanceHolder {
    private static final FleetManagementManager_Factory INSTANCE = new FleetManagementManager_Factory();
  }
}
