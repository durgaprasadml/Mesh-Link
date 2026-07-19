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
public final class TopologyManager_Factory implements Factory<TopologyManager> {
  @Override
  public TopologyManager get() {
    return newInstance();
  }

  public static TopologyManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TopologyManager newInstance() {
    return new TopologyManager();
  }

  private static final class InstanceHolder {
    private static final TopologyManager_Factory INSTANCE = new TopologyManager_Factory();
  }
}
