package com.meshlink.voice.codec;

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
public final class JitterBuffer_Factory implements Factory<JitterBuffer> {
  private final Provider<CoroutineDispatcher> defaultDispatcherProvider;

  public JitterBuffer_Factory(Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    this.defaultDispatcherProvider = defaultDispatcherProvider;
  }

  @Override
  public JitterBuffer get() {
    return newInstance(defaultDispatcherProvider.get());
  }

  public static JitterBuffer_Factory create(
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    return new JitterBuffer_Factory(defaultDispatcherProvider);
  }

  public static JitterBuffer newInstance(CoroutineDispatcher defaultDispatcher) {
    return new JitterBuffer(defaultDispatcher);
  }
}
