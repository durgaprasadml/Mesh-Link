package com.meshlink.data.media;

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
public final class VoicePlayer_Factory implements Factory<VoicePlayer> {
  @Override
  public VoicePlayer get() {
    return newInstance();
  }

  public static VoicePlayer_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static VoicePlayer newInstance() {
    return new VoicePlayer();
  }

  private static final class InstanceHolder {
    private static final VoicePlayer_Factory INSTANCE = new VoicePlayer_Factory();
  }
}
