package com.meshlink.service.work;

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
public final class BackgroundTaskScheduler_Factory implements Factory<BackgroundTaskScheduler> {
  private final Provider<Context> contextProvider;

  public BackgroundTaskScheduler_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BackgroundTaskScheduler get() {
    return newInstance(contextProvider.get());
  }

  public static BackgroundTaskScheduler_Factory create(Provider<Context> contextProvider) {
    return new BackgroundTaskScheduler_Factory(contextProvider);
  }

  public static BackgroundTaskScheduler newInstance(Context context) {
    return new BackgroundTaskScheduler(context);
  }
}
