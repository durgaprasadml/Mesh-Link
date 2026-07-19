package com.meshlink.voice.audio;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AudioFocusManager_Factory implements Factory<AudioFocusManager> {
  private final Provider<Context> contextProvider;

  public AudioFocusManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AudioFocusManager get() {
    return newInstance(contextProvider.get());
  }

  public static AudioFocusManager_Factory create(Provider<Context> contextProvider) {
    return new AudioFocusManager_Factory(contextProvider);
  }

  public static AudioFocusManager newInstance(Context context) {
    return new AudioFocusManager(context);
  }
}
