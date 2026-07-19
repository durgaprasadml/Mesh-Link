package com.meshlink.media.data;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "dagger.hilt.android.qualifiers.ApplicationContext",
    "com.meshlink.di.DefaultDispatcher",
    "com.meshlink.di.MainDispatcher"
})
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
public final class VoiceRecorder_Factory implements Factory<VoiceRecorder> {
  private final Provider<Context> contextProvider;

  private final Provider<CoroutineDispatcher> defaultDispatcherProvider;

  private final Provider<CoroutineDispatcher> mainDispatcherProvider;

  public VoiceRecorder_Factory(Provider<Context> contextProvider,
      Provider<CoroutineDispatcher> defaultDispatcherProvider,
      Provider<CoroutineDispatcher> mainDispatcherProvider) {
    this.contextProvider = contextProvider;
    this.defaultDispatcherProvider = defaultDispatcherProvider;
    this.mainDispatcherProvider = mainDispatcherProvider;
  }

  @Override
  public VoiceRecorder get() {
    return newInstance(contextProvider.get(), defaultDispatcherProvider.get(), mainDispatcherProvider.get());
  }

  public static VoiceRecorder_Factory create(Provider<Context> contextProvider,
      Provider<CoroutineDispatcher> defaultDispatcherProvider,
      Provider<CoroutineDispatcher> mainDispatcherProvider) {
    return new VoiceRecorder_Factory(contextProvider, defaultDispatcherProvider, mainDispatcherProvider);
  }

  public static VoiceRecorder newInstance(Context context, CoroutineDispatcher defaultDispatcher,
      CoroutineDispatcher mainDispatcher) {
    return new VoiceRecorder(context, defaultDispatcher, mainDispatcher);
  }
}
