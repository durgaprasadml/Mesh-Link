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
public final class IntegrityManager_Factory implements Factory<IntegrityManager> {
  @Override
  public IntegrityManager get() {
    return newInstance();
  }

  public static IntegrityManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static IntegrityManager newInstance() {
    return new IntegrityManager();
  }

  private static final class InstanceHolder {
    private static final IntegrityManager_Factory INSTANCE = new IntegrityManager_Factory();
  }
}
