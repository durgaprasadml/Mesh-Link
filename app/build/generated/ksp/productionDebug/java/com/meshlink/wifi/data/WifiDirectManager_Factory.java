package com.meshlink.wifi.data;

import android.content.Context;
import com.google.firebase.analytics.FirebaseAnalytics;
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

  private final Provider<FirebaseAnalytics> analyticsProvider;

  public WifiDirectManager_Factory(Provider<Context> contextProvider,
      Provider<FirebaseAnalytics> analyticsProvider) {
    this.contextProvider = contextProvider;
    this.analyticsProvider = analyticsProvider;
  }

  @Override
  public WifiDirectManager get() {
    return newInstance(contextProvider.get(), analyticsProvider.get());
  }

  public static WifiDirectManager_Factory create(Provider<Context> contextProvider,
      Provider<FirebaseAnalytics> analyticsProvider) {
    return new WifiDirectManager_Factory(contextProvider, analyticsProvider);
  }

  public static WifiDirectManager newInstance(Context context, FirebaseAnalytics analytics) {
    return new WifiDirectManager(context, analytics);
  }
}
