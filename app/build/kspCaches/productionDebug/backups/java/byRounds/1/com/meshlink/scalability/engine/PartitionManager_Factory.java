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
public final class PartitionManager_Factory implements Factory<PartitionManager> {
  @Override
  public PartitionManager get() {
    return newInstance();
  }

  public static PartitionManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PartitionManager newInstance() {
    return new PartitionManager();
  }

  private static final class InstanceHolder {
    private static final PartitionManager_Factory INSTANCE = new PartitionManager_Factory();
  }
}
