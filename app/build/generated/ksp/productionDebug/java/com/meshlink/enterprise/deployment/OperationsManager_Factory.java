package com.meshlink.enterprise.deployment;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class OperationsManager_Factory implements Factory<OperationsManager> {
  private final Provider<FleetManagementManager> fleetManagerProvider;

  public OperationsManager_Factory(Provider<FleetManagementManager> fleetManagerProvider) {
    this.fleetManagerProvider = fleetManagerProvider;
  }

  @Override
  public OperationsManager get() {
    return newInstance(fleetManagerProvider.get());
  }

  public static OperationsManager_Factory create(
      Provider<FleetManagementManager> fleetManagerProvider) {
    return new OperationsManager_Factory(fleetManagerProvider);
  }

  public static OperationsManager newInstance(FleetManagementManager fleetManager) {
    return new OperationsManager(fleetManager);
  }
}
