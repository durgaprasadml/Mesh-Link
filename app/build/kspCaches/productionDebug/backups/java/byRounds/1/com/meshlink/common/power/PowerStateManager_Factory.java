package com.meshlink.common.power;

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
public final class PowerStateManager_Factory implements Factory<PowerStateManager> {
  private final Provider<Context> contextProvider;

  public PowerStateManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PowerStateManager get() {
    return newInstance(contextProvider.get());
  }

  public static PowerStateManager_Factory create(Provider<Context> contextProvider) {
    return new PowerStateManager_Factory(contextProvider);
  }

  public static PowerStateManager newInstance(Context context) {
    return new PowerStateManager(context);
  }
}
