package com.meshlink.config;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class DeveloperSettingsManager_Factory implements Factory<DeveloperSettingsManager> {
  private final Provider<Context> contextProvider;

  public DeveloperSettingsManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DeveloperSettingsManager get() {
    return newInstance(contextProvider.get());
  }

  public static DeveloperSettingsManager_Factory create(Provider<Context> contextProvider) {
    return new DeveloperSettingsManager_Factory(contextProvider);
  }

  public static DeveloperSettingsManager newInstance(Context context) {
    return new DeveloperSettingsManager(context);
  }
}
