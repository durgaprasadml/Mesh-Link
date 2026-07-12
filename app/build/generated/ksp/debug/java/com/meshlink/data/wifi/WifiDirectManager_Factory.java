package com.meshlink.data.wifi;

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
public final class WifiDirectManager_Factory implements Factory<WifiDirectManager> {
  private final Provider<Context> contextProvider;

  public WifiDirectManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WifiDirectManager get() {
    return newInstance(contextProvider.get());
  }

  public static WifiDirectManager_Factory create(Provider<Context> contextProvider) {
    return new WifiDirectManager_Factory(contextProvider);
  }

  public static WifiDirectManager newInstance(Context context) {
    return new WifiDirectManager(context);
  }
}
