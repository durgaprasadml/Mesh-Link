package com.meshlink.di;

import com.meshlink.data.media.VoicePlayer;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideVoicePlayerFactory implements Factory<VoicePlayer> {
  @Override
  public VoicePlayer get() {
    return provideVoicePlayer();
  }

  public static AppModule_ProvideVoicePlayerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static VoicePlayer provideVoicePlayer() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideVoicePlayer());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideVoicePlayerFactory INSTANCE = new AppModule_ProvideVoicePlayerFactory();
  }
}
