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
public final class FeatureFlagManager_Factory implements Factory<FeatureFlagManager> {
  private final Provider<Context> contextProvider;

  public FeatureFlagManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FeatureFlagManager get() {
    return newInstance(contextProvider.get());
  }

  public static FeatureFlagManager_Factory create(Provider<Context> contextProvider) {
    return new FeatureFlagManager_Factory(contextProvider);
  }

  public static FeatureFlagManager newInstance(Context context) {
    return new FeatureFlagManager(context);
  }
}
