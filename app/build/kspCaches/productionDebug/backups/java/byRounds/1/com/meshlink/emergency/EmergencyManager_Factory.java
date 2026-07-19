package com.meshlink.emergency;

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
public final class EmergencyManager_Factory implements Factory<EmergencyManager> {
  @Override
  public EmergencyManager get() {
    return newInstance();
  }

  public static EmergencyManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static EmergencyManager newInstance() {
    return new EmergencyManager();
  }

  private static final class InstanceHolder {
    private static final EmergencyManager_Factory INSTANCE = new EmergencyManager_Factory();
  }
}
