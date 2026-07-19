package com.meshlink.video.codec;

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
public final class VideoCodecManager_Factory implements Factory<VideoCodecManager> {
  @Override
  public VideoCodecManager get() {
    return newInstance();
  }

  public static VideoCodecManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static VideoCodecManager newInstance() {
    return new VideoCodecManager();
  }

  private static final class InstanceHolder {
    private static final VideoCodecManager_Factory INSTANCE = new VideoCodecManager_Factory();
  }
}
