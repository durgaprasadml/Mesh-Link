package com.meshlink.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "com.meshlink.di.ApplicationScope",
    "com.meshlink.di.DefaultDispatcher"
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
public final class CoroutineModule_ProvideApplicationScopeFactory implements Factory<CoroutineScope> {
  private final Provider<CoroutineDispatcher> defaultDispatcherProvider;

  public CoroutineModule_ProvideApplicationScopeFactory(
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    this.defaultDispatcherProvider = defaultDispatcherProvider;
  }

  @Override
  public CoroutineScope get() {
    return provideApplicationScope(defaultDispatcherProvider.get());
  }

  public static CoroutineModule_ProvideApplicationScopeFactory create(
      Provider<CoroutineDispatcher> defaultDispatcherProvider) {
    return new CoroutineModule_ProvideApplicationScopeFactory(defaultDispatcherProvider);
  }

  public static CoroutineScope provideApplicationScope(CoroutineDispatcher defaultDispatcher) {
    return Preconditions.checkNotNullFromProvides(CoroutineModule.INSTANCE.provideApplicationScope(defaultDispatcher));
  }
}
