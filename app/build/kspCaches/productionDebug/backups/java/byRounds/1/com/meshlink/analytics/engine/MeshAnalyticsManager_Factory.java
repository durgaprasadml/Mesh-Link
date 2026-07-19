package com.meshlink.analytics.engine;

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
public final class MeshAnalyticsManager_Factory implements Factory<MeshAnalyticsManager> {
  @Override
  public MeshAnalyticsManager get() {
    return newInstance();
  }

  public static MeshAnalyticsManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MeshAnalyticsManager newInstance() {
    return new MeshAnalyticsManager();
  }

  private static final class InstanceHolder {
    private static final MeshAnalyticsManager_Factory INSTANCE = new MeshAnalyticsManager_Factory();
  }
}
