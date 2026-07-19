package com.meshlink.media.data;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.meshlink.di.DefaultDispatcher")
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
  private final Provider<CoroutineDispatcher> defaultDispatcherProvider;

  public VoicePlayer_Factory(Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    this.defaultDispatcherProvider = defaultDispatcherProvider;
  }

  @Override
  public VoicePlayer get() {
    return newInstance(defaultDispatcherProvider.get());
  }

  public static VoicePlayer_Factory create(
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    return new VoicePlayer_Factory(defaultDispatcherProvider);
  }

  public static VoicePlayer newInstance(CoroutineDispatcher defaultDispatcher) {
    return new VoicePlayer(defaultDispatcher);
  }
}
