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
public final class MeshScalabilityManager_Factory implements Factory<MeshScalabilityManager> {
  @Override
  public MeshScalabilityManager get() {
    return newInstance();
  }

  public static MeshScalabilityManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MeshScalabilityManager newInstance() {
    return new MeshScalabilityManager();
  }

  private static final class InstanceHolder {
    private static final MeshScalabilityManager_Factory INSTANCE = new MeshScalabilityManager_Factory();
  }
}
