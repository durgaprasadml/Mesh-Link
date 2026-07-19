package com.meshlink.config;

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
public final class RuntimeConfigManager_Factory implements Factory<RuntimeConfigManager> {
  @Override
  public RuntimeConfigManager get() {
    return newInstance();
  }

  public static RuntimeConfigManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static RuntimeConfigManager newInstance() {
    return new RuntimeConfigManager();
  }

  private static final class InstanceHolder {
    private static final RuntimeConfigManager_Factory INSTANCE = new RuntimeConfigManager_Factory();
  }
}
