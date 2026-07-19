package com.meshlink.voice.streaming;

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
public final class VoiceSessionManager_Factory implements Factory<VoiceSessionManager> {
  @Override
  public VoiceSessionManager get() {
    return newInstance();
  }

  public static VoiceSessionManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static VoiceSessionManager newInstance() {
    return new VoiceSessionManager();
  }

  private static final class InstanceHolder {
    private static final VoiceSessionManager_Factory INSTANCE = new VoiceSessionManager_Factory();
  }
}
