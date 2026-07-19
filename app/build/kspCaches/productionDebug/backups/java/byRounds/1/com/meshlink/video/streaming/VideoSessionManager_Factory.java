package com.meshlink.video.streaming;

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
public final class VideoSessionManager_Factory implements Factory<VideoSessionManager> {
  @Override
  public VideoSessionManager get() {
    return newInstance();
  }

  public static VideoSessionManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static VideoSessionManager newInstance() {
    return new VideoSessionManager();
  }

  private static final class InstanceHolder {
    private static final VideoSessionManager_Factory INSTANCE = new VideoSessionManager_Factory();
  }
}
