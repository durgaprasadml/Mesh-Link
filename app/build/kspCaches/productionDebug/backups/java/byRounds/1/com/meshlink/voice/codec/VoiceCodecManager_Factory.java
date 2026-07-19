package com.meshlink.voice.codec;

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
public final class VoiceCodecManager_Factory implements Factory<VoiceCodecManager> {
  @Override
  public VoiceCodecManager get() {
    return newInstance();
  }

  public static VoiceCodecManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static VoiceCodecManager newInstance() {
    return new VoiceCodecManager();
  }

  private static final class InstanceHolder {
    private static final VoiceCodecManager_Factory INSTANCE = new VoiceCodecManager_Factory();
  }
}
