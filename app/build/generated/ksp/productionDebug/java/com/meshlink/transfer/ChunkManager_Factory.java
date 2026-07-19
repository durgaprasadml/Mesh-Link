package com.meshlink.transfer;

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
public final class ChunkManager_Factory implements Factory<ChunkManager> {
  @Override
  public ChunkManager get() {
    return newInstance();
  }

  public static ChunkManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ChunkManager newInstance() {
    return new ChunkManager();
  }

  private static final class InstanceHolder {
    private static final ChunkManager_Factory INSTANCE = new ChunkManager_Factory();
  }
}
