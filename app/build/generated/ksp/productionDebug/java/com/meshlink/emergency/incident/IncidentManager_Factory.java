package com.meshlink.emergency.incident;

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
public final class IncidentManager_Factory implements Factory<IncidentManager> {
  @Override
  public IncidentManager get() {
    return newInstance();
  }

  public static IncidentManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static IncidentManager newInstance() {
    return new IncidentManager();
  }

  private static final class InstanceHolder {
    private static final IncidentManager_Factory INSTANCE = new IncidentManager_Factory();
  }
}
