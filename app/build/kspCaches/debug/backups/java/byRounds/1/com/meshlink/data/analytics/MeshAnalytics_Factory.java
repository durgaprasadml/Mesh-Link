package com.meshlink.data.analytics;

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
public final class MeshAnalytics_Factory implements Factory<MeshAnalytics> {
  @Override
  public MeshAnalytics get() {
    return newInstance();
  }

  public static MeshAnalytics_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MeshAnalytics newInstance() {
    return new MeshAnalytics();
  }

  private static final class InstanceHolder {
    private static final MeshAnalytics_Factory INSTANCE = new MeshAnalytics_Factory();
  }
}
