package com.meshlink.emergency.logistics;

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
public final class ResourceManager_Factory implements Factory<ResourceManager> {
  @Override
  public ResourceManager get() {
    return newInstance();
  }

  public static ResourceManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ResourceManager newInstance() {
    return new ResourceManager();
  }

  private static final class InstanceHolder {
    private static final ResourceManager_Factory INSTANCE = new ResourceManager_Factory();
  }
}
